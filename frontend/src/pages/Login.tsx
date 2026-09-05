import React, { useState } from 'react';
import { Lock, Mail, LogIn } from 'lucide-react';
import { AuthServiceAPI, LoginResponse } from '../services/authService';

interface Props {
  onSuccess: (authData: LoginResponse) => void;
}

export const Login: React.FC<Props> = ({ onSuccess }) => {
  const [email, setEmail] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const response = await AuthServiceAPI.login({ email, motDePasse });
      onSuccess(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Email ou mot de passe incorrect.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: '#f4f6f8', padding: '20px', boxSizing: 'border-box' }}>
      <div style={{ background: '#ffffff', borderRadius: '12px', boxShadow: '0 10px 25px rgba(0,0,0,0.08)', width: '100%', maxWidth: '420px', padding: '32px', boxSizing: 'border-box' }}>
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <LogIn size={40} color="#2563eb" style={{ marginBottom: '8px' }} />
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#1e293b', margin: '0 0 6px 0' }}>Connexion</h2>
          <p style={{ fontSize: '0.875rem', color: '#64748b', margin: 0 }}>Accédez à votre espace de gestion Facturation Pro</p>
        </div>

        {error && (
          <div style={{ backgroundColor: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', padding: '10px 14px', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '20px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Adresse email</label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <Mail size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none', zIndex: 1 }} />
              <input
                type="email"
                required
                placeholder="votre@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
              />
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Mot de passe</label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <Lock size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none', zIndex: 1 }} />
              <input
                type="password"
                required
                placeholder="••••••••"
                value={motDePasse}
                onChange={(e) => setMotDePasse(e.target.value)}
                style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{ width: '100%', padding: '12px', backgroundColor: '#2563eb', color: '#ffffff', border: 'none', borderRadius: '8px', fontSize: '0.95rem', fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer', opacity: loading ? 0.7 : 1, marginTop: '8px' }}
          >
            {loading ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;