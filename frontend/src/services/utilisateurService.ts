import api from './api'; // Réutilisation du client Axios centralisé
import { UtilisateurRequest, UtilisateurResponse } from '../types/utilisateur';

export const utilisateurService = {
  listerParEntreprise: async (idEntreprise: string): Promise<UtilisateurResponse[]> => {
    const response = await api.get(`/utilisateurs/entreprise/${idEntreprise}`);
    return response.data;
  },

  creer: async (idEntreprise: string, data: UtilisateurRequest): Promise<UtilisateurResponse> => {
    const response = await api.post(`/utilisateurs/entreprise/${idEntreprise}`, data);
    return response.data;
  },

  modifier: async (idUtilisateur: string, data: UtilisateurRequest): Promise<UtilisateurResponse> => {
    const response = await api.put(`/utilisateurs/${idUtilisateur}`, data);
    return response.data;
  },

  desactiver: async (idUtilisateur: string): Promise<void> => {
    await api.patch(`/utilisateurs/${idUtilisateur}/desactiver`);
  },

  reactiver: async (idUtilisateur: string): Promise<void> => {
    await api.patch(`/utilisateurs/${idUtilisateur}/reactiver`);
  },

  supprimer: async (idUtilisateur: string): Promise<void> => {
    await api.delete(`/utilisateurs/${idUtilisateur}`);
  },
};