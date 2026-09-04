SET search_path TO app_facturation, public;

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
    FROM app_facturation.ligne_facturation lf
    JOIN app_facturation.facturation f
        ON f.id_facturation = lf.id_facturation
    LEFT JOIN app_facturation.parametre p
        ON p.id_entreprise = f.id_entreprise
    WHERE lf.id_ligne = p_id_ligne;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Ligne de facturation introuvable : %', p_id_ligne;
    END IF;

    v_jours := calculer_nb_jours_factures(v_entree, v_sortie, v_debut, v_fin);
    v_ht := calculer_montant_ht(v_tarif, v_jours);
    v_tva := calculer_tva(v_ht, v_taux_tva);
    v_ttc := calculer_ttc(v_ht, v_tva);

    UPDATE app_facturation.ligne_facturation
    SET nb_jours = v_jours,
        montant_ht = v_ht,
        montant_tva = v_tva,
        montant_ttc = v_ttc
    WHERE id_ligne = p_id_ligne;
END;
$$;