-- =============================================================================
-- V16__Paiement_Tarif_Journalier_Verrouillage.sql
-- 1. facturation.date_paiement : date de reglement d'une facture.
-- 2. parametre.tarif_journalier : tarif unique par jour et par personne
--    accueillie (79,91 par defaut), modifiable dans Parametres > Facturation.
--    La devise par defaut des NOUVEAUX parametres devient EUR ; les parametres
--    existants ne sont PAS modifies (a changer depuis l'ecran Parametres).
-- 3. Verrouillage etendu aux factures PAYEE (facture + lignes).
--    Transitions autorisees sur une facture verrouillee, sans AUCUNE autre
--    modification :
--      VALIDEE -> BROUILLON  (reouverture)
--      VALIDEE -> PAYEE      (marquer comme payee)
--      PAYEE   -> VALIDEE    (annuler le paiement)
-- 4. Audit : PAIEMENT_FACTURE / ANNULATION_PAIEMENT (structure de V14 conservee).
-- =============================================================================

SET search_path TO app_facturation, public;

ALTER TABLE app_facturation.facturation
    ADD COLUMN IF NOT EXISTS date_paiement TIMESTAMPTZ;

ALTER TABLE app_facturation.parametre
    ADD COLUMN IF NOT EXISTS tarif_journalier NUMERIC(12,2) NOT NULL DEFAULT 79.91;

ALTER TABLE app_facturation.parametre
    DROP CONSTRAINT IF EXISTS chk_tarif_journalier;
ALTER TABLE app_facturation.parametre
    ADD CONSTRAINT chk_tarif_journalier CHECK (tarif_journalier >= 0);

ALTER TABLE app_facturation.parametre
    ALTER COLUMN devise SET DEFAULT 'EUR';

-- -----------------------------------------------------------------------------
-- Verrouillage de la facturation (VALIDEE et PAYEE)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_facturation.fn_verifier_immutabilite_facturation()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = app_facturation, public
AS $$
DECLARE
    v_autres_colonnes_identiques BOOLEAN;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'Opération interdite : la facturation % est validée ou payée et ne peut pas être supprimée.',
            OLD.id_facturation;
    END IF;

    -- Toutes les colonnes autres que statut / date_validation / valide_par /
    -- date_paiement / updated_at doivent rester strictement identiques.
    v_autres_colonnes_identiques :=
            NEW.id_entreprise IS NOT DISTINCT FROM OLD.id_entreprise
        AND NEW.numero_facture IS NOT DISTINCT FROM OLD.numero_facture
        AND NEW.annee IS NOT DISTINCT FROM OLD.annee
        AND NEW.mois IS NOT DISTINCT FROM OLD.mois
        AND NEW.date_debut_periode IS NOT DISTINCT FROM OLD.date_debut_periode
        AND NEW.date_fin_periode IS NOT DISTINCT FROM OLD.date_fin_periode
        AND NEW.cree_par IS NOT DISTINCT FROM OLD.cree_par
        AND NEW.commentaire IS NOT DISTINCT FROM OLD.commentaire
        AND NEW.created_at IS NOT DISTINCT FROM OLD.created_at
        AND NEW.deleted_at IS NOT DISTINCT FROM OLD.deleted_at;

    IF v_autres_colonnes_identiques THEN
        -- Reouverture : VALIDEE -> BROUILLON
        IF OLD.statut::text = 'VALIDEE' AND NEW.statut::text = 'BROUILLON' THEN
            RETURN NEW;
        END IF;

        -- Paiement : VALIDEE -> PAYEE (la validation reste inchangee)
        IF OLD.statut::text = 'VALIDEE' AND NEW.statut::text = 'PAYEE'
           AND NEW.date_validation IS NOT DISTINCT FROM OLD.date_validation
           AND NEW.valide_par IS NOT DISTINCT FROM OLD.valide_par THEN
            RETURN NEW;
        END IF;

        -- Annulation du paiement : PAYEE -> VALIDEE
        IF OLD.statut::text = 'PAYEE' AND NEW.statut::text = 'VALIDEE'
           AND NEW.date_validation IS NOT DISTINCT FROM OLD.date_validation
           AND NEW.valide_par IS NOT DISTINCT FROM OLD.valide_par THEN
            RETURN NEW;
        END IF;
    END IF;

    RAISE EXCEPTION
        'Opération interdite : la facturation % est validée ou payée. Seules la réouverture (Validée -> Brouillon), le paiement (Validée -> Payée) et l''annulation du paiement (Payée -> Validée) sont autorisés, sans autre modification.',
        OLD.id_facturation;
END;
$$;

DROP TRIGGER IF EXISTS trg_lock_facturation_validee ON app_facturation.facturation;
CREATE TRIGGER trg_lock_facturation_validee
BEFORE UPDATE OR DELETE ON app_facturation.facturation
FOR EACH ROW
WHEN (OLD.statut::text IN ('VALIDEE', 'PAYEE'))
EXECUTE FUNCTION app_facturation.fn_verifier_immutabilite_facturation();

-- -----------------------------------------------------------------------------
-- Verrouillage des lignes (facture parent VALIDEE ou PAYEE)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_facturation.fn_verifier_immutabilite_ligne()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = app_facturation, public
AS $$
DECLARE
    v_statut TEXT;
    v_id_facturation UUID;
BEGIN
    v_id_facturation := CASE WHEN TG_OP = 'INSERT' THEN NEW.id_facturation ELSE OLD.id_facturation END;

    SELECT statut::text INTO v_statut
    FROM app_facturation.facturation
    WHERE id_facturation = v_id_facturation;

    IF v_statut IN ('VALIDEE', 'PAYEE') THEN
        RAISE EXCEPTION
            'Opération interdite : la facturation parent % est validée ou payée, aucune ligne ne peut y être ajoutée, modifiée ou supprimée.',
            v_id_facturation;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

-- -----------------------------------------------------------------------------
-- Audit : on reprend EXACTEMENT la structure de V14 (IF imbriques) et on
-- ajoute les deux transitions de paiement.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_facturation.fn_audit_historique()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_avant JSONB;
    v_apres JSONB;
    v_id_entreprise UUID;
    v_action app_facturation.type_action_audit;
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

        -- IF imbrique : OLD.statut / NEW.statut ne sont references
        -- que si on est deja sur "facturation".
        IF TG_TABLE_NAME = 'facturation' THEN
            IF OLD.statut::text = 'BROUILLON' AND NEW.statut::text = 'VALIDEE' THEN
                v_action := 'VALIDATION_MOIS';
            ELSIF OLD.statut::text = 'VALIDEE' AND NEW.statut::text = 'BROUILLON' THEN
                v_action := 'REOUVERTURE_MOIS';
            ELSIF OLD.statut::text = 'VALIDEE' AND NEW.statut::text = 'PAYEE' THEN
                v_action := 'PAIEMENT_FACTURE';
            ELSIF OLD.statut::text = 'PAYEE' AND NEW.statut::text = 'VALIDEE' THEN
                v_action := 'ANNULATION_PAIEMENT';
            END IF;
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
        FROM app_facturation.facturation f
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

    INSERT INTO app_facturation.historique (
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
