-- =============================================================================
-- V9__Restauration_Audit_Historique_Complet.sql
-- La base de production a fini avec une version de fn_audit_historique
-- differente de V4/V7/V8 (probablement une correction manuelle appliquee
-- en debug direct), qui omettait nom_table et id_enregistrement (colonnes
-- NOT NULL de historique) -> erreurs lors de la validation d'une facture.
--
-- Un correctif manuel ulterieur a resolu l'erreur immediate mais en
-- restreignant la fonction au seul cas facturation BROUILLON->VALIDEE,
-- SANS verifier TG_OP avant de lire OLD/NEW : plante sur tout INSERT
-- (ex. "Ouvrir la periode") ou DELETE sur facturation. Il a aussi perdu
-- la journalisation de toutes les autres tables et actions (creation,
-- modification, suppression, reouverture de mois...).
--
-- Cette migration republie la version complete et sure (celle de V8),
-- qui gere explicitement TG_OP, couvre les 7 tables auditees, et
-- inclut deja nom_table / id_enregistrement dans l'INSERT.
-- =============================================================================

SET search_path TO app_facturation, public;

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

        IF TG_TABLE_NAME = 'facturation'
           AND OLD.statut::text = 'BROUILLON'
           AND NEW.statut::text = 'VALIDEE' THEN
            v_action := 'VALIDATION_MOIS';
        ELSIF TG_TABLE_NAME = 'facturation'
           AND OLD.statut::text = 'VALIDEE'
           AND NEW.statut::text = 'BROUILLON' THEN
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