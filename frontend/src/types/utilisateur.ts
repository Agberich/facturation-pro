export type RoleUtilisateur = 'ADMIN' | 'COMPTABLE' | 'CONSULTATION';

export interface UtilisateurResponse {
  idUtilisateur: string;
  nom: string;
  prenom?: string;
  telephone?: string;
  email: string;
  role: RoleUtilisateur;
  actif: boolean;
  doitChangerMotDePasse: boolean;
  idEntreprise: string;
  derniereConnexion?: string;
  createdAt: string;
}

export interface UtilisateurRequest {
  nom: string;
  prenom?: string;
  telephone?: string;
  email: string;
  motDePasse?: string;
  role: RoleUtilisateur;
  actif: boolean;
}