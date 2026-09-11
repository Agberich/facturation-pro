-- =============================================================================
-- V13__Correction_Generation_Numero_Facture.sql
-- La fonction generer_numero_facture() reellement deployee calculait le
-- numero via "SELECT COUNT(*) + 1 FROM facturation WHERE ...annee=...",
-- en comptant TOUTES les periodes ouvertes (brouillons compris), sans
-- jamais regarder les numeros deja attribues, et sans aucun verrou.
-- Deux validations dont le COUNT(*) tombe sur la meme valeur -> le meme
-- numero -> violation de la contrainte uq_facturation_numero.
--
-- Cette version utilise une sequence PostgreSQL dediee par (entreprise,
-- annee), atomique par construction (deux appels concurrents ne peuvent
-- jamais recevoir la meme valeur de nextval()).
--
-- Format du numero aligne sur l'exemple affiche dans l'ecran Parametres
-- (apercu "FAC-2026-00042") : PREFIXE + "-" + ANNEE + "-" + 5 chiffres,
-- ex. FAC-2026-00005. C'est un changement de format par rapport aux
-- factures deja emises (ex. FAC2026-0005, sans tiret, 4 chiffres) : ces
-- anciens numeros ne sont PAS renommes retroactivement, seules les
-- prochaines factures validees utiliseront le nouveau format.
--
-- Au premier appel pour une entreprise/annee donnee, la sequence est
-- initialisee a partir du plus grand numero deja attribue, quel que soit
-- son format (ancien ou nouveau) : pas de redemarrage a 1, donc pas de
-- collision avec des numeros existants.
-- =============================================================================

SET search_path TO app_facturation, public;

CREATE OR REPLACE FUNCTION app_facturation.generer_numero_facture(p_id_entreprise UUID, p_annee INTEGER)
RETURNS VARCHAR
LANGUAGE plpgsql
AS $$
DECLARE
    v_prefixe VARCHAR;
    v_sequence_name TEXT;
    v_prochain BIGINT;
    v_max_existant INTEGER;
BEGIN
    SELECT COALESCE(NULLIF(TRIM(prefixe_facture), ''), 'FAC') INTO v_prefixe
    FROM app_facturation.parametre
    WHERE id_entreprise = p_id_entreprise;

    IF v_prefixe IS NULL THEN
        v_prefixe := 'FAC';
    END IF;

    v_sequence_name := 'app_facturation.seq_facture_' || replace(p_id_entreprise::text, '-', '_')
                        || '_' || p_annee::text;

    IF to_regclass(v_sequence_name) IS NULL THEN
        -- La sequence n'existe pas encore pour cette entreprise/annee :
        -- on l'initialise juste apres le plus grand numero deja attribue,
        -- peu importe son format (ancien "FAC2026-0005" sans tiret ou
        -- nouveau "FAC-2026-00005" avec tiret), pour ne jamais reproduire
        -- un numero deja utilise.
        SELECT COALESCE(MAX(
            NULLIF(regexp_replace(numero_facture, '^\D*\d{4}-?(\d+)$', '\1'), '')::INTEGER
        ), 0)
        INTO v_max_existant
        FROM app_facturation.facturation
        WHERE id_entreprise = p_id_entreprise
          AND annee = p_annee
          AND numero_facture IS NOT NULL;

        EXECUTE format(
            'CREATE SEQUENCE %s START WITH %s INCREMENT BY 1',
            v_sequence_name, v_max_existant + 1
        );
    END IF;

    v_prochain := nextval(v_sequence_name::regclass);

    RETURN v_prefixe || '-' || p_annee::text || '-' || lpad(v_prochain::text, 5, '0');
END;
$$;