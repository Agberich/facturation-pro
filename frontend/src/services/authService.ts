import api from './api';

export interface UtilisateurResponse {
  id: string;
  nom: string;
  prenom?: string;
  email: string;
  role: string;
  entrepriseNom?: string;
  doitChangerMotDePasse?: boolean;
}

export interface LoginResponse {
  token: string;
  idEntreprise: string;
  idUtilisateur?: string;
  nom?: string;
  utilisateur?: UtilisateurResponse;
  doitChangerMotDePasse?: boolean;
}

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

export interface PremierAdminRequest {
  nomEntreprise: string;
  nom: string;
  prenom?: string;
  telephone?: string;
  email: string;
  motDePasse: string;
}

export interface PremierAdminExisteResponse {
  premierAdminExiste: boolean;
}

export interface ChangerMotDePasseRequest {
  ancienMotDePasse: string;
  nouveauMotDePasse: string;
}

export const AuthServiceAPI = {
  verifierPremierAdminExiste: async (): Promise<boolean> => {
    const response = await api.get<PremierAdminExisteResponse>('/auth/premier-admin/existe');
    return response.data.premierAdminExiste;
  },

  creerPremierAdmin: async (data: PremierAdminRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/premier-admin', data);
    return response.data;
  },

  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  },

  changerMotDePasse: async (data: ChangerMotDePasseRequest): Promise<void> => {
    await api.post('/auth/changer-mot-de-passe', data);
  },
};