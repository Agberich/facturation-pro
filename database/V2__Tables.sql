-- =============================================================================
-- V2__Tables.sql
-- SPRINT 1 - Base de données
-- =============================================================================

SET search_path TO app_facturation, public;

CREATE TABLE entreprise (
    id_entreprise UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL,
    raison_sociale VARCHAR(200),
    adresse TEXT,
    telephone VARCHAR(30),
    email VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_entreprise_email CHECK (
        email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    )
);

CREATE TABLE parametre (
    id_parametre UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_entreprise UUID NOT NULL UNIQUE REFERENCES entreprise(id_entreprise) ON DELETE CASCADE,
    taux_tva NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    devise devise NOT NULL DEFAULT 'XOF',
    prefixe_facture VARCHAR(20) NOT NULL DEFAULT 'FAC',
    adresse TEXT,
    telephone VARCHAR(30),
    email VARCHAR(150),
    logo TEXT,
    signature TEXT,
    signature_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_taux_tva CHECK (taux_tva BETWEEN 0 AND 100),
    CONSTRAINT chk_parametre_email CHECK (
        email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    )
);

CREATE TABLE utilisateur (
    id_utilisateur UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_entreprise UUID NOT NULL REFERENCES entreprise(id_entreprise) ON DELETE CASCADE,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    role role_utilisateur NOT NULL DEFAULT 'COMPTABLE',
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    derniere_connexion TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_utilisateur_email CHECK (
        email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    )
);

CREATE TABLE client (
    id_client UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_entreprise UUID NOT NULL REFERENCES entreprise(id_entreprise) ON DELETE CASCADE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE,
    date_entree DATE NOT NULL,
    date_sortie DATE,
    tarif_par_defaut NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    commentaire TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_tarif_client CHECK (tarif_par_defaut >= 0),
    CONSTRAINT chk_dates_client CHECK (date_sortie IS NULL OR date_sortie >= date_entree)
);

CREATE TABLE facturation (
    id_facturation UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_entreprise UUID NOT NULL REFERENCES entreprise(id_entreprise) ON DELETE CASCADE,
    numero_facture VARCHAR(30),
    annee INTEGER NOT NULL,
    mois INTEGER NOT NULL,
    date_debut_periode DATE NOT NULL,
    date_fin_periode DATE NOT NULL,
    statut statut_facturation NOT NULL DEFAULT 'BROUILLON',
    cree_par UUID REFERENCES utilisateur(id_utilisateur) ON DELETE SET NULL,
    valide_par UUID REFERENCES utilisateur(id_utilisateur) ON DELETE SET NULL,
    date_validation TIMESTAMPTZ,
    commentaire TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_facturation UNIQUE (id_entreprise, annee, mois),
    CONSTRAINT uq_facturation_numero UNIQUE (id_entreprise, numero_facture),
    CONSTRAINT chk_annee CHECK (annee BETWEEN 2000 AND 2100),
    CONSTRAINT chk_mois CHECK (mois BETWEEN 1 AND 12),
    CONSTRAINT chk_dates_facturation CHECK (date_fin_periode >= date_debut_periode)
);

CREATE TABLE ligne_facturation (
    id_ligne UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_facturation UUID NOT NULL REFERENCES facturation(id_facturation) ON DELETE CASCADE,
    id_client UUID NOT NULL REFERENCES client(id_client) ON DELETE RESTRICT,
    date_facture DATE NOT NULL DEFAULT CURRENT_DATE,
    date_entree_effective DATE NOT NULL,
    date_sortie_effective DATE,
    dernier_jour_mois DATE NOT NULL,
    tarif_applique NUMERIC(12,2) NOT NULL,
    nb_jours INTEGER NOT NULL DEFAULT 0,
    montant_ht NUMERIC(12,2) NOT NULL DEFAULT 0,
    montant_tva NUMERIC(12,2) NOT NULL DEFAULT 0,
    montant_ttc NUMERIC(12,2) NOT NULL DEFAULT 0,
    statut statut_ligne_facturation NOT NULL DEFAULT 'ACTIF',
    ordre_affichage INTEGER NOT NULL DEFAULT 1,
    observation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_ligne_client_facturation UNIQUE (id_facturation, id_client),
    CONSTRAINT chk_tarif CHECK (tarif_applique >= 0),
    CONSTRAINT chk_nb_jours CHECK (nb_jours >= 0),
    CONSTRAINT chk_ht CHECK (montant_ht >= 0),
    CONSTRAINT chk_tva CHECK (montant_tva >= 0),
    CONSTRAINT chk_ttc CHECK (montant_ttc >= 0),
    CONSTRAINT chk_dates_ligne CHECK (
        date_sortie_effective IS NULL OR date_sortie_effective >= date_entree_effective
    )
);

CREATE TABLE facture_pdf (
    id_pdf UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_facturation UUID NOT NULL REFERENCES facturation(id_facturation) ON DELETE CASCADE,
    version INTEGER NOT NULL DEFAULT 1,
    format_fichier VARCHAR(10) NOT NULL DEFAULT 'PDF',
    nom_fichier VARCHAR(255) NOT NULL,
    chemin_fichier TEXT NOT NULL,
    taille_fichier BIGINT,
    hash_sha256 VARCHAR(64),
    genere_par UUID REFERENCES utilisateur(id_utilisateur) ON DELETE SET NULL,
    date_generation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_format_fichier CHECK (format_fichier IN ('PDF','XLSX','CSV')),
    CONSTRAINT chk_version CHECK (version >= 1),
    CONSTRAINT chk_taille_fichier CHECK (taille_fichier IS NULL OR taille_fichier >= 0)
);

CREATE TABLE historique (
    id_historique UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_utilisateur UUID REFERENCES utilisateur(id_utilisateur) ON DELETE SET NULL,
    id_entreprise UUID,
    action type_action_audit NOT NULL,
    nom_table VARCHAR(100) NOT NULL,
    id_enregistrement UUID NOT NULL,
    ancienne_valeur JSONB,
    nouvelle_valeur JSONB,
    description TEXT,
    adresse_ip VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
