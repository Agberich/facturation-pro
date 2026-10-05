-- =============================================================================
-- V19__Calcul_Centime_Numero_Mensuel_Trimestre.sql
--
-- 1. CALCUL AU CENTIME (modèle FACTURATION_2026.xlsx, feuille « facture JUIN »)
--      TTC total = somme des (prix de journée TTC x jours)
--      HT total  = ROUND(TTC total / (1 + taux de TVA), 2)
--      TVA total = TTC total - HT total
--    Le HT et la TVA sont donc calculés sur le TOTAL de la facture, pas ligne par
--    ligne. Pour que les lignes affichées s'additionnent EXACTEMENT au total,
--    les centimes du HT sont répartis entre les lignes (méthode du plus fort
--    reste) : somme des HT des lignes = HT total, TTC = HT + TVA sur chaque ligne.
--    Fonction repartir_montants_facturation(), appelée après le calcul des lignes.
--
-- 2. NUMÉRO DE FACTURE : PREFIXE-AAAA-MM (ex. C710-2026-06), comme le fichier.
--    Une facture par mois : le numéro est déterminé par le mois facturé. Si ce
--    numéro existe déjà (facture supprimée puis refaite), on ajoute -002, -003...
--    L'ancienne fonction generer_numero_facture() est conservée, plus utilisée.
--
-- 3. TRIMESTRE DE LA PRESTATION : paramètre « premier mois de la prestation »
--    (avril par défaut) ; le trimestre est calculé automatiquement depuis le mois.
-- =============================================================================

SET search_path TO app_facturation, public;

ALTER TABLE parametre ADD COLUMN IF NOT EXISTS premier_mois_prestation INTEGER NOT NULL DEFAULT 4;
ALTER TABLE parametre DROP CONSTRAINT IF EXISTS chk_premier_mois_prestation;
ALTER TABLE parametre ADD CONSTRAINT chk_premier_mois_prestation
    CHECK (premier_mois_prestation BETWEEN 1 AND 12);

-- -----------------------------------------------------------------------------
-- Répartition des centimes du HT entre les lignes d'une facturation
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_facturation.repartir_montants_facturation(p_id_facturation UUID)
RETURNS VOID
LANGUAGE plpgsql
SET search_path = app_facturation, public
AS $$
DECLARE
    v_taux NUMERIC;
    v_total_ttc NUMERIC;
    v_total_ht NUMERIC;
BEGIN
    SELECT COALESCE(p.taux_tva, 18.00)
    INTO v_taux
    FROM app_facturation.facturation f
    LEFT JOIN app_facturation.parametre p ON p.id_entreprise = f.id_entreprise
    WHERE f.id_facturation = p_id_facturation;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Facturation introuvable : %', p_id_facturation;
    END IF;

    SELECT COALESCE(SUM(montant_ttc), 0)
    INTO v_total_ttc
    FROM app_facturation.ligne_facturation
    WHERE id_facturation = p_id_facturation;

    -- HT total calculé sur le total TTC (règle du modèle)
    v_total_ht := ROUND(v_total_ttc / (1 + COALESCE(v_taux, 0) / 100), 2);

    WITH base AS (
        SELECT
            id_ligne,
            montant_ttc,
            FLOOR(montant_ttc / (1 + COALESCE(v_taux, 0) / 100) * 100) AS centimes_plancher,
            (montant_ttc / (1 + COALESCE(v_taux, 0) / 100) * 100)
                - FLOOR(montant_ttc / (1 + COALESCE(v_taux, 0) / 100) * 100) AS reste
        FROM app_facturation.ligne_facturation
        WHERE id_facturation = p_id_facturation
    ),
    a_repartir AS (
        SELECT (v_total_ht * 100) - COALESCE(SUM(centimes_plancher), 0) AS nb_centimes
        FROM base
    ),
    classement AS (
        SELECT
            id_ligne,
            montant_ttc,
            centimes_plancher,
            ROW_NUMBER() OVER (ORDER BY reste DESC, id_ligne) AS rang
        FROM base
    ),
    final AS (
        SELECT
            c.id_ligne,
            ((c.centimes_plancher
              + CASE WHEN c.rang <= (SELECT nb_centimes FROM a_repartir) THEN 1 ELSE 0 END) / 100)::NUMERIC(12,2) AS ht
        FROM classement c
    )
    UPDATE app_facturation.ligne_facturation l
    SET montant_ht = f.ht,
        montant_tva = l.montant_ttc - f.ht
    FROM final f
    WHERE l.id_ligne = f.id_ligne
      AND (l.montant_ht IS DISTINCT FROM f.ht
           OR l.montant_tva IS DISTINCT FROM (l.montant_ttc - f.ht));
END;
$$;

-- -----------------------------------------------------------------------------
-- Numéro de facture mensuel : PREFIXE-AAAA-MM
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION app_facturation.generer_numero_facture_mensuel(
    p_id_entreprise UUID, p_annee INTEGER, p_mois INTEGER)
RETURNS VARCHAR
LANGUAGE plpgsql
SET search_path = app_facturation, public
AS $$
DECLARE
    v_prefixe VARCHAR;
    v_base VARCHAR;
    v_candidat VARCHAR;
    v_rang INTEGER := 1;
BEGIN
    SELECT COALESCE(NULLIF(TRIM(prefixe_facture), ''), 'FAC')
    INTO v_prefixe
    FROM app_facturation.parametre
    WHERE id_entreprise = p_id_entreprise;

    IF v_prefixe IS NULL THEN
        v_prefixe := 'FAC';
    END IF;

    v_base := v_prefixe || '-' || p_annee::text || '-' || lpad(p_mois::text, 2, '0');
    v_candidat := v_base;

    -- Numéro déjà pris (y compris par une facture supprimée) : -002, -003, ...
    WHILE EXISTS (
        SELECT 1 FROM app_facturation.facturation
        WHERE id_entreprise = p_id_entreprise AND numero_facture = v_candidat
    ) LOOP
        v_rang := v_rang + 1;
        v_candidat := v_base || '-' || lpad(v_rang::text, 3, '0');
    END LOOP;

    RETURN v_candidat;
END;
$$;
