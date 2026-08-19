export interface Client {
  idClient?: string;
  nom: string;
  prenom: string;
  dateNaissance?: string;
  dateEntree?: string;
  dateSortie?: string;
  tarifParDefaut: number;
  actif: boolean;
  commentaire?: string;
}

export type StatutFacturation = 'BROUILLON' | 'VALIDEE' | 'ARCHIVE';

export type StatutLigne = 'ACTIF' | 'NOUVEAU' | 'SORTI' | 'SUSPENDU';

export interface Facturation {
  idFacturation: string;
  numeroFacture?: string;
  annee: number;
  mois: number;
  statut: StatutFacturation;
  dateValidation?: string;
  commentaire?: string;
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
  prefixeFacture: string;
  adresse?: string;
  telephone?: string;
  email?: string;
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