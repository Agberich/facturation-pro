-- =============================================================================
-- V1__Initialisation.sql
-- SPRINT 1 - Base de données
-- PostgreSQL 17+
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS app_facturation;
SET search_path TO app_facturation, public;

CREATE TYPE role_utilisateur AS ENUM (
    'ADMIN', 'COMPTABLE', 'CONSULTATION'
);

CREATE TYPE statut_facturation AS ENUM (
    'BROUILLON', 'VALIDEE', 'ARCHIVE'
);

CREATE TYPE statut_ligne_facturation AS ENUM (
    'ACTIF', 'NOUVEAU', 'SORTI', 'SUSPENDU'
);

CREATE TYPE type_action_audit AS ENUM (
    'CREATION',
    'MODIFICATION',
    'SUPPRESSION',
    'VALIDATION_MOIS',
    'REOUVERTURE_MOIS',
    'PREPARATION_MOIS',
    'GENERATION_FACTURES',
    'GENERATION_PDF',
    'EXPORT_EXCEL',
    'EXPORT_CSV',
    'EXPORT_PDF',
    'CONNEXION',
    'DECONNEXION'
);

CREATE TYPE devise AS ENUM ('XOF', 'EUR', 'USD');

CREATE OR REPLACE FUNCTION generate_uuid()
RETURNS UUID
LANGUAGE SQL
AS $$
    SELECT gen_random_uuid();
$$;

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

-- Alias conservé pour compatibilité avec d'anciens scripts.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

COMMENT ON SCHEMA app_facturation IS
'Schéma principal de l''application de facturation.';
