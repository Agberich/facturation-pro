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
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: '#f4f6f8', padding: '20px' }}>
      <div style={{ background: '#ffffff', borderRadius: '12px', boxShadow: '0 10px 25px rgba(0,0,0,0.08)', width: '100%', maxWidth: '420px', padding: '32px', boxSizing: 'border-box' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{ background: '#eff6ff', width: '56px', height: '56px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
            <Lock size={28} color="#2563eb" />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#1e293b', margin: '0 0 6px 0' }}>Changement de mot de passe</h2>
          <p style={{ color: '#64748b', fontSize: '0.875rem', margin: 0 }}>
            Vous devez modifier votre mot de passe temporaire pour continuer.
          </p>
        </div>

        {erreur && (
          <div style={{ backgroundColor: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', padding: '10px 14px', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={18} />
            <span>{erreur}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
              Mot de passe actuel
            </label>
            <input
              type="password"
              value={ancienMotDePasse}
              onChange={(e) => setAncienMotDePasse(e.target.value)}
              required
              style={{ width: '100%', padding: '10px 12px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
              Nouveau mot de passe
            </label>
            <input
              type="password"
              value={nouveauMotDePasse}
              onChange={(e) => setNouveauMotDePasse(e.target.value)}
              required
              style={{ width: '100%', padding: '10px 12px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
              Confirmer le nouveau mot de passe
            </label>
            <input
              type="password"
              value={confirmation}
              onChange={(e) => setConfirmation(e.target.value)}
              required
              style={{ width: '100%', padding: '10px 12px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
            />
          </div>

          <button
            type="submit"
            disabled={chargement}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', padding: '12px', backgroundColor: '#2563eb', color: '#ffffff', border: 'none', borderRadius: '8px', fontSize: '0.95rem', fontWeight: 600, cursor: chargement ? 'not-allowed' : 'pointer', opacity: chargement ? 0.7 : 1, marginTop: '8px' }}
          >
            {chargement ? (
              <>
                <RefreshCw size={16} style={{ marginRight: '8px' }} />
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

export default ChangerMotDePasse;