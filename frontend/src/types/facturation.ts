export interface Client {
  idClient?: string;
  nom: string;
  prenom: string;
  dateNaissance?: string;
  dateEntree?: string;
  dateSortie?: string;
  tarifParDefaut?: number;
  actif: boolean;
  commentaire?: string;
}

export type StatutFacturation = 'BROUILLON' | 'VALIDEE' | 'PAYEE' | 'ARCHIVE';

export type StatutLigne = 'ACTIF' | 'NOUVEAU' | 'SORTI' | 'SUSPENDU';

export interface Facturation {
  idFacturation: string;
  numeroFacture?: string;
  annee: number;
  mois: number;
  statut: StatutFacturation;
  dateValidation?: string;
  datePaiement?: string;
  commentaire?: string;
}

/** Totaux d'une facturation (GET /facturations/entreprise/{id}/resumes). */
export interface ResumeFacturation {
  idFacturation: string;
  nbPersonnes: number;
  totalHt: number;
  totalTva: number;
  totalTtc: number;
}

export interface LigneFacturation {
  idLigne?: string;
  client: Client;
  tarifApplique: number;
  nbJours: number;
  montantHt: number;
  montantTva: number;
  montantTtc: number;
  statut: StatutLigne;
}

export interface Parametre {
  idParametre?: string;
  idEntreprise?: string;
  nomEntreprise?: string;
  tauxTva: number;
  devise: 'XOF' | 'EUR' | 'USD';
  tarifJournalier: number;
  prefixeFacture: string;
  adresse?: string;
  telephone?: string;
  email?: string;
  /** Image PNG/JPEG en base64 (adresse « data: »). */
  logoUrl?: string;
  signatureUrl?: string;
  dispositif?: string;
  typePrestation?: string;
  categorieEtablissement?: string;
  discipline?: string;
  modeFonctionnement?: string;
  publicAccueilli?: string;
  centreProfit?: string;
  financeurNom?: string;
  financeurService?: string;
  financeurAdresse?: string;
  financeurEmail?: string;
  financeurSiret?: string;
  numeroEngagement?: string;
  fournisseurSiret?: string;
  directionTerritoriale?: string;
  iban?: string;
  mentionReglement?: string;
  contactUt?: string;
  interlocuteur?: string;
  fonctionInterlocuteur?: string;
  contactDispositif?: string;
  capacite?: number;
  /** Mois (1-12) où commence le trimestre 1 de la prestation (4 = avril). */
  premierMoisPrestation?: number;
}
/** Ligne brute d'aperçu retournée par POST /api/import/clients/apercu, avant
 * conversion en Client. Toutes les valeurs sont des chaînes non typées côté
 * backend (voir ImportClientDTO.java). */
export interface ImportClientApercu {
  nom: string;
  prenom: string;
  dateNaissance: string;
  dateEntree: string;
  tarifParDefaut: string;
  commentaire: string;
}
export interface Utilisateur {
  idUtilisateur: string;
  nom: string;
  email: string;
  role: 'ADMIN' | 'COMPTABLE' | 'CONSULTATION';
  idEntreprise: string;
}

export interface ConnexionReponse extends Utilisateur {
  token: string;
}