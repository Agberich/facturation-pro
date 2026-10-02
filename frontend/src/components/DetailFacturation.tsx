import React, { useEffect, useState } from 'react';
import { ArrowLeft, RefreshCw, CheckCircle2, Unlock, FileDown, FileSpreadsheet, FileText, Wallet, RotateCcw } from 'lucide-react';
import { FacturationServiceAPI, ExportServiceAPI, ParametreServiceAPI } from '../services/api';
import { Facturation, LigneFacturation, StatutLigne } from '../types/facturation';
import { statutFacturation } from '../utils/statut';
import { montant, montantDevise } from '../utils/format';

interface Props {
  idFacturation: string;
  idEntreprise: string;
  onRetour: () => void;
}

const mois = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
const lineStatus = (s: StatutLigne) => s === 'NOUVEAU' ? ['status-new', 'Nouveau'] : s === 'SORTI' ? ['status-out', 'Sorti'] : s === 'SUSPENDU' ? ['status-suspended', 'Suspendu'] : ['status-active', 'Actif'];

export const DetailFacturation: React.FC<Props> = ({ idFacturation, idEntreprise, onRetour }) => {
  const [f, setF] = useState<Facturation | null>(null);
  const [lines, setLines] = useState<LigneFacturation[]>([]);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState('');
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState('');
  const [devise, setDevise] = useState('EUR');

  const load = async () => {
    setLoading(true);
    try {
      const [a, b] = await Promise.all([
        FacturationServiceAPI.obtenirFacturation(idFacturation),
        FacturationServiceAPI.listerLignes(idFacturation)
      ]);
      setF(a);
      setLines(b);

      // Devise définie dans les paramètres de l'entreprise
      ParametreServiceAPI.obtenirParametres(idEntreprise)
        .then(p => setDevise(p.devise || 'EUR'))
        .catch(() => undefined);

      // Synchronisation du mois et de l'année courante consultés
      if (a) {
        localStorage.setItem('facturation_annee', a.annee.toString());
        localStorage.setItem('facturation_mois', a.mois.toString());
      }
    } catch {
      setError('Impossible de charger cette facturation.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [idFacturation]);

  const generate = async () => {
    setAction('generate');
    try {
      setLines(await FacturationServiceAPI.genererLignes(idFacturation));
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur de génération.');
    } finally {
      setAction('');
    }
  };

  const validate = async () => {
    setAction('validate');
    try {
      setF(await FacturationServiceAPI.validerFacture(idFacturation));
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur de validation.');
    } finally {
      setAction('');
    }
  };

  const pay = async () => {
    setAction('pay');
    try {
      setF(await FacturationServiceAPI.marquerCommePayee(idFacturation));
      setError('');
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur lors du paiement.');
    } finally {
      setAction('');
    }
  };

  const cancelPayment = async () => {
    setAction('cancelPay');
    try {
      setF(await FacturationServiceAPI.annulerPaiement(idFacturation));
      setError('');
    } catch (e: any) {
      setError(e.response?.data?.message || "Erreur lors de l'annulation du paiement.");
    } finally {
      setAction('');
    }
  };

  const reopen = async () => {
    setAction('reopen');
    try {
      setF(await FacturationServiceAPI.reouvrirFacture(idFacturation));
    } catch (e: any) {
      setError(e.response?.data?.message || 'Erreur de réouverture.');
    } finally {
      setAction('');
    }
  };

  const handleExportPdf = async () => {
    setExporting('pdf');
    try {
      await ExportServiceAPI.telechargerPdf(idFacturation, f?.numeroFacture);
    } catch (e) {
      setError('Erreur lors du téléchargement du PDF.');
    } finally {
      setExporting('');
    }
  };

  const handleExportExcel = async () => {
    setExporting('excel');
    try {
      await ExportServiceAPI.telechargerExcel(idFacturation, f?.numeroFacture);
    } catch (e) {
      setError('Erreur lors du téléchargement du fichier Excel.');
    } finally {
      setExporting('');
    }
  };

  const handleExportCsv = async () => {
    setExporting('csv');
    try {
      await ExportServiceAPI.telechargerCsv(idFacturation, f?.numeroFacture);
    } catch (e) {
      setError('Erreur lors du téléchargement du fichier CSV.');
    } finally {
      setExporting('');
    }
  };

  if (loading) return <div className="loading">Chargement de la facturation…</div>;
  if (!f) return <div className="empty">Facturation introuvable.<br /><button className="btn" onClick={onRetour} style={{ marginTop: 12 }}>Retour</button></div>;

  const total = lines.reduce((a, l) => ({ ht: a.ht + l.montantHt, tva: a.tva + l.montantTva, ttc: a.ttc + l.montantTtc, j: a.j + l.nbJours }), { ht: 0, tva: 0, ttc: 0, j: 0 });
  const draft = f.statut === 'BROUILLON';
  const validee = f.statut === 'VALIDEE';
  const payee = f.statut === 'PAYEE';
  const [statutCls, statutLabel] = statutFacturation(f.statut);

  return (
    <>
      <button className="btn" onClick={onRetour} style={{ marginBottom: 18 }}>
        <ArrowLeft size={14} />Retour aux facturations
      </button>

      <div className="detail-hero">
        <div>
          <div className="eyebrow">Facturation mensuelle</div>
          <h1 className="page-title">{mois[f.mois - 1]} {f.annee}</h1>
          <div className="detail-number">{f.numeroFacture ? `N° ${f.numeroFacture}` : 'Numéro attribué à la validation'}</div>
          <div style={{ marginTop: 8 }}>
            <span className={`status ${statutCls}`}>{statutLabel}</span>
            {payee && f.datePaiement && (
              <span style={{ marginLeft: 10, fontSize: 12, color: '#667085' }}>
                le {new Date(f.datePaiement).toLocaleDateString('fr-FR')}
              </span>
            )}
          </div>
        </div>
        <div className="actions">
          {draft && (
            <button className="btn" onClick={generate} disabled={!!action}>
              <RefreshCw size={14} />{action === 'generate' ? 'Calcul…' : 'Générer / recalculer'}
            </button>
          )}
          {draft && (
            <button className="btn btn-success" onClick={validate} disabled={!lines.length || !!action}>
              <CheckCircle2 size={14} />{action === 'validate' ? 'Validation…' : 'Valider'}
            </button>
          )}
          {validee && (
            <button className="btn btn-success" onClick={pay} disabled={!!action}>
              <Wallet size={14} />{action === 'pay' ? 'Enregistrement…' : 'Marquer comme payée'}
            </button>
          )}
          {validee && (
            <button className="btn btn-warning" onClick={reopen} disabled={!!action}>
              <Unlock size={14} />{action === 'reopen' ? 'Réouverture…' : 'Réouvrir'}
            </button>
          )}
          {payee && (
            <button className="btn btn-warning" onClick={cancelPayment} disabled={!!action}>
              <RotateCcw size={14} />{action === 'cancelPay' ? 'Annulation…' : 'Annuler le paiement'}
            </button>
          )}
        </div>
      </div>

      {error && <div className="notice notice-error">{error}</div>}

      <div className="grid summary-grid">
        <div className="card summary">
          <div className="summary-label">Personnes facturées</div>
          <div className="summary-value">{lines.length}</div>
        </div>
        <div className="card summary">
          <div className="summary-label">Jours facturés</div>
          <div className="summary-value">{total.j}</div>
        </div>
        <div className="card summary">
          <div className="summary-label">Total HT</div>
          <div className="summary-value">{montantDevise(total.ht, devise)}</div>
        </div>
        <div className="card summary summary-highlight">
          <div className="summary-label">Total TTC</div>
          <div className="summary-value">{montantDevise(total.ttc, devise)}</div>
        </div>
      </div>

      {lines.length > 0 && (
        <div className="export-row">
          <button className="btn" onClick={handleExportPdf} disabled={!!exporting}>
            <FileDown size={14} />{exporting === 'pdf' ? 'Téléchargement…' : 'PDF'}
          </button>
          <button className="btn" onClick={handleExportExcel} disabled={!!exporting}>
            <FileSpreadsheet size={14} />{exporting === 'excel' ? 'Téléchargement…' : 'Excel'}
          </button>
          <button className="btn" onClick={handleExportCsv} disabled={!!exporting}>
            <FileText size={14} />{exporting === 'csv' ? 'Téléchargement…' : 'CSV'}
          </button>
        </div>
      )}

      <section className="card table-card">
        <div className="table-tools">
          <div>
            <div className="section-title">Lignes de facturation</div>
            <div className="section-sub">Calculées à partir des personnes accueillies actives et des règles du mois.</div>
          </div>
        </div>

        {!lines.length ? (
          <div className="empty">
            <div className="empty-icon"><RefreshCw size={19} /></div>
            Aucune ligne pour le moment.
            {draft && <><br />Utilisez « Générer / recalculer » pour créer les lignes.</>}
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>PERSONNE ACCUEILLIE</th>
                  <th>STATUT</th>
                  <th className="num">JOURS</th>
                  <th className="num">TARIF</th>
                  <th className="num">HT</th>
                  <th className="num">TVA</th>
                  <th className="num">TTC</th>
                </tr>
              </thead>
              <tbody>
                {lines.map(l => {
                  const [s, label] = lineStatus(l.statut);
                  return (
                    <tr key={l.idLigne}>
                      <td><strong>{l.client ? l.client.nom + ' ' + l.client.prenom : 'Personne inconnue'}</strong></td>
                      <td><span className={`status ${s}`}>{label}</span></td>
                      <td className="num">{l.nbJours}</td>
                      <td className="num">{montant(l.tarifApplique)}</td>
                      <td className="num">{montant(l.montantHt)}</td>
                      <td className="num">{montant(l.montantTva)}</td>
                      <td className="num"><strong>{montant(l.montantTtc)}</strong></td>
                    </tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={4}><strong>Total</strong></td>
                  <td className="num"><strong>{montant(total.ht)}</strong></td>
                  <td className="num"><strong>{montant(total.tva)}</strong></td>
                  <td className="num"><strong>{montant(total.ttc)}</strong></td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </section>
    </>
  );
};