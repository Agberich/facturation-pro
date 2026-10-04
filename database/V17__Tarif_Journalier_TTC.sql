-- =============================================================================
-- V17__Tarif_Journalier_TTC.sql
-- Le tarif journalier saisi (parametre.tarif_journalier, copie dans
-- ligne_facturation.tarif_applique) est desormais un prix TTC : la TVA y est
-- DEJA incluse et ne doit pas etre ajoutee une seconde fois.
--
-- Ancien calcul : HT = tarif x jours ; TVA = HT x taux ; TTC = HT + TVA
-- Nouveau calcul : TTC = tarif x jours ; HT = TTC / (1 + taux) ; TVA = TTC - HT
--
-- TVA est calculee par difference : HT + TVA = TTC exactement, sans ecart
-- d'arrondi, ligne par ligne comme sur les totaux.
--
-- Cette migration ne modifie QUE la fonction. Les lignes existantes ne sont pas
-- recalculees : les factures en brouillon se recalculent avec le bouton
-- "Generer / recalculer" ; les factures validees ou payees sont verrouillees et
-- gardent leurs montants.
-- =============================================================================

CREATE OR REPLACE FUNCTION app_facturation.recalculer_ligne_facturation(p_id_ligne UUID)
RETURNS VOID
LANGUAGE plpgsql
SET search_path = app_facturation, public
AS $$
DECLARE
    v_tarif_ttc NUMERIC;
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
    INTO v_tarif_ttc, v_entree, v_sortie, v_debut, v_fin, v_taux_tva
    FROM app_facturation.ligne_facturation lf
    JOIN app_facturation.facturation f
        ON f.id_facturation = lf.id_facturation
    LEFT JOIN app_facturation.parametre p
        ON p.id_entreprise = f.id_entreprise
    WHERE lf.id_ligne = p_id_ligne;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Ligne de facturation introuvable : %', p_id_ligne;
    END IF;

    v_jours := app_facturation.calculer_nb_jours_factures(v_entree, v_sortie, v_debut, v_fin);

    -- Le tarif est TTC : on part du TTC et on en extrait HT et TVA.
    v_ttc := ROUND(COALESCE(v_tarif_ttc, 0) * COALESCE(v_jours, 0), 2);
    v_ht  := ROUND(v_ttc / (1 + COALESCE(v_taux_tva, 0) / 100), 2);
    v_tva := v_ttc - v_ht;

    UPDATE app_facturation.ligne_facturation
    SET nb_jours = v_jours,
        montant_ht = v_ht,
        montant_tva = v_tva,
        montant_ttc = v_ttc
    WHERE id_ligne = p_id_ligne;
END;
$$;
