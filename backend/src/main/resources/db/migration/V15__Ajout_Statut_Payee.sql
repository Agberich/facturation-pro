-- =============================================================================
-- V15__Ajout_Statut_Payee.sql
-- Ajoute le statut de facturation PAYEE et deux actions d'historique.
-- Migration volontairement SEPAREE de V16 : PostgreSQL interdit d'utiliser une
-- valeur d'enum ajoutee dans la meme transaction que son ALTER TYPE.
-- =============================================================================

ALTER TYPE app_facturation.statut_facturation ADD VALUE IF NOT EXISTS 'PAYEE';
ALTER TYPE app_facturation.type_action_audit ADD VALUE IF NOT EXISTS 'PAIEMENT_FACTURE';
ALTER TYPE app_facturation.type_action_audit ADD VALUE IF NOT EXISTS 'ANNULATION_PAIEMENT';
