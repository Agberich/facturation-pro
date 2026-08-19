import React, { useEffect, useMemo, useState } from 'react';
import { Plus, Search, UserRound, Save, Pencil, Power } from 'lucide-react';
import { ClientServiceAPI } from '../services/api';
import { Client } from '../types/facturation';

interface Props {
  idEntreprise: string;
}

const empty: Client = {
  nom: '',
  prenom: '',
  dateNaissance: '',
  dateEntree: new Date().toISOString().slice(0, 10),
  dateSortie: '',
  tarifParDefaut: 0,
  actif: true,
  commentaire: ''
};

export const GestionClients: React.FC<Props> = ({ idEntreprise }) => {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<Client>(empty);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [showInactive, setShowInactive] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setClients(await ClientServiceAPI.getListeClients(idEntreprise, showInactive));
      setError('');
    } catch {
      setError('Impossible de charger les clients.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [idEntreprise, showInactive]);

  const filtered = useMemo(
    () => clients.filter(c => `${c.nom} ${c.prenom}`.toLowerCase().includes(search.toLowerCase())),
    [clients, search]
  );

  const handleOpenNew = () => {
    setEditingId(null);
    setForm(empty);
    setOpen(v => !v);
  };

  const handleEdit = (client: Client) => {
    setEditingId(client.idClient || null);
    setForm({
      ...client,
      dateNaissance: client.dateNaissance || '',
      dateSortie: client.dateSortie || ''
    });
    setOpen(true);
  };

  const handleToggleStatut = async (client: Client) => {
    if (!client.idClient) return;

    const messageConfirmation = client.actif
      ? 'Voulez-vous vraiment désactiver ce client ?'
      : 'Voulez-vous réactiver ce client ?';

    const confirme = window.confirm(messageConfirmation);
    if (!confirme) return;

    try {
      if (client.actif) {
        await ClientServiceAPI.desactiverClient(client.idClient);
      } else {
        await ClientServiceAPI.reactiverClient(client.idClient);
      }
      await load();
    } catch {
      setError('Impossible de modifier le statut du client.');
    }
  };

  const save = async (e: React.FormEvent) => {
    e.preventDefault();

    // 1. Validation des champs obligatoires
    if (!form.nom.trim() || !form.prenom.trim()) {
      setError('Le nom et le prénom sont obligatoires.');
      return;
    }

    // 2. Validation de la chronologie des dates
    if (form.dateEntree && form.dateSortie && form.dateSortie < form.dateEntree) {
      setError('La date de sortie doit être postérieure ou égale à la date d’entrée.');
      return;
    }

    setSaving(true);

    // 3. Transformation des chaînes vides en undefined pour le backend Java
    const payload: Client = {
      ...form,
      dateNaissance: form.dateNaissance?.trim() || undefined,
      dateSortie: form.dateSortie?.trim() || undefined,
    };

    try {
      if (editingId) {
        await ClientServiceAPI.modifierClient(idEntreprise, editingId, payload);
      } else {
        await ClientServiceAPI.creerClient(idEntreprise, payload);
      }
      setForm({ ...empty });
      setEditingId(null);
      setOpen(false);
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message || 'Impossible d’enregistrer le client.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-head">
        <div>
          <div className="eyebrow">Répertoire</div>
          <h1 className="page-title">Clients</h1>
          <p className="page-desc">Gérez les personnes prises en compte dans vos facturations mensuelles.</p>
        </div>
        <button className="btn btn-primary" onClick={handleOpenNew}>
          <Plus size={15} />
          {open && !editingId ? 'Fermer' : 'Nouveau client'}
        </button>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      {open && (
        <form className="card form-card" onSubmit={save}>
          <div className="section-title">{editingId ? 'Modifier le client' : 'Nouveau client'}</div>
          <div className="section-sub" style={{ marginBottom: 16 }}>
            Les champs marqués d’un * sont obligatoires.
          </div>
          <div className="form-grid">
            <div className="field">
              <label>Nom *</label>
              <input value={form.nom} onChange={e => setForm({ ...form, nom: e.target.value })} required />
            </div>
            <div className="field">
              <label>Prénom *</label>
              <input value={form.prenom} onChange={e => setForm({ ...form, prenom: e.target.value })} required />
            </div>
            <div className="field">
              <label>Date de naissance</label>
              <input type="date" value={form.dateNaissance || ''} onChange={e => setForm({ ...form, dateNaissance: e.target.value })} />
            </div>
            <div className="field">
              <label>Date d’entrée *</label>
              <input type="date" value={form.dateEntree || ''} onChange={e => setForm({ ...form, dateEntree: e.target.value })} required />
            </div>
            <div className="field">
              <label>Date de sortie</label>
              <input type="date" value={form.dateSortie || ''} onChange={e => setForm({ ...form, dateSortie: e.target.value })} />
            </div>
            <div className="field">
              <label>Tarif par défaut</label>
              <input type="number" min="0" step="0.01" value={form.tarifParDefaut} onChange={e => setForm({ ...form, tarifParDefaut: +e.target.value })} />
            </div>
            <div className="field">
              <label>Statut</label>
              <select value={form.actif ? 'actif' : 'inactif'} onChange={e => setForm({ ...form, actif: e.target.value === 'actif' })}>
                <option value="actif">Actif</option>
                <option value="inactif">Inactif</option>
              </select>
            </div>
            <div className="field form-full">
              <label>Commentaire</label>
              <textarea value={form.commentaire || ''} onChange={e => setForm({ ...form, commentaire: e.target.value })} />
            </div>
            <div className="form-full" style={{ display: 'flex', gap: '8px' }}>
              <button className="btn btn-success" disabled={saving}>
                <Save size={14} />
                {saving ? 'Enregistrement…' : editingId ? 'Mettre à jour' : 'Enregistrer le client'}
              </button>
              <button type="button" className="btn btn-secondary" onClick={() => { setOpen(false); setEditingId(null); }}>
                Annuler
              </button>
            </div>
          </div>
        </form>
      )}

      <section className="card table-card">
        <div className="table-tools">
          <div>
            <div className="section-title">Répertoire clients</div>
            <div className="section-sub">
              {clients.length} client{clients.length !== 1 ? 's' : ''}
            </div>
          </div>
          <div className="search" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <Search size={15} />
              <input placeholder="Rechercher un client…" value={search} onChange={e => setSearch(e.target.value)} />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', cursor: 'pointer', whiteSpace: 'nowrap' }}>
              <input
                type="checkbox"
                checked={showInactive}
                onChange={e => setShowInactive(e.target.checked)}
              />
              Afficher les archivés
            </label>
          </div>
        </div>

        {loading ? (
          <div className="loading">Chargement…</div>
        ) : filtered.length === 0 ? (
          <div className="empty">
            <div className="empty-icon">
              <UserRound size={19} />
            </div>
            Aucun client trouvé.
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>CLIENT</th>
                  <th>DATE D’ENTRÉE</th>
                  <th>DATE DE SORTIE</th>
                  <th className="num">TARIF / JOUR</th>
                  <th>STATUT</th>
                  <th className="num">ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(c => (
                  <tr key={c.idClient} style={{ opacity: c.actif ? 1 : 0.65 }}>
                    <td>
                      <strong>{c.nom} {c.prenom}</strong>
                    </td>
                    <td>{c.dateEntree || '—'}</td>
                    <td>{c.dateSortie || '—'}</td>
                    <td className="num">{c.tarifParDefaut.toLocaleString('fr-FR')} XOF</td>
                    <td>
                      <span className={`status ${c.actif ? 'status-active' : 'status-arch'}`}>
                        {c.actif ? 'Actif' : 'Archivé'}
                      </span>
                    </td>
                    <td className="num">
                      <button
                        className="btn btn-icon"
                        title="Modifier"
                        onClick={() => handleEdit(c)}
                        style={{ marginRight: '4px' }}
                      >
                        <Pencil size={15} />
                      </button>
                      <button
                        className="btn btn-icon"
                        title={c.actif ? 'Désactiver (Archiver)' : 'Réactiver'}
                        onClick={() => handleToggleStatut(c)}
                      >
                        <Power size={15} color={c.actif ? '#e53e3e' : '#38a169'} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
};