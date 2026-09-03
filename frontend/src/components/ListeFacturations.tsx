import React, { useEffect, useMemo, useState } from 'react';
import { CalendarPlus, Search, ArrowUpRight } from 'lucide-react';
import { FacturationServiceAPI } from '../services/api';
import { Facturation, StatutFacturation } from '../types/facturation';

interface Props {
  idEntreprise: string;
  onOuvrirFacturation?: (id: string) => void;
}

const mois = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
const status = (s: StatutFacturation) =>
  s === 'VALIDEE' ? ['status-valid', 'Validée'] : s === 'ARCHIVE' ? ['status-arch', 'Archivée'] : ['status-draft', 'Brouillon'];

// Fonctions d'initialisation avec persistance
const getInitialAnnee = () => {
  const saved = localStorage.getItem('facturation_annee');
  return saved ? parseInt(saved, 10) : new Date().getFullYear();
};

const getInitialMois = () => {
  const saved = localStorage.getItem('facturation_mois');
  return saved ? parseInt(saved, 10) : new Date().getMonth() + 1;
};

export const ListeFacturations: React.FC<Props> = ({ idEntreprise, onOuvrirFacturation }) => {
  const [items, setItems] = useState<Facturation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // États initialisés avec localStorage
  const [annee, setAnnee] = useState(getInitialAnnee);
  const [moisSel, setMoisSel] = useState(getInitialMois);
  const [creating, setCreating] = useState(false);
  const [search, setSearch] = useState('');

  // Sauvegarde dans localStorage lors du changement des filtres
  useEffect(() => {
    localStorage.setItem('facturation_annee', annee.toString());
    localStorage.setItem('facturation_mois', moisSel.toString());
  }, [annee, moisSel]);

  const load = async () => {
    setLoading(true);
    try {
      setItems(await FacturationServiceAPI.listerFacturations(idEntreprise));
      setError('');
    } catch {
      setError('Impossible de charger les facturations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [idEntreprise]);

  const filtered = useMemo(
    () => items.filter(f => `${mois[f.mois - 1]} ${f.annee} ${f.numeroFacture || ''}`.toLowerCase().includes(search.toLowerCase())),
    [items, search]
  );

  const create = async () => {
    setCreating(true);
    try {
      const facturation = await FacturationServiceAPI.initialiserMois(idEntreprise, annee, moisSel);
      await load();
      if (onOuvrirFacturation && facturation.idFacturation) {
        onOuvrirFacturation(facturation.idFacturation);
      }
    } catch (e: any) {
      setError(e.response?.data?.message || "Impossible d'ouvrir ce mois.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <div className="page-head">
        <div>
          <div className="eyebrow">Gestion mensuelle</div>
          <h1 className="page-title">Facturations</h1>
          <p className="page-desc">Créez, suivez, validez et exportez vos périodes de facturation.</p>
        </div>
      </div>

      <div className="card period-card">
        <CalendarPlus size={20} color="#175cd3" />
        <div className="field">
          <label>Mois à ouvrir</label>
          <select value={moisSel} onChange={e => setMoisSel(+e.target.value)}>
            {mois.map((m, i) => (
              <option key={m} value={i + 1}>
                {m}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>Année</label>
          <input type="number" value={annee} onChange={e => setAnnee(+e.target.value)} />
        </div>
        <button className="btn btn-primary" onClick={create} disabled={creating}>
          {creating ? 'Ouverture…' : 'Ouvrir la période'}
        </button>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      <section className="card table-card">
        <div className="table-tools">
          <div>
            <div className="section-title">Toutes les facturations</div>
            <div className="section-sub">
              {items.length} période{items.length !== 1 ? 's' : ''} enregistrée{items.length !== 1 ? 's' : ''}
            </div>
          </div>
          <div className="search">
            <Search size={15} />
            <input placeholder="Rechercher une période…" value={search} onChange={e => setSearch(e.target.value)} />
          </div>
        </div>

        {loading ? (
          <div className="loading">Chargement des facturations…</div>
        ) : filtered.length === 0 ? (
          <div className="empty">
            <div className="empty-icon">
              <CalendarPlus size={19} />
            </div>
            Aucune facturation correspondante.
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>PÉRIODE</th>
                  <th>N° FACTURE</th>
                  <th>STATUT</th>
                  <th>VALIDATION</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filtered
                  .sort((a, b) => b.annee - a.annee || b.mois - a.mois)
                  .map(f => {
                    const [cls, label] = status(f.statut);
                    return (
                      <tr key={f.idFacturation}>
                        <td>
                          <strong>
                            {mois[f.mois - 1]} {f.annee}
                          </strong>
                        </td>
                        <td>{f.numeroFacture || '—'}</td>
                        <td>
                          <span className={`status ${cls}`}>{label}</span>
                        </td>
                        <td>{f.dateValidation ? new Date(f.dateValidation).toLocaleDateString('fr-FR') : '—'}</td>
                        <td className="num">
                          <button className="btn" onClick={() => onOuvrirFacturation?.(f.idFacturation)}>
                            Voir <ArrowUpRight size={13} />
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