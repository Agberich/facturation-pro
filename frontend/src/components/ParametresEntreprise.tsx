import React, { useEffect, useState } from 'react';
import { Building2, ReceiptText, Save, CheckCircle2 } from 'lucide-react';
import { ParametreServiceAPI } from '../services/api';
import { Parametre } from '../types/facturation';

interface Props {
  idEntreprise: string;
}

export const ParametresEntreprise: React.FC<Props> = ({ idEntreprise }) => {
  const [p, setP] = useState<Parametre | null>(null);
  const [tab, setTab] = useState<'profil' | 'facturation' | 'document'>('profil');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    ParametreServiceAPI.obtenirParametres(idEntreprise)
      .then(setP)
      .catch(() =>
        setP({
          nomEntreprise: '',
          tauxTva: 18,
          devise: 'XOF',
          prefixeFacture: 'FAC',
          adresse: '',
          telephone: '',
          email: ''
        })
      )
      .finally(() => setLoading(false));
  }, [idEntreprise]);

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!p) return;
    setSaving(true);
    setSuccess(false);
    setError('');
    try {
      setP(await ParametreServiceAPI.mettreAJourParametres(idEntreprise, p));
      setSuccess(true);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Impossible d’enregistrer les paramètres.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading">Chargement des paramètres…</div>;
  if (!p) return null;

  return (
    <>
      <div className="page-head">
        <div>
          <div className="eyebrow">Configuration</div>
          <h1 className="page-title">Paramètres de l’entreprise</h1>
          <p className="page-desc">Centralisez les informations utilisées par vos facturations et documents.</p>
        </div>
      </div>

      {error && <div className="notice notice-error">{error}</div>}
      {success && (
        <div className="notice notice-success">
          <CheckCircle2 size={15} style={{ verticalAlign: 'middle', marginRight: 6 }} />
          Paramètres enregistrés.
        </div>
      )}

      <div className="settings-layout">
        <aside className="card tabs">
          <button
            className={`tab ${tab === 'profil' ? 'active' : ''}`}
            onClick={() => setTab('profil')}
          >
            <Building2 size={14} style={{ verticalAlign: 'middle', marginRight: 7 }} />
            Profil & coordonnées
          </button>
          <button
            className={`tab ${tab === 'facturation' ? 'active' : ''}`}
            onClick={() => setTab('facturation')}
          >
            <ReceiptText size={14} style={{ verticalAlign: 'middle', marginRight: 7 }} />
            Facturation
          </button>
          <button
            className={`tab ${tab === 'document' ? 'active' : ''}`}
            onClick={() => setTab('document')}
          >
            Documents
          </button>
        </aside>

        <form className="card form-card" style={{ margin: 0 }} onSubmit={save}>
          {tab === 'profil' && (
            <>
              <div className="section-title">Profil & coordonnées</div>
              <div className="section-sub" style={{ marginBottom: 18 }}>
                Informations affichées sur vos documents.
              </div>
              <div className="form-grid">
                <div className="field form-full">
                  <label>Nom de l'entreprise</label>
                  <input
                    value={p.nomEntreprise || ''}
                    onChange={(e) => setP({ ...p, nomEntreprise: e.target.value })}
                  />
                </div>
                <div className="field form-full">
                  <label>Adresse</label>
                  <input
                    value={p.adresse || ''}
                    onChange={(e) => setP({ ...p, adresse: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label>Téléphone</label>
                  <input
                    value={p.telephone || ''}
                    onChange={(e) => setP({ ...p, telephone: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label>Email</label>
                  <input
                    type="email"
                    value={p.email || ''}
                    onChange={(e) => setP({ ...p, email: e.target.value })}
                  />
                </div>
              </div>
            </>
          )}

          {tab === 'facturation' && (
            <>
              <div className="section-title">Facturation & numérotation</div>
              <div className="section-sub" style={{ marginBottom: 18 }}>
                Définissez les règles utilisées par les nouvelles facturations.
              </div>
              <div className="form-grid">
                <div className="field">
                  <label>Taux de TVA (%)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={p.tauxTva}
                    onChange={(e) => setP({ ...p, tauxTva: +e.target.value })}
                  />
                </div>
                <div className="field">
                  <label>Devise</label>
                  <select
                    value={p.devise}
                    onChange={(e) =>
                      setP({ ...p, devise: e.target.value as Parametre['devise'] })
                    }
                  >
                    <option value="XOF">Franc CFA (XOF)</option>
                    <option value="EUR">Euro (EUR)</option>
                    <option value="USD">Dollar (USD)</option>
                  </select>
                </div>
                <div className="field">
                  <label>Préfixe facture</label>
                  <input
                    value={p.prefixeFacture}
                    onChange={(e) => setP({ ...p, prefixeFacture: e.target.value })}
                  />
                </div>
                <div className="preview-invoice">
                  <div className="section-sub">Aperçu du numéro</div>
                  <div className="invoice-number">
                    {p.prefixeFacture || 'FAC'}-{new Date().getFullYear()}-00042
                  </div>
                </div>
              </div>
            </>
          )}

          {tab === 'document' && (
            <>
              <div className="section-title">Mentions & documents</div>
              <div className="section-sub" style={{ marginBottom: 18 }}>
                Cette section prépare les informations documentaires à enrichir dans une prochaine version.
              </div>
              <div className="alert" style={{ marginTop: 0 }}>
                <ReceiptText size={17} />
                <div>
                  <strong>Configuration actuelle</strong>
                  <div style={{ fontSize: 11, marginTop: 3 }}>
                    Les paramètres disponibles dans le backend sont la TVA, la devise, le préfixe et les coordonnées.
                  </div>
                </div>
              </div>
            </>
          )}

          <div style={{ marginTop: 22 }}>
            <button className="btn btn-primary" disabled={saving}>
              <Save size={14} />
              {saving ? 'Enregistrement…' : 'Enregistrer les paramètres'}
            </button>
          </div>
        </form>
      </div>
    </>
  );
};