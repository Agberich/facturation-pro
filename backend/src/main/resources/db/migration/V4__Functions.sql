-- =============================================================================
-- V4__Functions.sql
-- SPRINT 1 - Base de données
-- Fonctions métier, numérotation et audit
-- =============================================================================

SET search_path TO app_facturation, public;

CREATE OR REPLACE FUNCTION fn_creer_sequence_facture()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format(
        'CREATE SEQUENCE IF NOT EXISTS app_facturation.seq_facture_%s START WITH 1 INCREMENT BY 1',
        replace(NEW.id_entreprise::text, '-', '_')
    );
    RETURN NEW;
END;
$$;

-- p_annee doit être l'année de la période facturée (facturation.annee),
-- et non l'année d'exécution (CURRENT_DATE) : une facturation de mars 2026
-- validée en 2027, par exemple, doit conserver un numéro basé sur 2026.
CREATE OR REPLACE FUNCTION generer_numero_facture(p_id_entreprise UUID, p_annee INTEGER)
RETURNS VARCHAR(30)
LANGUAGE plpgsql
AS $$
DECLARE
    v_prefixe VARCHAR(20);
    v_sequence_name TEXT;
    v_prochain BIGINT;
BEGIN
    SELECT COALESCE(NULLIF(TRIM(prefixe_facture), ''), 'FAC')
    INTO v_prefixe
    FROM parametre
    WHERE id_entreprise = p_id_entreprise;

    IF v_prefixe IS NULL THEN
        v_prefixe := 'FAC';
    END IF;

    -- Séquence par entreprise ET par année : la numérotation redémarre
    -- proprement à 1 chaque nouvelle année, comme l'exigent la plupart
    -- des réglementations de facturation.
    v_sequence_name := 'app_facturation.seq_facture_' || replace(p_id_entreprise::text, '-', '_')
                        || '_' || p_annee::text;

    EXECUTE format(
        'CREATE SEQUENCE IF NOT EXISTS %s START WITH 1 INCREMENT BY 1',
        v_sequence_name
    );

    v_prochain := nextval(v_sequence_name::regclass);

    RETURN v_prefixe || '-' || p_annee::text || '-' || lpad(v_prochain::text, 5, '0');
END;
$$;

CREATE OR REPLACE FUNCTION calculer_nb_jours_factures(
    p_date_entree DATE,
    p_date_sortie DATE,
    p_debut_mois DATE,
    p_fin_mois DATE
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_debut DATE;
    v_fin DATE;
BEGIN
    v_debut := GREATEST(COALESCE(p_date_entree, p_debut_mois), p_debut_mois);
    v_fin := LEAST(COALESCE(p_date_sortie, p_fin_mois), p_fin_mois);

    IF v_fin < v_debut THEN
        RETURN 0;
    END IF;

    RETURN v_fin - v_debut + 1;
END;
$$;

CREATE OR REPLACE FUNCTION calculer_montant_ht(p_tarif NUMERIC, p_nb_jours INTEGER)
RETURNS NUMERIC(12,2)
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT ROUND(COALESCE(p_tarif, 0) * COALESCE(p_nb_jours, 0), 2);
$$;

CREATE OR REPLACE FUNCTION calculer_tva(p_montant_ht NUMERIC, p_taux_tva NUMERIC)
RETURNS NUMERIC(12,2)
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT ROUND(COALESCE(p_montant_ht, 0) * COALESCE(p_taux_tva, 0) / 100, 2);
$$;

CREATE OR REPLACE FUNCTION calculer_ttc(p_ht NUMERIC, p_tva NUMERIC)
RETURNS NUMERIC(12,2)
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT ROUND(COALESCE(p_ht, 0) + COALESCE(p_tva, 0), 2);
$$;

CREATE OR REPLACE FUNCTION recalculer_ligne_facturation(p_id_ligne UUID)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_tarif NUMERIC;
    v_taux_tva NUMERIC;
    v_entree DATE;
    v_sortie DATE;
    v_debut DATE;
    v_fin DATE;
    v_jours INTEGER;
    v_ht NUMERIC;
    v_tva NUMERIC;
    v_ttc NUMERIC;
BEGIN
    SELECT
        lf.tarif_applique,
        lf.date_entree_effective,
        lf.date_sortie_effective,
        f.date_debut_periode,
        f.date_fin_periode,
        COALESCE(p.taux_tva, 18.00)
    INTO v_tarif, v_entree, v_sortie, v_debut, v_fin, v_taux_tva
    FROM ligne_facturation lf
    JOIN facturation f ON f.id_facturation = lf.id_facturation
    LEFT JOIN parametre p ON p.id_entreprise = f.id_entreprise
    WHERE lf.id_ligne = p_id_ligne;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Ligne de facturation introuvable : %', p_id_ligne;
    END IF;

    v_jours := calculer_nb_jours_factures(v_entree, v_sortie, v_debut, v_fin);
    v_ht := calculer_montant_ht(v_tarif, v_jours);
    v_tva := calculer_tva(v_ht, v_taux_tva);
    v_ttc := calculer_ttc(v_ht, v_tva);

    UPDATE ligne_facturation
    SET nb_jours = v_jours,
        montant_ht = v_ht,
        montant_tva = v_tva,
        montant_ttc = v_ttc
    WHERE id_ligne = p_id_ligne;
END;
$$;

CREATE OR REPLACE FUNCTION fn_audit_historique()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_avant JSONB;
    v_apres JSONB;
    v_id_entreprise UUID;
    v_action type_action_audit;
    v_id_utilisateur UUID;
    v_id_enregistrement UUID;
    v_pk TEXT := TG_ARGV[0];
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_avant := to_jsonb(OLD);
        v_apres := NULL;
        v_action := 'SUPPRESSION';
    ELSIF TG_OP = 'UPDATE' THEN
        v_avant := to_jsonb(OLD);
        v_apres := to_jsonb(NEW);
        v_action := 'MODIFICATION';

        IF TG_TABLE_NAME = 'facturation'
           AND OLD.statut = 'BROUILLON'
           AND NEW.statut = 'VALIDEE' THEN
            v_action := 'VALIDATION_MOIS';
        ELSIF TG_TABLE_NAME = 'facturation'
           AND OLD.statut = 'VALIDEE'
           AND NEW.statut = 'BROUILLON' THEN
            v_action := 'REOUVERTURE_MOIS';
        END IF;
    ELSE
        v_avant := NULL;
        v_apres := to_jsonb(NEW);
        v_action := 'CREATION';
    END IF;

    BEGIN
        v_id_entreprise := COALESCE(
            (v_apres->>'id_entreprise')::UUID,
            (v_avant->>'id_entreprise')::UUID
        );
    EXCEPTION WHEN OTHERS THEN
        v_id_entreprise := NULL;
    END;

    IF TG_TABLE_NAME IN ('ligne_facturation', 'facture_pdf') AND v_id_entreprise IS NULL THEN
        SELECT f.id_entreprise INTO v_id_entreprise
        FROM facturation f
        WHERE f.id_facturation = COALESCE(
            (v_apres->>'id_facturation')::UUID,
            (v_avant->>'id_facturation')::UUID
        );
    END IF;

    BEGIN
        v_id_enregistrement := COALESCE(
            (v_apres->>v_pk)::UUID,
            (v_avant->>v_pk)::UUID
        );
    EXCEPTION WHEN OTHERS THEN
        v_id_enregistrement := NULL;
    END;

    BEGIN
        v_id_utilisateur := NULLIF(current_setting('app.current_user_id', true), '')::UUID;
    EXCEPTION WHEN OTHERS THEN
        v_id_utilisateur := NULL;
    END;

    INSERT INTO historique (
        id_utilisateur, id_entreprise, action, nom_table, id_enregistrement,
        ancienne_valeur, nouvelle_valeur, description, adresse_ip, user_agent
    ) VALUES (
        v_id_utilisateur,
        v_id_entreprise,
        v_action,
        TG_TABLE_NAME,
        v_id_enregistrement,
        v_avant,
        v_apres,
        format('%s sur %s', v_action, TG_TABLE_NAME),
        NULLIF(current_setting('app.current_ip', true), ''),
        NULLIF(current_setting('app.current_user_agent', true), '')
    );

    RETURN COALESCE(NEW, OLD);
END;
$$;
