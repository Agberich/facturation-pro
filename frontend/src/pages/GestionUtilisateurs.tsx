import React, { useEffect, useState } from 'react';
import { UtilisateurResponse, UtilisateurRequest } from '../types/utilisateur';
import { utilisateurService } from '../services/utilisateurService';
import { UtilisateurTable } from '../components/utilisateurs/UtilisateurTable';
import { UtilisateurForm } from '../components/utilisateurs/UtilisateurForm';

export const GestionUtilisateurs: React.FC = () => {
  const [utilisateurs, setUtilisateurs] = useState<UtilisateurResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [utilisateurAEditer, setUtilisateurAEditer] = useState<UtilisateurResponse | null>(null);

const idEntreprise = React.useMemo(() => {
  const auth = localStorage.getItem('facturation_auth');

  if (!auth) return '';

  try {
    return JSON.parse(auth).idEntreprise;
  } catch {
    return '';
  }
}, []);
  const chargerUtilisateurs = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await utilisateurService.listerParEntreprise(idEntreprise);
      setUtilisateurs(data);
    } catch (err: any) {
      setError('Impossible de charger la liste des utilisateurs.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
  if (!idEntreprise) {
    setError('Entreprise non identifiée.');
    setLoading(false);
    return;
  }

  chargerUtilisateurs();
}, [idEntreprise]);
  const handleOpenCreateModal = () => {
    setUtilisateurAEditer(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (utilisateur: UtilisateurResponse) => {
    setUtilisateurAEditer(utilisateur);
    setIsModalOpen(true);
  };

  const handleSubmitForm = async (data: UtilisateurRequest) => {
    if (utilisateurAEditer) {
      await utilisateurService.modifier(utilisateurAEditer.idUtilisateur, data);
    } else {
      await utilisateurService.creer(idEntreprise, data);
    }
    await chargerUtilisateurs();
  };

  const handleToggleStatut = async (utilisateur: UtilisateurResponse) => {
    try {
      if (utilisateur.actif) {
        await utilisateurService.desactiver(utilisateur.idUtilisateur);
      } else {
        await utilisateurService.reactiver(utilisateur.idUtilisateur);
      }
      await chargerUtilisateurs();
    } catch (err) {
      alert('Erreur lors du changement de statut.');
    }
  };

  const handleDelete = async (idUtilisateur: string) => {
    if (window.confirm('Êtes-vous sûr de vouloir supprimer cet utilisateur ?')) {
      try {
        await utilisateurService.supprimer(idUtilisateur);
        await chargerUtilisateurs();
      } catch (err) {
        alert("Erreur lors de la suppression de l'utilisateur.");
      }
    }
  };

  return (
        <div className="utilisateurs-page">
        <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Gestion des Utilisateurs</h1>
          <p className="text-sm text-gray-500">Gérez les accès et les rôles des membres de votre entreprise.</p>
        </div>
        <button
          onClick={handleOpenCreateModal}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-md shadow flex items-center space-x-2"
        >
          <span>+ Ajouter un utilisateur</span>
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 text-red-700 rounded-md border border-red-200">
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-12 text-center text-gray-500">Chargement des utilisateurs...</div>
      ) : (
        <UtilisateurTable
          utilisateurs={utilisateurs}
          onEdit={handleOpenEditModal}
          onToggleStatut={handleToggleStatut}
          onDelete={handleDelete}
        />
      )}

      <UtilisateurForm
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmitForm}
        utilisateurAEditer={utilisateurAEditer}
      />
    </div>
  );
};