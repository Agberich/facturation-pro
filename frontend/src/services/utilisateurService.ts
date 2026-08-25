import axios from 'axios';
import { UtilisateurRequest, UtilisateurResponse } from '../types/utilisateur';

// Remplacement de localhost par l'URL Render en ligne
const API_URL = 'https://facturation-pro-c14q.onrender.com/api/utilisateurs';

// Configuration de l'intercepteur pour ajouter le token JWT à chaque requête
const getAuthHeaders = () => {
  const auth = localStorage.getItem('facturation_auth');

  let token: string | null = null;

  if (auth) {
    try {
      token = JSON.parse(auth).token;
    } catch (e) {
      console.error('Erreur lecture facturation_auth', e);
    }
  }

  return {
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
      'Content-Type': 'application/json',
    },
  };
};

export const utilisateurService = {
  listerParEntreprise: async (idEntreprise: string): Promise<UtilisateurResponse[]> => {
    const response = await axios.get(`${API_URL}/entreprise/${idEntreprise}`, getAuthHeaders());
    return response.data;
  },

  creer: async (idEntreprise: string, data: UtilisateurRequest): Promise<UtilisateurResponse> => {
    const response = await axios.post(`${API_URL}/entreprise/${idEntreprise}`, data, getAuthHeaders());
    return response.data;
  },

  modifier: async (idUtilisateur: string, data: UtilisateurRequest): Promise<UtilisateurResponse> => {
    const response = await axios.put(`${API_URL}/${idUtilisateur}`, data, getAuthHeaders());
    return response.data;
  },

  desactiver: async (idUtilisateur: string): Promise<void> => {
    await axios.patch(`${API_URL}/${idUtilisateur}/desactiver`, {}, getAuthHeaders());
  },

  reactiver: async (idUtilisateur: string): Promise<void> => {
    await axios.patch(`${API_URL}/${idUtilisateur}/reactiver`, {}, getAuthHeaders());
  },

  supprimer: async (idUtilisateur: string): Promise<void> => {
    await axios.delete(`${API_URL}/${idUtilisateur}`, getAuthHeaders());
  },
};