import React, { useState } from 'react';
import { Lock, CheckCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { AuthServiceAPI } from '../services/authService';

interface Props {
  onSuccess: () => void;
}

export const ChangerMotDePasse: React.FC<Props> = ({ onSuccess }) => {
  const [ancienMotDePasse, setAncienMotDePasse] = useState('');
  const [nouveauMotDePasse, setNouveauMotDePasse] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErreur(null);

    if (nouveauMotDePasse !== confirmation) {
      setErreur('Les deux mots de passe ne correspondent pas.');
      return;
    }

    if (nouveauMotDePasse.length < 6) {
      setErreur('Le nouveau mot de passe doit contenir au moins 6 caractères.');
      return;
    }

    try {
      setChargement(true);
      await AuthServiceAPI.changerMotDePasse({ ancienMotDePasse, nouveauMotDePasse });
      onSuccess();
    } catch (err: any) {
      setErreur(
        err.response?.data?.message ||
          'Erreur lors du changement de mot de passe. Vérifiez votre mot de passe actuel.'
      );
    } finally {
      setChargement(false);
    }
  };

  return (
    <div className="auth-container" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
      <div className="auth-card" style={{ width: '100%', maxWidth: '420px', padding: '32px' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{ background: '#f3f4f6', width: '56px', height: '56px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
            <Lock size={28} color="#2563eb" />
          </div>
          <h2>Changement de mot de passe</h2>
          <p style={{ color: '#666', fontSize: '0.9rem', marginTop: '8px' }}>
            Vous devez modifier votre mot de passe temporaire pour continuer.
          </p>
        </div>

        {erreur && (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', padding: '12px', borderRadius: '8px', fontSize: '0.875rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={18} />
            <span>{erreur}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, marginBottom: '6px' }}>
              Mot de passe actuel
            </label>
            <input
              type="password"
              className="input"
              value={ancienMotDePasse}
              onChange={(e) => setAncienMotDePasse(e.target.value)}
              required
            />
          </div>

          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, marginBottom: '6px' }}>
              Nouveau mot de passe
            </label>
            <input
              type="password"
              className="input"
              value={nouveauMotDePasse}
              onChange={(e) => setNouveauMotDePasse(e.target.value)}
              required
            />
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, marginBottom: '6px' }}>
              Confirmer le nouveau mot de passe
            </label>
            <input
              type="password"
              className="input"
              value={confirmation}
              onChange={(e) => setConfirmation(e.target.value)}
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', justifyContent: 'center' }}
            disabled={chargement}
          >
            {chargement ? (
              <>
                <RefreshCw size={16} className="spin-icon" style={{ marginRight: '8px' }} />
                Mise à jour...
              </>
            ) : (
              <>
                <CheckCircle size={16} style={{ marginRight: '8px' }} />
                Valider et continuer
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};