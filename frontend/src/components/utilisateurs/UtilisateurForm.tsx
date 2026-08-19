import React, { useState, useEffect } from 'react';
import {
  UtilisateurRequest,
  UtilisateurResponse,
  RoleUtilisateur,
} from '../../types/utilisateur';

interface UtilisateurFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: UtilisateurRequest) => Promise<void>;
  utilisateurAEditer?: UtilisateurResponse | null;
}

export const UtilisateurForm: React.FC<UtilisateurFormProps> = ({
  isOpen,
  onClose,
  onSubmit,
  utilisateurAEditer,
}) => {
  const [nom, setNom] = useState('');
  const [email, setEmail] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [role, setRole] = useState<RoleUtilisateur>('COMPTABLE');
  const [actif, setActif] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (utilisateurAEditer) {
      setNom(utilisateurAEditer.nom);
      setEmail(utilisateurAEditer.email);
      setRole(utilisateurAEditer.role);
      setActif(utilisateurAEditer.actif);
      setMotDePasse('');
    } else {
      setNom('');
      setEmail('');
      setMotDePasse('');
      setRole('COMPTABLE');
      setActif(true);
    }

    setError(null);
  }, [utilisateurAEditer, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setIsSubmitting(true);
    setError(null);

    try {
      const payload: UtilisateurRequest = {
        nom,
        email,
        role,
        actif,
        ...(motDePasse ? { motDePasse } : {}),
      };

      await onSubmit(payload);
      onClose();
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
          "Une erreur est survenue lors de l'enregistrement."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="utilisateur-modal">
        <div className="utilisateur-modal-header">
          <h3>
            {utilisateurAEditer
              ? "Modifier l'utilisateur"
              : 'Créer un utilisateur'}
          </h3>

          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
          >
            ×
          </button>
        </div>

        {error && (
          <div className="utilisateur-error">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Nom complet *</label>

            <input
              type="text"
              required
              value={nom}
              onChange={(e) => setNom(e.target.value)}
              placeholder="Jean Dupont"
            />
          </div>

          <div className="form-group">
            <label>Adresse Email *</label>

            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="jean@entreprise.com"
            />
          </div>

          <div className="form-group">
            <label>
              Mot de passe{' '}
              {utilisateurAEditer
                ? '(laisser vide pour conserver)'
                : '*'}
            </label>

            <input
              type="password"
              required={!utilisateurAEditer}
              value={motDePasse}
              onChange={(e) => setMotDePasse(e.target.value)}
              placeholder="********"
            />
          </div>

          <div className="form-group">
            <label>Rôle *</label>

            <select
              value={role}
              onChange={(e) =>
                setRole(e.target.value as RoleUtilisateur)
              }
            >
              <option value="ADMIN">Administrateur</option>
              <option value="COMPTABLE">Comptable</option>
              <option value="CONSULTATION">Consultation</option>
            </select>
          </div>

          <div className="checkbox-group">
            <input
              type="checkbox"
              id="actif"
              checked={actif}
              onChange={(e) => setActif(e.target.checked)}
            />

            <label htmlFor="actif">
              Compte actif
            </label>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn-cancel"
              onClick={onClose}
            >
              Annuler
            </button>

            <button
              type="submit"
              className="btn-save"
              disabled={isSubmitting}
            >
              {isSubmitting
                ? 'Enregistrement...'
                : 'Enregistrer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};