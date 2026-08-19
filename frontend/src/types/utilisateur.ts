export type RoleUtilisateur = 'ADMIN' | 'COMPTABLE' | 'CONSULTATION';

export interface UtilisateurResponse {
  idUtilisateur: string;
  nom: string;
  email: string;
  role: RoleUtilisateur;
  actif: boolean;
  idEntreprise: string;
  derniereConnexion?: string;
  createdAt: string;
}

export interface UtilisateurRequest {
  nom: string;
  email: string;
  motDePasse?: string;
  role: RoleUtilisateur;
  actif: boolean;
}