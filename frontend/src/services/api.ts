import axios from 'axios';
import { Client, Facturation, LigneFacturation, Parametre, ImportClientApercu, ConnexionReponse } from '../types/facturation';

// URL directe de ton backend sur Render (évite de cibler localhost en production)
const API_BASE_URL = 'https://facturation-pro-c14q.onrender.com/api';

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

  // --- ACTIONS EN MASSE (BULK) ---
  desactiverClientsEnMasse: async (idsClients: string[]): Promise<void> => {
    await api.put('/clients/desactiver', idsClients);
  },

  reactiverClientsEnMasse: async (idsClients: string[]): Promise<void> => {
    await api.put('/clients/reactiver', idsClients);
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

/** Service d'exportation sécurisé avec injection automatique du Token JWT */
export const ExportServiceAPI = {
  telechargerPdf: async (idFacturation: string, numeroFacture?: string): Promise<void> => {
    const response = await api.get(`/export/factures/${idFacturation}/pdf`, {
      responseType: 'blob'
    });
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `facture-${numeroFacture || idFacturation}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  telechargerExcel: async (idFacturation: string, numeroFacture?: string): Promise<void> => {
    const response = await api.get(`/export/factures/${idFacturation}/excel`, {
      responseType: 'blob'
    });
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `facture-${numeroFacture || idFacturation}.xlsx`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  telechargerCsv: async (idFacturation: string, numeroFacture?: string): Promise<void> => {
    const response = await api.get(`/export/factures/${idFacturation}/csv`, {
      responseType: 'blob'
    });
    const blob = new Blob([response.data], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `facture-${numeroFacture || idFacturation}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  // Rétrocompatibilité basée sur l'URL courante
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