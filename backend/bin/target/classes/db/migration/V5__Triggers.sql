-- =============================================================================
-- V5__Triggers.sql
-- SPRINT 1 - Base de données
-- Automatisations, audit et protection des facturations validées
-- =============================================================================

SET search_path TO app_facturation, public;

CREATE TRIGGER trg_updated_at_entreprise
BEFORE UPDATE ON entreprise
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_updated_at_parametre
BEFORE UPDATE ON parametre
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_updated_at_utilisateur
BEFORE UPDATE ON utilisateur
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_updated_at_client
BEFORE UPDATE ON client
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_updated_at_facturation
BEFORE UPDATE ON facturation
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_updated_at_ligne_facturation
BEFORE UPDATE ON ligne_facturation
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_creer_sequence_facture
AFTER INSERT ON entreprise
FOR EACH ROW EXECUTE FUNCTION fn_creer_sequence_facture();

CREATE TRIGGER trg_audit_entreprise
AFTER INSERT OR UPDATE OR DELETE ON entreprise
FOR EACH ROW EXECUTE FUNCTION fn_audit_historique('id_entreprise');

CREATE TRIGGER trg_audit_parametre
AFTER INSERT OR UPDATE OR DELETE ON parametre
FOR EACH ROW EXECUTE FUNCTION fn_audit_historique('id_parametre');

CREATE TRIGGER trg_audit_utilisateur
AFTER INSERT OR UPDATE OR DELETE ON utilisateur
FOR EACH ROW EXECUTE FUNCTION fn_audit_historique('id_utilisateur');

CREATE TRIGGER trg_audit_client
AFTER INSERT OR UPDATE OR DELETE ON client
FOR EACH ROW EXECUTE FUNCTION fn_audit_historique('id_client');

CREATE TRIGGER trg_audit_facturation
AFTER INSERT OR UPDATE OR DELETE ON facturation
FOR EACH ROW EXECUTE FUNCTION fn_audit_historique('id_facturation');

CREATE TRIGGER trg_audit_ligne_facturation
AFTER INSERT OR UPDATE OR DELETE ON ligne_facturation
FOR EACH ROW EXECUTE FUNCTION fn_audit_historique('id_ligne');

CREATE TRIGGER trg_audit_facture_pdf
AFTER INSERT OR UPDATE OR DELETE ON facture_pdf
FOR EACH ROW EXECUTE FUNCTION fn_audit_historique('id_pdf');

CREATE OR REPLACE FUNCTION fn_verifier_immutabilite_facturation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION
            'Opération interdite : la facturation % est validée et ne peut pas être supprimée.',
            OLD.id_facturation;
    END IF;

    -- Cas explicitement autorisé : réouverture d'une facturation validée
    -- (VALIDEE -> BROUILLON), et RIEN D'AUTRE dans la même opération.
    -- Toute colonne autre que statut / date_validation / valide_par / updated_at
    -- doit rester strictement identique, sans quoi on pourrait "profiter" de la
    -- réouverture pour glisser une modification de numero_facture, de dates,
    -- de montants, etc. Le trigger d'audit (fn_audit_historique) trace cette
    -- transition sous l'action REOUVERTURE_MOIS.
    IF NEW.statut = 'BROUILLON'
       AND NEW.id_entreprise IS NOT DISTINCT FROM OLD.id_entreprise
       AND NEW.numero_facture IS NOT DISTINCT FROM OLD.numero_facture
       AND NEW.annee IS NOT DISTINCT FROM OLD.annee
       AND NEW.mois IS NOT DISTINCT FROM OLD.mois
       AND NEW.date_debut_periode IS NOT DISTINCT FROM OLD.date_debut_periode
       AND NEW.date_fin_periode IS NOT DISTINCT FROM OLD.date_fin_periode
       AND NEW.cree_par IS NOT DISTINCT FROM OLD.cree_par
       AND NEW.commentaire IS NOT DISTINCT FROM OLD.commentaire
       AND NEW.created_at IS NOT DISTINCT FROM OLD.created_at
       AND NEW.deleted_at IS NOT DISTINCT FROM OLD.deleted_at
    THEN
        RETURN NEW;
    END IF;

    -- Toute autre modification d'une facturation déjà validée est interdite
    -- (dates, montants, numéro, etc., y compris combinée à une réouverture)
    RAISE EXCEPTION
        'Opération interdite : la facturation % est validée. Seule une réouverture pure (statut -> BROUILLON, sans autre changement) est autorisée.',
        OLD.id_facturation;
END;
$$;

CREATE TRIGGER trg_lock_facturation_validee
BEFORE UPDATE OR DELETE ON facturation
FOR EACH ROW
WHEN (OLD.statut = 'VALIDEE')
EXECUTE FUNCTION fn_verifier_immutabilite_facturation();

CREATE OR REPLACE FUNCTION fn_verifier_immutabilite_ligne()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_statut statut_facturation;
    v_id_facturation UUID;
BEGIN
    -- Pour un INSERT, la ligne à vérifier est NEW (OLD n'existe pas encore) ;
    -- pour UPDATE/DELETE, on se base sur l'ancienne ligne (OLD).
    v_id_facturation := CASE WHEN TG_OP = 'INSERT' THEN NEW.id_facturation ELSE OLD.id_facturation END;

    SELECT statut INTO v_statut
    FROM facturation
    WHERE id_facturation = v_id_facturation;

    IF v_statut = 'VALIDEE' THEN
        RAISE EXCEPTION
            'Opération interdite : la facturation parent % est validée, aucune ligne ne peut y être ajoutée, modifiée ou supprimée.',
            v_id_facturation;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_lock_ligne_facturation_validee
BEFORE INSERT OR UPDATE OR DELETE ON ligne_facturation
FOR EACH ROW
EXECUTE FUNCTION fn_verifier_immutabilite_ligne();
