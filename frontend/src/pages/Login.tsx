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
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <LogIn size={40} className="auth-icon" />
          <h2>Connexion</h2>
          <p>Accédez à votre espace de gestion Facturation Pro</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label>Adresse email</label>
            <div className="input-icon-wrapper">
              <Mail size={18} />
              <input
                type="email"
                required
                placeholder="votre@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
          </div>

          <div className="form-group">
            <label>Mot de passe</label>
            <div className="input-icon-wrapper">
              <Lock size={18} />
              <input
                type="password"
                required
                placeholder="••••••••"
                value={motDePasse}
                onChange={(e) => setMotDePasse(e.target.value)}
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Connexion…' : 'Se connecter'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;