-- =============================================================================
-- V6__Seed.sql
-- SPRINT 1 - Base de données
-- Données initiales de démonstration
-- =============================================================================

SET search_path TO app_facturation, public;

INSERT INTO entreprise (
    id_entreprise, nom, raison_sociale, adresse, telephone, email
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Ma Structure',
    'Ma Structure SARL',
    'Dakar - Sénégal',
    '+221770000000',
    'contact@mastructure.sn'
);

INSERT INTO parametre (
    id_entreprise, taux_tva, devise, prefixe_facture,
    adresse, telephone, email
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    18.00,
    'XOF',
    'FAC',
    'Dakar - Sénégal',
    '+221770000000',
    'contact@mastructure.sn'
);

-- Mot de passe de démonstration : Admin@123
-- À remplacer avant toute utilisation réelle.
INSERT INTO utilisateur (
    id_entreprise, nom, email, mot_de_passe_hash, role
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Administrateur',
    'admin@facturation.local',
    '$2a$10$8lMvbpcllLfX/lAqISRDV.sMVJ20eEtoKtJFt4G9KwbmGJGQaTGLy',
    'ADMIN'
);

INSERT INTO client (
    id_entreprise, nom, prenom, date_naissance, date_entree,
    tarif_par_defaut, actif
) VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'DUPONT', 'Jean', '1950-05-15', '2026-01-10', 45.00, TRUE
),
(
    '11111111-1111-1111-1111-111111111111',
    'MARTIN', 'Claire', '1947-11-02', '2026-03-01', 50.00, TRUE
);
