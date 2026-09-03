import React, { useState } from 'react';
import { Building2, Lock, Mail, Phone, User, ShieldCheck } from 'lucide-react';
import { AuthServiceAPI, LoginResponse } from '../services/authService';

interface Props {
  onSuccess: (authData: LoginResponse) => void;
}

export const PremierAdmin: React.FC<Props> = ({ onSuccess }) => {
  const [formData, setFormData] = useState({
    nomEntreprise: '',
    nom: '',
    prenom: '',
    telephone: '',
    email: '',
    motDePasse: '',
    confirmMotDePasse: ''
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (formData.motDePasse !== formData.confirmMotDePasse) {
      setError('Les mots de passe ne correspondent pas.');
      return;
    }

    setLoading(true);

    try {
      const response = await AuthServiceAPI.creerPremierAdmin({
        nomEntreprise: formData.nomEntreprise,
        nom: formData.nom,
        prenom: formData.prenom,
        telephone: formData.telephone,
        email: formData.email,
        motDePasse: formData.motDePasse
      });
      onSuccess(response);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erreur lors de l'initialisation du compte.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <ShieldCheck size={40} className="auth-icon" />
          <h2>Initialisation de l'application</h2>
          <p>Configurez l'entreprise et le compte administrateur principal.</p>
        </div>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label>Nom de l'entreprise *</label>
            <div className="input-icon-wrapper">
              <Building2 size={18} />
              <input
                type="text"
                required
                placeholder="Ex: Facturation Pro SARL"
                value={formData.nomEntreprise}
                onChange={e => setFormData({ ...formData, nomEntreprise: e.target.value })}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Nom *</label>
              <div className="input-icon-wrapper">
                <User size={18} />
                <input
                  type="text"
                  required
                  placeholder="Nom"
                  value={formData.nom}
                  onChange={e => setFormData({ ...formData, nom: e.target.value })}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Prénom</label>
              <input
                type="text"
                placeholder="Prénom"
                value={formData.prenom}
                onChange={e => setFormData({ ...formData, prenom: e.target.value })}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Email *</label>
              <div className="input-icon-wrapper">
                <Mail size={18} />
                <input
                  type="email"
                  required
                  placeholder="admin@entreprise.com"
                  value={formData.email}
                  onChange={e => setFormData({ ...formData, email: e.target.value })}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Téléphone</label>
              <div className="input-icon-wrapper">
                <Phone size={18} />
                <input
                  type="tel"
                  placeholder="+221..."
                  value={formData.telephone}
                  onChange={e => setFormData({ ...formData, telephone: e.target.value })}
                />
              </div>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Mot de passe *</label>
              <div className="input-icon-wrapper">
                <Lock size={18} />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={formData.motDePasse}
                  onChange={e => setFormData({ ...formData, motDePasse: e.target.value })}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Confirmer mot de passe *</label>
              <div className="input-icon-wrapper">
                <Lock size={18} />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={formData.confirmMotDePasse}
                  onChange={e => setFormData({ ...formData, confirmMotDePasse: e.target.value })}
                />
              </div>
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Création en cours…' : 'Créer le premier administrateur'}
          </button>
        </form>
      </div>
    </div>
  );
};