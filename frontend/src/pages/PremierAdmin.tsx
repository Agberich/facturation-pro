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
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', backgroundColor: '#f4f6f8', padding: '20px' }}>
      <div style={{ background: '#ffffff', borderRadius: '12px', boxShadow: '0 10px 25px rgba(0,0,0,0.08)', width: '100%', maxWidth: '520px', padding: '32px', boxSizing: 'border-box' }}>
        
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <ShieldCheck size={48} color="#2563eb" style={{ marginBottom: '8px' }} />
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#1e293b', margin: '0 0 6px 0' }}>Initialisation de Facturation Pro</h1>
          <p style={{ fontSize: '0.875rem', color: '#64748b', margin: 0 }}>Configurez votre entreprise et créez le premier administrateur.</p>
        </div>

        {error && (
          <div style={{ backgroundColor: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626', padding: '10px 14px', borderRadius: '6px', fontSize: '0.875rem', marginBottom: '20px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          
          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#1e293b', borderBottom: '1px solid #e2e8f0', paddingBottom: '6px', marginBottom: '12px' }}>Entreprise</h3>
            <div>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Nom de l'entreprise *</label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <Building2 size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none' }} />
                <input
                  type="text"
                  required
                  value={formData.nomEntreprise}
                  placeholder="Ex : Facturation Pro SARL"
                  onChange={(e) => setFormData({ ...formData, nomEntreprise: e.target.value })}
                  style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
                />
              </div>
            </div>
          </div>

          <div>
            <h3 style={{ fontSize: '1rem', fontWeight: 600, color: '#1e293b', borderBottom: '1px solid #e2e8f0', paddingBottom: '6px', marginBottom: '12px' }}>Administrateur principal</h3>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Nom *</label>
                <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                  <User size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none' }} />
                  <input
                    type="text"
                    required
                    value={formData.nom}
                    onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Prénom</label>
                <input
                  type="text"
                  value={formData.prenom}
                  onChange={(e) => setFormData({ ...formData, prenom: e.target.value })}
                  style={{ width: '100%', padding: '10px 12px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
                />
              </div>
            </div>

            <div style={{ marginBottom: '12px' }}>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Email *</label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <Mail size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none' }} />
                <input
                  type="email"
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
                />
              </div>
            </div>

            <div style={{ marginBottom: '12px' }}>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Téléphone</label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <Phone size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none' }} />
                <input
                  type="text"
                  value={formData.telephone}
                  onChange={(e) => setFormData({ ...formData, telephone: e.target.value })}
                  style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Mot de passe *</label>
                <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                  <Lock size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none' }} />
                  <input
                    type="password"
                    required
                    value={formData.motDePasse}
                    onChange={(e) => setFormData({ ...formData, motDePasse: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>Confirmation *</label>
                <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                  <Lock size={18} color="#94a3b8" style={{ position: 'absolute', left: '12px', pointerEvents: 'none' }} />
                  <input
                    type="password"
                    required
                    value={formData.confirmMotDePasse}
                    onChange={(e) => setFormData({ ...formData, confirmMotDePasse: e.target.value })}
                    style={{ width: '100%', padding: '10px 12px 10px 40px', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '0.95rem', outline: 'none', boxSizing: 'border-box' }}
                  />
                </div>
              </div>
            </div>

          </div>

          <button
            type="submit"
            disabled={loading}
            style={{ width: '100%', padding: '12px', backgroundColor: '#2563eb', color: '#ffffff', border: 'none', borderRadius: '8px', fontSize: '1rem', fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer', opacity: loading ? 0.7 : 1, marginTop: '8px' }}
          >
            {loading ? 'Création en cours...' : 'Créer l’entreprise'}
          </button>

        </form>

      </div>
    </div>
  );
};