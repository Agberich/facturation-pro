import React, { useState } from 'react';
import { AuthServiceAPI } from '../services/api';
import { ConnexionReponse } from '../types/facturation';

interface Props {
  onConnexionReussie: (reponse: ConnexionReponse) => void;
}

export const Login: React.FC<Props> = ({ onConnexionReussie }) => {
  const [email, setEmail] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErreur('');
    setChargement(true);
    try {
      const reponse = await AuthServiceAPI.connexion(email.trim(), motDePasse);
      onConnexionReussie(reponse);
    } catch (err: any) {
      setErreur(err.response?.data?.message || 'Connexion impossible. Vérifiez vos identifiants.');
    } finally {
      setChargement(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#f8fafc',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    }}>
      <form
        onSubmit={handleSubmit}
        style={{
          backgroundColor: '#fff',
          border: '1px solid #e2e8f0',
          borderRadius: '8px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
          padding: '32px',
          width: '340px',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{
            width: '40px', height: '40px', borderRadius: '8px',
            backgroundColor: '#1e293b', color: '#fff', fontWeight: 700,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 12px', fontSize: '18px',
          }}>
            F
          </div>
          <div style={{ fontWeight: 700, fontSize: '16px', color: '#0f172a' }}>
            Facturation Pro
          </div>
          <div style={{ fontSize: '13px', color: '#64748b' }}>
            Connectez-vous pour continuer
          </div>
        </div>

        {erreur && (
          <div style={{
            padding: '10px 14px', marginBottom: '16px',
            backgroundColor: '#fef2f2', color: '#dc2626',
            borderRadius: '6px', fontSize: '13px',
          }}>
            {erreur}
          </div>
        )}

        <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#64748b', marginBottom: '4px' }}>
          Email
        </label>
        <input
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          style={{
            width: '100%', padding: '8px 10px', marginBottom: '14px',
            border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box',
          }}
        />

        <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#64748b', marginBottom: '4px' }}>
          Mot de passe
        </label>
        <input
          type="password"
          required
          value={motDePasse}
          onChange={(e) => setMotDePasse(e.target.value)}
          style={{
            width: '100%', padding: '8px 10px', marginBottom: '20px',
            border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box',
          }}
        />

        <button
          type="submit"
          disabled={chargement}
          style={{
            width: '100%', padding: '10px', backgroundColor: '#2563eb', color: '#fff',
            border: 'none', borderRadius: '6px', fontSize: '14px', fontWeight: 600,
            cursor: chargement ? 'default' : 'pointer', opacity: chargement ? 0.7 : 1,
          }}
        >
          {chargement ? 'Connexion...' : 'Se connecter'}
        </button>
      </form>
    </div>
  );
};