import React, { useEffect, useMemo, useState } from 'react';
import { Plus, Search, UserRound, Save, Pencil, Power, CheckSquare, XSquare } from 'lucide-react';
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

  // État pour la sélection multiple (Priorité 2)
  const [selectedClients, setSelectedClients] = useState<string[]>([]);

  const load = async () => {
    setLoading(true);
    try {
      setClients(await ClientServiceAPI.getListeClients(idEntreprise, showInactive));
      setSelectedClients([]); // Réinitialise la sélection lors du rechargement
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

  // --- GESTION DE LA SÉLECTION MULTIPLE ---
  const handleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.checked) {
      const allIds = filtered.map(c => c.idClient).filter((id): id is string => !!id);
      setSelectedClients(allIds);
    } else {
      setSelectedClients([]);
    }
  };

  const handleSelectOne = (idClient?: string) => {
    if (!idClient) return;
    setSelectedClients(prev =>
      prev.includes(idClient) ? prev.filter(id => id !== idClient) : [...prev, idClient]
    );
  };

  // --- ACTIONS EN MASSE ---
  const handleBulkAction = async (action: 'desactiver' | 'reactiver') => {
    if (selectedClients.length === 0) return;

    const label = action === 'desactiver' ? 'désactiver' : 'réactiver';
    if (!window.confirm(`Voulez-vous vraiment ${label} les ${selectedClients.length} client(s) sélectionné(s) ?`)) {
      return;
    }

    try {
      if (action === 'desactiver') {
        await ClientServiceAPI.desactiverClientsEnMasse(selectedClients);
      } else {
        await ClientServiceAPI.reactiverClientsEnMasse(selectedClients);
      }
      await load();
    } catch {
      setError(`Impossible de ${label} les clients sélectionnés.`);
    }
  };

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

    if (client.actif) {
      // Depart reel (client present X jours ce mois-ci puis parti) vs simple
      // erreur de saisie a annuler : on distingue via une date de sortie
      // optionnelle, plutot qu'un simple on/off qui ne demandait rien.
      const aujourdHui = new Date().toISOString().slice(0, 10);
      const saisie = window.prompt(
        "Désactiver ce client.\n\n" +
        "S'il s'agit d'un départ réel en cours de mois, indiquez sa date de sortie " +
        "(AAAA-MM-JJ) pour que la facturation en cours soit calculée au prorata.\n" +
        "Laissez le champ vide s'il s'agit d'une erreur de saisie à annuler " +
        "(le client sera alors entièrement retiré des facturations en brouillon).",
        aujourdHui
      );
      if (saisie === null) return; // annulé

      const dateSortie = saisie.trim() === '' ? undefined : saisie.trim();
      try {
        await ClientServiceAPI.desactiverClient(client.idClient, dateSortie);
        await load();
      } catch {
        setError('Impossible de désactiver ce client.');
      }
      return;
    }

    if (!window.confirm('Voulez-vous réactiver ce client ?')) return;
    try {
      await ClientServiceAPI.reactiverClient(client.idClient);
      await load();
    } catch {
      setError('Impossible de modifier le statut du client.');
    }
  };

  const save = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!form.nom.trim() || !form.prenom.trim()) {
      setError('Le nom et le prénom sont obligatoires.');
      return;
    }

    if (form.dateEntree && form.dateSortie && form.dateSortie < form.dateEntree) {
      setError('La date de sortie doit être postérieure ou égale à la date d’entrée.');
      return;
    }

    setSaving(true);

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

  const isAllSelected = filtered.length > 0 && selectedClients.length === filtered.length;

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

      {/* BARRE D'ACTIONS GROUPÉES */}
      {selectedClients.length > 0 && (
        <div className="notice" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#edf2f7', borderColor: '#cbd5e0' }}>
          <span><strong>{selectedClients.length}</strong> client(s) sélectionné(s)</span>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button className="btn btn-secondary" onClick={() => handleBulkAction('desactiver')} style={{ color: '#e53e3e' }}>
              <XSquare size={15} /> Désactiver la sélection
            </button>
            <button className="btn btn-secondary" onClick={() => handleBulkAction('reactiver')} style={{ color: '#38a169' }}>
              <CheckSquare size={15} /> Réactiver la sélection
            </button>
          </div>
        </div>
      )}

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
                  <th style={{ width: '40px' }}>
                    <input
                      type="checkbox"
                      checked={isAllSelected}
                      onChange={handleSelectAll}
                    />
                  </th>
                  <th>CLIENT</th>
                  <th>DATE D’ENTRÉE</th>
                  <th>DATE DE SORTIE</th>
                  <th className="num">TARIF / JOUR</th>
                  <th>STATUT</th>
                  <th className="num">ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(c => {
                  const isSelected = !!c.idClient && selectedClients.includes(c.idClient);
                  return (
                    <tr key={c.idClient} style={{ opacity: c.actif ? 1 : 0.65, backgroundColor: isSelected ? '#f7fafc' : undefined }}>
                      <td>
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleSelectOne(c.idClient)}
                        />
                      </td>
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
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
};