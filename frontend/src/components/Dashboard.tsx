import React, { useEffect, useMemo, useState } from 'react';
import { ArrowUpRight, FileText, Users, Wallet, Tag, Plus, Upload, Eye } from 'lucide-react';
import { ClientServiceAPI, FacturationServiceAPI, ParametreServiceAPI } from '../services/api';
import { Client, Facturation, Parametre, ResumeFacturation } from '../types/facturation';
import { statutFacturation } from '../utils/statut';
import { montantDevise } from '../utils/format';

interface Props {
  idEntreprise: string;
  onNavigate: (page: 'facturations' | 'clients' | 'import' | 'parametres') => void;
  onOpenFacturation: (id: string) => void;
}

const MOIS = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

export const Dashboard: React.FC<Props> = ({ idEntreprise, onNavigate, onOpenFacturation }) => {
  const [factures, setFactures] = useState<Facturation[]>([]);
  const [resumes, setResumes] = useState<ResumeFacturation[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [parametre, setParametre] = useState<Parametre | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      FacturationServiceAPI.listerFacturations(idEntreprise),
      FacturationServiceAPI.listerResumes(idEntreprise),
      ClientServiceAPI.getListeClients(idEntreprise),
      ParametreServiceAPI.obtenirParametres(idEntreprise).catch(() => null)
    ])
      .then(([f, r, c, p]) => {
        setFactures(f);
        setResumes(r);
        setClients(c);
        setParametre(p);
      })
      .finally(() => setLoading(false));
  }, [idEntreprise]);

  const devise = parametre?.devise || 'EUR';
  const resumeDe = (id: string) => resumes.find(r => r.idFacturation === id);

  // Chiffre d'affaires perçu : uniquement les factures PAYÉES (TTC)
  const chiffreAffairesPercu = useMemo(
    () => factures.filter(f => f.statut === 'PAYEE').reduce((somme, f) => somme + (resumeDe(f.idFacturation)?.totalTtc ?? 0), 0),
    [factures, resumes]
  );
  const nbPayees = factures.filter(f => f.statut === 'PAYEE').length;
  // Factures émises : validées + payées (un brouillon n'est pas encore une facture)
  const emises = factures.filter(f => f.statut === 'VALIDEE' || f.statut === 'PAYEE');
  const totalEmis = emises.reduce((somme, f) => somme + (resumeDe(f.idFacturation)?.totalTtc ?? 0), 0);
  const actifs = clients.filter(c => c.actif).length;

  if (loading) return <div className="loading">Chargement du tableau de bord…</div>;

  return (
    <>
      <div className="page-head">
        <div>
          <div className="eyebrow">Vue d'ensemble</div>
          <h1 className="page-title">Tableau de bord</h1>
          <p className="page-desc">Pilotez vos facturations mensuelles depuis un seul espace.</p>
        </div>
        <div className="actions">
          <button
            className="btn btn-primary"
            onClick={() => {
              const now = new Date();
              localStorage.setItem('facturation_annee', now.getFullYear().toString());
              localStorage.setItem('facturation_mois', (now.getMonth() + 1).toString());
              onNavigate('facturations');
            }}
          >
            <Plus size={15} /> Nouvelle facturation
          </button>
          <button className="btn" onClick={() => onNavigate('clients')}>
            <Users size={15} /> Ajouter une personne accueillie
          </button>
          <button className="btn" onClick={() => onNavigate('import')}>
            <Upload size={15} /> Importer
          </button>
        </div>
      </div>

      <div className="grid kpi-grid">
        <div className="card kpi">
          <div className="kpi-top">
            <span className="kpi-label">Personnes accueillies</span>
            <span className="kpi-icon"><Users size={16} /></span>
          </div>
          <div className="kpi-value">{actifs}</div>
          <div className="kpi-meta">actives sur {clients.length} enregistrée{clients.length !== 1 ? 's' : ''}</div>
        </div>

        <div className="card kpi">
          <div className="kpi-top">
            <span className="kpi-label">Chiffre d'affaires perçu</span>
            <span className="kpi-icon"><Wallet size={16} /></span>
          </div>
          <div className="kpi-value">{montantDevise(chiffreAffairesPercu, devise)}</div>
          <div className="kpi-meta">{nbPayees} facture{nbPayees !== 1 ? 's' : ''} payée{nbPayees !== 1 ? 's' : ''}</div>
        </div>

        <div className="card kpi">
          <div className="kpi-top">
            <span className="kpi-label">Factures émises</span>
            <span className="kpi-icon"><FileText size={16} /></span>
          </div>
          <div className="kpi-value">{emises.length}</div>
          <div className="kpi-meta">{montantDevise(totalEmis, devise)} au total</div>
        </div>

        <div className="card kpi">
          <div className="kpi-top">
            <span className="kpi-label">Tarif journalier actuel</span>
            <span className="kpi-icon"><Tag size={16} /></span>
          </div>
          <div className="kpi-value">{montantDevise(parametre?.tarifJournalier ?? 79.92, devise)}</div>
          <div className="kpi-meta">TTC, par jour et par personne</div>
        </div>
      </div>

      <section className="card table-card" style={{ marginTop: 16 }}>
        <div className="table-tools">
          <div>
            <div className="section-title">Factures mensuelles récentes</div>
            <div className="section-sub">Accès direct aux dernières périodes</div>
          </div>
          <button className="btn" onClick={() => onNavigate('facturations')}>
            Voir tout <ArrowUpRight size={14} />
          </button>
        </div>
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>N° FACTURE</th>
                <th>MOIS / PÉRIODE</th>
                <th>PERSONNES PRISES EN COMPTE</th>
                <th className="num">MONTANT TOTAL (TTC)</th>
                <th>STATUT</th>
                <th>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              {factures
                .slice()
                .sort((a, b) => b.annee - a.annee || b.mois - a.mois)
                .slice(0, 5)
                .map(f => {
                  const [cls, label] = statutFacturation(f.statut);
                  const r = resumeDe(f.idFacturation);
                  return (
                    <tr key={f.idFacturation}>
                      <td><strong>{f.numeroFacture || '—'}</strong></td>
                      <td>{MOIS[f.mois - 1]} {f.annee}</td>
                      <td>{r ? `${r.nbPersonnes} personne${r.nbPersonnes !== 1 ? 's' : ''}` : '—'}</td>
                      <td className="num">{r ? montantDevise(r.totalTtc, devise) : '—'}</td>
                      <td><span className={`status ${cls}`}>{label}</span></td>
                      <td>
                        <button className="btn" onClick={() => onOpenFacturation(f.idFacturation)} aria-label="Voir la facture">
                          <Eye size={14} /> Voir
                        </button>
                      </td>
                    </tr>
                  );
                })}
              {factures.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', color: '#667085', padding: 28 }}>
                    Aucune facturation pour le moment.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
};
