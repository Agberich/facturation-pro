import React, { useEffect, useState } from 'react';
import { LayoutDashboard, FileText, Users, Upload, Settings2, Menu, X, LogOut, UserCog } from 'lucide-react';
import { Dashboard } from './components/Dashboard';
import { ListeFacturations } from './components/ListeFacturations';
import { DetailFacturation } from './components/DetailFacturation';
import { GestionClients } from './components/GestionClients';
import { ImportClients } from './components/ImportClients';
import { ParametresEntreprise } from './components/ParametresEntreprise';
import { GestionUtilisateurs } from './pages/GestionUtilisateurs';
import { Login } from './components/Login';
import { definirContexte, definirJeton } from './services/api';
import { ConnexionReponse } from './types/facturation';

type Page = 'dashboard' | 'facturations' | 'clients' | 'import' | 'utilisateurs' | 'parametres';

const nav = [
  { id: 'dashboard', label: 'Tableau de bord', icon: LayoutDashboard },
  { id: 'facturations', label: 'Facturations', icon: FileText },
  { id: 'clients', label: 'Clients', icon: Users },
  { id: 'import', label: 'Importation', icon: Upload },
  { id: 'utilisateurs', label: 'Utilisateurs', icon: UserCog },
  { id: 'parametres', label: 'Paramètres', icon: Settings2 },
] as const;

const CLE_STOCKAGE = 'facturation_auth';

const initiales = (nom: string) =>
  nom.split(' ').filter(Boolean).map((m) => m[0]).slice(0, 2).join('').toUpperCase();

export const App: React.FC = () => {
  const [auth, setAuth] = useState<ConnexionReponse | null>(null);
  const [chargementInitial, setChargementInitial] = useState(true);
  const [page, setPage] = useState<Page>('dashboard');
  const [selected, setSelected] = useState<string | null>(null);
  const [mobile, setMobile] = useState(false);

  // Au chargement de la page, on tente de restaurer une session existante
  // (jeton sauvegardé localement) avant d'afficher l'écran de connexion.
  useEffect(() => {
    const brut = localStorage.getItem(CLE_STOCKAGE);
    if (brut) {
      try {
        const reponse: ConnexionReponse = JSON.parse(brut);
        definirJeton(reponse.token);
        definirContexte(reponse.idEntreprise, reponse.idUtilisateur);
        setAuth(reponse);
      } catch {
        localStorage.removeItem(CLE_STOCKAGE);
      }
    }
    setChargementInitial(false);
  }, []);

  const handleConnexionReussie = (reponse: ConnexionReponse) => {
    localStorage.setItem(CLE_STOCKAGE, JSON.stringify(reponse));
    definirJeton(reponse.token);
    definirContexte(reponse.idEntreprise, reponse.idUtilisateur);
    setAuth(reponse);
  };

  const handleDeconnexion = () => {
    localStorage.removeItem(CLE_STOCKAGE);
    definirJeton(null);
    setAuth(null);
    setPage('dashboard');
    setSelected(null);
  };

  const go = (p: Page) => {
    setPage(p);
    setSelected(null);
    setMobile(false);
  };

  if (chargementInitial) {
    return null; // Évite un flash de l'écran de connexion pendant la restauration de session
  }

  if (!auth) {
    return <Login onConnexionReussie={handleConnexionReussie} />;
  }

  const active = nav.find((n) => n.id === page);
  const idEntreprise = auth.idEntreprise;

  return (
    <div className="app-shell">
      <aside className={`sidebar ${mobile ? 'open' : ''}`}>
        <div className="brand">
          <div className="brand-mark">F</div>
          <div>
            <div className="brand-title">Facturation Pro</div>
            <div className="brand-sub">Gestion mensuelle</div>
          </div>
        </div>

        <div className="nav-label">Navigation</div>
        <nav className="side-nav">
          {nav.map((n) => {
            const Icon = n.icon;
            return (
              <button
                key={n.id}
                className={`nav-btn ${page === n.id ? 'active' : ''}`}
                onClick={() => go(n.id)}
              >
                <Icon size={17} />
                {n.label}
              </button>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          Entreprise démo
          <br />
          <span>Gestion de facturation</span>
        </div>
      </aside>

      <div className="main-shell">
        <header className="topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <button className="btn mobile-toggle" onClick={() => setMobile((v) => !v)}>
              {mobile ? <X size={16} /> : <Menu size={16} />}
            </button>
            <div>
              <div className="crumb">Espace de gestion / {active?.label}</div>
              <div className="top-title">{active?.label}</div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div className="user-chip">
              <div className="avatar">{initiales(auth.nom)}</div>
              <span>{auth.nom}</span>
            </div>
            <button
              className="btn"
              onClick={handleDeconnexion}
              title="Se déconnecter"
              style={{ display: 'flex', alignItems: 'center', gap: 6 }}
            >
              <LogOut size={15} />
              Déconnexion
            </button>
          </div>
        </header>

        <main className="content">
          {page === 'dashboard' && (
            <Dashboard
              idEntreprise={idEntreprise}
              onNavigate={go}
              onOpenFacturation={(id) => {
                setPage('facturations');
                setSelected(id);
              }}
            />
          )}
          {page === 'facturations' &&
            (selected ? (
              <DetailFacturation idFacturation={selected} onRetour={() => setSelected(null)} />
            ) : (
              <ListeFacturations idEntreprise={idEntreprise} onOuvrirFacturation={setSelected} />
            ))}
          {page === 'clients' && <GestionClients idEntreprise={idEntreprise} />}
          {page === 'import' && <ImportClients idEntreprise={idEntreprise} />}
          {page === 'utilisateurs' && <GestionUtilisateurs />}
          {page === 'parametres' && <ParametresEntreprise idEntreprise={idEntreprise} />}
        </main>
      </div>
    </div>
  );
};

export default App;