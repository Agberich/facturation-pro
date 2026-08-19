import axios from 'axios';
import { Client, Facturation, LigneFacturation, Parametre, ImportClientApercu, ConnexionReponse } from '../types/facturation';
// Le backend Spring Boot n'expose pas de préfixe de version : tous les
// controllers sont sous /api (voir @RequestMapping des controllers Java).
const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Permet de propager l'entreprise / l'utilisateur courants au backend,
// utilisés notamment par ContexteRequeteFilter pour l'audit et l'import.
export const definirContexte = (idEntreprise?: string, idUtilisateur?: string) => {
  if (idEntreprise) api.defaults.headers.common['X-Id-Entreprise'] = idEntreprise;
  if (idUtilisateur) api.defaults.headers.common['X-Id-Utilisateur'] = idUtilisateur;
};

export const ClientServiceAPI = {
  getListeClients: async (idEntreprise: string, includeInactive: boolean = false): Promise<Client[]> => {
    const response = await api.get(`/clients/entreprise/${idEntreprise}`, {
      params: { includeInactive }
    });
    return response.data;
  },

  creerClient: async (idEntreprise: string, client: Client): Promise<Client> => {
    const response = await api.post(`/clients/entreprise/${idEntreprise}`, client);
    return response.data;
  },

  modifierClient: async (idEntreprise: string, idClient: string, client: Client): Promise<Client> => {
    const response = await api.put(`/clients/${idClient}`, client);
    return response.data;
  },

  desactiverClient: async (idClient: string): Promise<void> => {
    await api.put(`/clients/${idClient}/desactiver`);
  },

  reactiverClient: async (idClient: string): Promise<void> => {
    await api.put(`/clients/${idClient}/reactiver`);
  },
};

export const FacturationServiceAPI = {
  listerFacturations: async (idEntreprise: string): Promise<Facturation[]> => {
    const response = await api.get(`/facturations/entreprise/${idEntreprise}`);
    return response.data;
  },

  obtenirFacturation: async (idFacturation: string): Promise<Facturation> => {
    const response = await api.get(`/facturations/${idFacturation}`);
    return response.data;
  },

  listerLignes: async (idFacturation: string): Promise<LigneFacturation[]> => {
    const response = await api.get(`/facturations/${idFacturation}/lignes`);
    return response.data;
  },

  initialiserMois: async (idEntreprise: string, annee: number, mois: number): Promise<Facturation> => {
    const response = await api.post(`/facturations/entreprise/${idEntreprise}/initialiser`, null, {
      params: { annee, mois },
    });
    return response.data;
  },

  genererLignes: async (idFacturation: string): Promise<LigneFacturation[]> => {
    const response = await api.post(`/facturations/${idFacturation}/generer-lignes`);
    return response.data;
  },

  validerFacture: async (idFacturation: string): Promise<Facturation> => {
    const response = await api.put(`/facturations/${idFacturation}/valider`);
    return response.data;
  },

  reouvrirFacture: async (idFacturation: string): Promise<Facturation> => {
    const response = await api.put(`/facturations/${idFacturation}/reouvrir`);
    return response.data;
  },
};

export const ParametreServiceAPI = {
  obtenirParametres: async (idEntreprise: string): Promise<Parametre> => {
    const response = await api.get(`/parametres/entreprise/${idEntreprise}`);
    return response.data;
  },

  mettreAJourParametres: async (idEntreprise: string, parametre: Parametre): Promise<Parametre> => {
    const response = await api.put(`/parametres/entreprise/${idEntreprise}`, parametre);
    return response.data;
  },
};

export const ExportServiceAPI = {
  urlPdf: (idFacturation: string) => `${API_BASE_URL}/export/factures/${idFacturation}/pdf`,
  urlExcel: (idFacturation: string) => `${API_BASE_URL}/export/factures/${idFacturation}/excel`,
  urlCsv: (idFacturation: string) => `${API_BASE_URL}/export/factures/${idFacturation}/csv`,
};

export const ImportServiceAPI = {
  apercuClients: async (fichier: File): Promise<ImportClientApercu[]> => {
    const formData = new FormData();
    formData.append('file', fichier);
    const response = await api.post('/import/clients/apercu', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  importerClients: async (fichier: File): Promise<Client[]> => {
    const formData = new FormData();
    formData.append('file', fichier);
    const response = await api.post('/import/clients', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },
};
export const AuthServiceAPI = {
  connexion: async (email: string, motDePasse: string): Promise<ConnexionReponse> => {
    const response = await api.post('/auth/login', { email, motDePasse });
    return response.data;
  },
};

/** Attache (ou retire) le jeton JWT sur toutes les requêtes suivantes. */
export const definirJeton = (token: string | null) => {
  if (token) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common['Authorization'];
  }
};
export default api;