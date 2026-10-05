-- =============================================================================
-- V18__Parametres_Facture_Prix_Journee.sql
-- 1. Prix de journée TTC : 79,92 par défaut (au lieu de 79,91). Les paramètres
--    qui ont encore EXACTEMENT l'ancienne valeur par défaut passent à 79,92 ;
--    une valeur saisie à la main est conservée.
-- 2. Informations imprimées sur la facture (modèle FACTURATION_2026.xlsx) :
--    prestation / dispositif, financeur, fournisseur, références bancaires,
--    contacts. Toutes facultatives : une facture sans ces informations reste
--    valide, les blocs vides ne sont simplement pas imprimés.
-- Le logo (parametre.logo) et la signature (parametre.signature_url) existent
-- déjà en base ; ils reçoivent désormais une image enregistrée depuis l'écran
-- Paramètres.
-- =============================================================================

SET search_path TO app_facturation, public;

ALTER TABLE parametre ALTER COLUMN tarif_journalier SET DEFAULT 79.92;
UPDATE parametre SET tarif_journalier = 79.92 WHERE tarif_journalier = 79.91;

-- Prestation / dispositif
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS dispositif              VARCHAR(150);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS type_prestation         VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS categorie_etablissement VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS discipline              VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS mode_fonctionnement     VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS public_accueilli        VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS capacite                INTEGER;
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS centre_profit           VARCHAR(150);

-- Financeur (destinataire de la facture)
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS financeur_nom           VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS financeur_service       VARCHAR(300);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS financeur_adresse       TEXT;
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS financeur_email         VARCHAR(150);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS financeur_siret         VARCHAR(20);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS numero_engagement       VARCHAR(60);

-- Fournisseur (nom, adresse, téléphone, e-mail : déjà dans entreprise)
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS fournisseur_siret       VARCHAR(20);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS direction_territoriale  VARCHAR(200);

-- Références bancaires
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS iban                    VARCHAR(40);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS mention_reglement       VARCHAR(200)
    DEFAULT 'Règlement par virement à réception';

-- Contacts
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS contact_ut              VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS interlocuteur           VARCHAR(200);
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS fonction_interlocuteur  VARCHAR(100)
    DEFAULT 'Chef de service';
ALTER TABLE parametre ADD COLUMN IF NOT EXISTS contact_dispositif      VARCHAR(200);

ALTER TABLE parametre DROP CONSTRAINT IF EXISTS chk_capacite_positive;
ALTER TABLE parametre ADD CONSTRAINT chk_capacite_positive CHECK (capacite IS NULL OR capacite >= 0);
