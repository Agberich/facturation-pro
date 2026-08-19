-- =============================================================================
-- V3__Indexes_Views.sql
-- SPRINT 1 - Base de données
-- =============================================================================

SET search_path TO app_facturation, public;

CREATE INDEX idx_entreprise_nom ON entreprise(nom);
CREATE INDEX idx_utilisateur_entreprise ON utilisateur(id_entreprise);
CREATE INDEX idx_client_entreprise ON client(id_entreprise);
CREATE INDEX idx_client_nom_prenom ON client(nom, prenom);
CREATE INDEX idx_client_actif ON client(actif) WHERE deleted_at IS NULL;
CREATE INDEX idx_facturation_periode ON facturation(id_entreprise, annee, mois);
CREATE INDEX idx_facturation_statut ON facturation(statut);
CREATE INDEX idx_ligne_facturation_facture ON ligne_facturation(id_facturation);
CREATE INDEX idx_ligne_facturation_client ON ligne_facturation(id_client);
CREATE INDEX idx_pdf_facturation ON facture_pdf(id_facturation);
CREATE INDEX idx_historique_entreprise ON historique(id_entreprise);
CREATE INDEX idx_historique_table ON historique(nom_table, id_enregistrement);
CREATE INDEX idx_historique_date ON historique(created_at);
CREATE INDEX idx_historique_action ON historique(action);

CREATE OR REPLACE VIEW vw_resume_facturation AS
SELECT
    f.id_facturation,
    f.id_entreprise,
    f.numero_facture,
    f.annee,
    f.mois,
    f.statut,
    COUNT(l.id_ligne) AS total_clients,
    COALESCE(SUM(l.nb_jours), 0) AS total_jours,
    COALESCE(SUM(l.montant_ht), 0) AS total_ht,
    COALESCE(SUM(l.montant_tva), 0) AS total_tva,
    COALESCE(SUM(l.montant_ttc), 0) AS total_ttc
FROM facturation f
LEFT JOIN ligne_facturation l ON l.id_facturation = f.id_facturation
WHERE f.deleted_at IS NULL
GROUP BY f.id_facturation, f.id_entreprise, f.numero_facture, f.annee, f.mois, f.statut;

CREATE OR REPLACE VIEW vw_clients_actifs AS
SELECT *
FROM client
WHERE actif = TRUE AND deleted_at IS NULL;

CREATE OR REPLACE VIEW vw_derniere_facturation AS
SELECT *
FROM facturation
WHERE deleted_at IS NULL
ORDER BY annee DESC, mois DESC;

CREATE OR REPLACE VIEW vw_chiffre_affaires AS
SELECT
    f.id_entreprise,
    f.annee,
    f.mois,
    COALESCE(SUM(l.montant_ht), 0) AS ht,
    COALESCE(SUM(l.montant_tva), 0) AS tva,
    COALESCE(SUM(l.montant_ttc), 0) AS ttc
FROM facturation f
JOIN ligne_facturation l ON f.id_facturation = l.id_facturation
WHERE f.deleted_at IS NULL
GROUP BY f.id_entreprise, f.annee, f.mois
ORDER BY f.id_entreprise, f.annee, f.mois;
