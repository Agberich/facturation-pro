-- =============================================================================
-- V7__Correction_Audit.sql
-- Correction de la fonction d'audit générique
-- =============================================================================

SET search_path TO app_facturation, public;

CREATE OR REPLACE FUNCTION app_facturation.fn_audit_historique()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    V_avant JSONB;
    V_apres JSONB;
    V_id_entreprise UUID;
    V_action app_facturation.type_action_audit;
    V_id_utilisateur UUID;
    V_id_enregistrement UUID;
    V_colonne_pk TEXT := TG_ARGV[0];
BEGIN

    IF TG_OP = 'DELETE' THEN
        V_avant := to_jsonb(OLD);
        V_apres := NULL;
        V_action := 'SUPPRESSION';

    ELSIF TG_OP = 'UPDATE' THEN
        V_avant := to_jsonb(OLD);
        V_apres := to_jsonb(NEW);
        V_action := 'MODIFICATION';

    ELSE
        V_avant := NULL;
        V_apres := to_jsonb(NEW);
        V_action := 'CREATION';
    END IF;


    BEGIN
        V_id_entreprise :=
            COALESCE(
                (V_apres->>'id_entreprise')::UUID,
                (V_avant->>'id_entreprise')::UUID
            );
    EXCEPTION
        WHEN OTHERS THEN
            V_id_entreprise := NULL;
    END;


    V_id_enregistrement :=
        COALESCE(
            (V_apres->>V_colonne_pk)::UUID,
            (V_avant->>V_colonne_pk)::UUID
        );


    BEGIN
        V_id_utilisateur :=
            NULLIF(
                current_setting('app.current_user_id', true),
                ''
            )::UUID;
    EXCEPTION
        WHEN OTHERS THEN
            V_id_utilisateur := NULL;
    END;


    INSERT INTO app_facturation.historique
    (
        id_utilisateur,
        action,
        nom_table,
        id_enregistrement,
        ancienne_valeur,
        nouvelle_valeur,
        id_entreprise
    )
    VALUES
    (
        V_id_utilisateur,
        V_action,
        TG_TABLE_NAME,
        V_id_enregistrement,
        V_avant,
        V_apres,
        V_id_entreprise
    );


    RETURN COALESCE(NEW, OLD);

END;
$$;