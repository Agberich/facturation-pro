import React from 'react';
import { UtilisateurResponse } from '../../types/utilisateur';

interface UtilisateurTableProps {
  utilisateurs: UtilisateurResponse[];
  onEdit: (utilisateur: UtilisateurResponse) => void;
  onToggleStatut: (utilisateur: UtilisateurResponse) => void;
  onDelete: (idUtilisateur: string) => void;
}

export const UtilisateurTable: React.FC<UtilisateurTableProps> = ({
  utilisateurs,
  onEdit,
  onToggleStatut,
  onDelete,
}) => {
  const getBadgeRoleClass = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'badge-admin';
      case 'COMPTABLE':
        return 'badge-comptable';
      default:
        return 'badge-comptable';
    }
  };

  return (
    <div className="utilisateurs-card">
      <table className="utilisateurs-table">
        <thead>
          <tr>
            <th>Nom & Email</th>
            <th>Rôle</th>
            <th>Statut</th>
            <th>Dernière connexion</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          {utilisateurs.length === 0 ? (
            <tr>
              <td
                colSpan={5}
                style={{
                  textAlign: 'center',
                  padding: '20px',
                  color: '#64748b',
                }}
              >
                Aucun utilisateur trouvé.
              </td>
            </tr>
          ) : (
            utilisateurs.map((u) => (
              <tr key={u.idUtilisateur}>
                <td>
                  <div style={{ fontWeight: 600 }}>
                    {u.nom}
                  </div>

                  <div
                    style={{
                      fontSize: '14px',
                      color: '#64748b',
                    }}
                  >
                    {u.email}
                  </div>
                </td>

                <td>
                  <span className={getBadgeRoleClass(u.role)}>
                    {u.role}
                  </span>
                </td>

                <td>
                  {u.actif ? (
                    <span className="badge-actif">
                      Actif
                    </span>
                  ) : (
                    <span className="badge-inactif">
                      Inactif
                    </span>
                  )}
                </td>

                <td>
                  {u.derniereConnexion
                    ? new Date(
                        u.derniereConnexion
                      ).toLocaleDateString('fr-FR', {
                        day: '2-digit',
                        month: '2-digit',
                        year: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })
                    : 'Jamais'}
                </td>

                <td>
                  <button
                    onClick={() => onEdit(u)}
                    className="btn-edit"
                  >
                    Éditer
                  </button>

                  <button
                    onClick={() => onToggleStatut(u)}
                    className="btn-disable"
                  >
                    {u.actif
                      ? 'Désactiver'
                      : 'Réactiver'}
                  </button>

                  <button
                    onClick={() =>
                      onDelete(u.idUtilisateur)
                    }
                    className="btn-delete"
                  >
                    Supprimer
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};