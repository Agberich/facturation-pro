-- =============================================================================
-- V14__Correction_Audit_Champ_Statut_Polymorphe.sql
-- fn_audit_historique() est partagee par 7 triggers (facturation, client,
-- utilisateur, entreprise, parametre, ligne_facturation, facture_pdf).
-- Le bloc :
--   IF TG_TABLE_NAME = 'facturation' AND OLD.statut::text = 'BROUILLON' ...
-- combine TG_TABLE_NAME et OLD.statut dans UNE SEULE expression booleenne.
-- OLD/NEW sont de type RECORD generique dans une fonction polymorphe :
-- PostgreSQL doit resoudre le type de TOUTE l'expression combinee avant
-- de l'evaluer, et ca echoue des qu'une table sans colonne "statut"
-- (client, utilisateur, parametre, entreprise...) declenche le trigger,
-- meme si le AND aurait du s'arreter avant d'y arriver :
--   ERROR: record "old" has no field "statut"
--
-- Fix : separer en deux IF imbriques. Le IF interieur (qui reference
-- OLD.statut/NEW.statut) n'est alors ni prepare ni evalue tant qu'on
-- n'est pas deja confirme sur la table "facturation".
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

        -- IF imbrique : OLD.statut / NEW.statut ne sont references
        -- (et donc jamais resolus) que si on est deja sur "facturation".
        IF TG_TABLE_NAME = 'facturation' THEN
            IF OLD.statut::text = 'BROUILLON' AND NEW.statut::text = 'VALIDEE' THEN
                v_action := 'VALIDATION_MOIS';
            ELSIF OLD.statut::text = 'VALIDEE' AND NEW.statut::text = 'BROUILLON' THEN
                v_action := 'REOUVERTURE_MOIS';
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