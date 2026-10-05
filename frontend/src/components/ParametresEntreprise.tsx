import React, { useEffect, useState } from 'react';
import { Building2, ReceiptText, Save, CheckCircle2, FileText, Image as ImageIcon, Trash2 } from 'lucide-react';
import { ParametreServiceAPI } from '../services/api';
import { Parametre } from '../types/facturation';
import { lireImage } from '../utils/image';

interface Props {
  idEntreprise: string;
}

export const ParametresEntreprise: React.FC<Props> = ({ idEntreprise }) => {
  const [p, setP] = useState<Parametre | null>(null);
  const [tab, setTab] = useState<'profil' | 'facturation' | 'facture' | 'document'>('profil');
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
          devise: 'EUR',
          tarifJournalier: 79.92,
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

  // Champ texte simple lié à un paramètre
  const champ = (label: string, cle: keyof Parametre, options?: { full?: boolean; placeholder?: string }) => (
    <div className={`field ${options?.full ? 'form-full' : ''}`}>
      <label>{label}</label>
      <input
        value={(p[cle] as string) || ''}
        placeholder={options?.placeholder}
        onChange={(e) => setP({ ...p, [cle]: e.target.value })}
      />
    </div>
  );

  const choisirImage = async (
    e: React.ChangeEvent<HTMLInputElement>,
    cle: 'logoUrl' | 'signatureUrl',
    largeurMax: number
  ) => {
    const fichier = e.target.files?.[0];
    e.target.value = '';
    if (!fichier) return;
    try {
      const data = await lireImage(fichier, largeurMax);
      setP((prev) => (prev ? { ...prev, [cle]: data } : prev));
      setError('');
      setSuccess(false);
    } catch (err: any) {
      setError(err.message || "Impossible de charger l'image.");
    }
  };

  const blocImage = (titre: string, aide: string, cle: 'logoUrl' | 'signatureUrl', largeurMax: number) => (
    <div className="field form-full">
      <label>{titre}</label>
      <div style={{ fontSize: 12, color: '#667085', marginBottom: 8 }}>{aide}</div>
      <div
        style={{
          border: '1px dashed #cbd5e1',
          borderRadius: 8,
          padding: 12,
          minHeight: 90,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#fff'
        }}
      >
        {p[cle] ? (
          <img src={p[cle]} alt={titre} style={{ maxHeight: 90, maxWidth: '100%' }} />
        ) : (
          <span style={{ color: '#98a2b3', fontSize: 13 }}>Aucune image</span>
        )}
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <label className="btn" style={{ cursor: 'pointer', margin: 0 }}>
          <ImageIcon size={14} /> {p[cle] ? 'Remplacer' : 'Choisir une image'}
          <input
            type="file"
            accept="image/png,image/jpeg"
            style={{ display: 'none' }}
            onChange={(e) => choisirImage(e, cle, largeurMax)}
          />
        </label>
        {p[cle] && (
          <button type="button" className="btn" onClick={() => setP({ ...p, [cle]: '' })}>
            <Trash2 size={14} /> Supprimer
          </button>
        )}
      </div>
    </div>
  );

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
            className={`tab ${tab === 'facture' ? 'active' : ''}`}
            onClick={() => setTab('facture')}
          >
            <FileText size={14} style={{ verticalAlign: 'middle', marginRight: 7 }} />
            Informations de facture
          </button>
          <button
            className={`tab ${tab === 'document' ? 'active' : ''}`}
            onClick={() => setTab('document')}
          >
            <ImageIcon size={14} style={{ verticalAlign: 'middle', marginRight: 7 }} />
            Logo & signature
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
                  <label>Tarif journalier TTC (TVA incluse, par jour et par personne accueillie)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={p.tarifJournalier ?? 79.92}
                    onChange={(e) => setP({ ...p, tarifJournalier: +e.target.value })}
                  />
                </div>
                <div className="field">
                  <label>Préfixe facture</label>
                  <input
                    value={p.prefixeFacture}
                    onChange={(e) => setP({ ...p, prefixeFacture: e.target.value })}
                  />
                </div>
                <div className="preview-invoice">
                  <div className="section-sub">Aperçu du numéro (préfixe-année-mois)</div>
                  <div className="invoice-number">
                    {p.prefixeFacture || 'FAC'}-{new Date().getFullYear()}-{String(new Date().getMonth() + 1).padStart(2, '0')}
                  </div>
                </div>
              </div>
            </>
          )}

          {tab === 'facture' && (
            <>
              <div className="section-title">Informations imprimées sur la facture</div>
              <div className="section-sub" style={{ marginBottom: 18 }}>
                Toutes ces informations sont facultatives : un bloc laissé vide n'est pas imprimé sur le PDF.
              </div>

              <div className="section-sub" style={{ margin: '4px 0 10px', fontWeight: 600 }}>Prestation</div>
              <div className="form-grid">
                {champ('Dispositif', 'dispositif', { placeholder: 'Ex : MNA' })}
                {champ('Type de prestation', 'typePrestation')}
                {champ("Catégorie d'établissement", 'categorieEtablissement')}
                {champ('Discipline', 'discipline')}
                {champ('Mode de fonctionnement', 'modeFonctionnement')}
                {champ('Public accueilli', 'publicAccueilli')}
                <div className="field">
                  <label>Capacité (places)</label>
                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={p.capacite ?? ''}
                    onChange={(e) => setP({ ...p, capacite: e.target.value === '' ? undefined : +e.target.value })}
                  />
                </div>
                {champ('Centre de profit', 'centreProfit')}
                <div className="field">
                  <label>Premier mois de la prestation (début du trimestre 1)</label>
                  <select
                    value={p.premierMoisPrestation ?? 4}
                    onChange={(e) => setP({ ...p, premierMoisPrestation: +e.target.value })}
                  >
                    {['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'].map(
                      (nom, i) => (
                        <option key={nom} value={i + 1}>{nom}</option>
                      )
                    )}
                  </select>
                </div>
              </div>

              <div className="section-sub" style={{ margin: '20px 0 10px', fontWeight: 600 }}>
                Financeur (destinataire de la facture)
              </div>
              <div className="form-grid">
                {champ('Nom du financeur', 'financeurNom', { full: true })}
                {champ('Service / direction', 'financeurService', { full: true })}
                {champ('Adresse', 'financeurAdresse', { full: true })}
                {champ('E-mail', 'financeurEmail')}
                {champ('SIRET', 'financeurSiret')}
                {champ("N° d'engagement", 'numeroEngagement')}
              </div>

              <div className="section-sub" style={{ margin: '20px 0 10px', fontWeight: 600 }}>
                Fournisseur (vous)
              </div>
              <div className="section-sub" style={{ marginBottom: 10 }}>
                Le nom, l'adresse, le téléphone et l'e-mail viennent de l'onglet « Profil & coordonnées ».
              </div>
              <div className="form-grid">
                {champ('SIRET', 'fournisseurSiret')}
                {champ('Direction territoriale', 'directionTerritoriale')}
              </div>

              <div className="section-sub" style={{ margin: '20px 0 10px', fontWeight: 600 }}>
                Références bancaires
              </div>
              <div className="form-grid">
                {champ('Mention de règlement', 'mentionReglement', { full: true })}
                {champ('IBAN', 'iban', { full: true })}
              </div>

              <div className="section-sub" style={{ margin: '20px 0 10px', fontWeight: 600 }}>Contacts</div>
              <div className="form-grid">
                {champ('Contact UT', 'contactUt')}
                {champ('Contact du dispositif', 'contactDispositif')}
                {champ('Interlocuteur', 'interlocuteur')}
                {champ("Fonction de l'interlocuteur", 'fonctionInterlocuteur')}
              </div>
            </>
          )}

          {tab === 'document' && (
            <>
              <div className="section-title">Logo & signature</div>
              <div className="section-sub" style={{ marginBottom: 18 }}>
                Images imprimées sur le PDF des factures : le logo en haut à gauche, la signature en bas.
                Choisissez une image PNG ou JPEG, elle est réduite automatiquement. N'oubliez pas de cliquer sur
                « Enregistrer les paramètres » ensuite.
              </div>
              <div className="form-grid">
                {blocImage('Logo', 'Idéalement sur fond transparent ou blanc.', 'logoUrl', 500)}
                {blocImage('Signature', 'Signature ou cachet scanné, de préférence sur fond transparent.', 'signatureUrl', 400)}
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