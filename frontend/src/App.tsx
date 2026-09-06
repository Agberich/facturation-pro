import React, { useEffect, useState } from 'react';
import './styles/app.css'; // <-- Import indispensable pour charger les classes CSS de l'application
import { 
  LayoutDashboard, 
  FileText, 
  Users, 
  Upload, 
  Settings2, 
  Menu, 
  X, 
  LogOut, 
  UserCog, 
  RefreshCw, 
  ServerOff 
} from 'lucide-react';

import { Dashboard } from './components/Dashboard';
import { ListeFacturations } from './components/ListeFacturations';
import { DetailFacturation } from './components/DetailFacturation';
import { GestionClients } from './components/GestionClients';
import { ImportClients } from './components/ImportClients';
import { ParametresEntreprise } from './components/ParametresEntreprise';
import { GestionUtilisateurs } from './pages/GestionUtilisateurs';
import Login from './pages/Login';
import { PremierAdmin } from './pages/PremierAdmin';
import { ChangerMotDePasse } from './pages/ChangerMotDePasse';

import { AuthServiceAPI, LoginResponse } from './services/authService';

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

const initiales = (nom?: string) =>
  nom ? nom.split(' ').filter(Boolean).map((m) => m[0]).slice(0, 2).join('').toUpperCase() : 'U';

export const App: React.FC = () => {
  const [auth, setAuth] = useState<LoginResponse | null>(null);
  const [etatServeur, setEtatServeur] = useState<'loading' | 'ok' | 'erreur'>('loading');
  const [premierAdminExiste, setPremierAdminExiste] = useState<boolean>(true);
  const [afficherChangerMotDePasse, setAfficherChangerMotDePasse] = useState<boolean>(false);

  const [page, setPage] = useState<Page>('dashboard');
  const [selected, setSelected] = useState<string | null>(null);
  const [mobile, setMobile] = useState(false);

  useEffect(() => {
    const sessionBrute = localStorage.getItem(CLE_STOCKAGE);
    
    if (sessionBrute) {
      try {
        const reponseSession: LoginResponse = JSON.parse(sessionBrute);
        setAuth(reponseSession);
        if (reponseSession.doitChangerMotDePasse || reponseSession.utilisateur?.doitChangerMotDePasse) {
          setAfficherChangerMotDePasse(true);
        }
        setEtatServeur('ok');
      } catch {
        localStorage.removeItem(CLE_STOCKAGE);
        verifierServeur();
      }
    } else {
      verifierServeur();
    }
  }, []);

  const verifierServeur = () => {
    AuthServiceAPI.verifierPremierAdminExiste()
      .then((res: any) => {
        const existe = typeof res === 'boolean' ? res : (res?.existe ?? true);
        setPremierAdminExiste(existe);
        setEtatServeur('ok');
      })
      .catch(() => {
        setEtatServeur('erreur');
      });
  };

  const handleConnexionReussie = (response: LoginResponse) => {
    const utilisateurSession = response.utilisateur || {
      id: response.idUtilisateur || '',
      nom: response.nom || '',
      email: '',
      role: '',
    };

    const objetAuth: LoginResponse = {
      token: response.token,
      idEntreprise: response.idEntreprise,
      idUtilisateur: response.idUtilisateur || utilisateurSession.id,
      nom: response.nom || utilisateurSession.nom,
      utilisateur: utilisateurSession,
      doitChangerMotDePasse: response.doitChangerMotDePasse ?? utilisateurSession.doitChangerMotDePasse
    };

    localStorage.setItem(CLE_STOCKAGE, JSON.stringify(objetAuth));
    setAuth(objetAuth);
    setPremierAdminExiste(true);

    if (objetAuth.doitChangerMotDePasse) {
      setAfficherChangerMotDePasse(true);
    }
  };

  const handleDeconnexion = () => {
    localStorage.removeItem(CLE_STOCKAGE);
    setAuth(null);
    setAfficherChangerMotDePasse(false);
    setPage('dashboard');
    setSelected(null);
    
    verifierServeur();
  };

  const go = (p: Page) => {
    setPage(p);
    setSelected(null);
    setMobile(false);
  };

  if (etatServeur === 'loading') {
    return (
      <div className="auth-container">
        <div className="auth-card" style={{ textAlign: 'center', padding: '40px' }}>
          <RefreshCw size={32} className="spin-icon" style={{ marginBottom: 16 }} />
          <h3>Connexion au serveur backend…</h3>
          <p style={{ color: '#666', fontSize: '0.9rem', marginTop: 8 }}>
            Démarrage des services en cours, veuillez patienter.
          </p>
        </div>
      </div>
    );
  }

  if (etatServeur === 'erreur' && !auth) {
    return (
      <div className="auth-container">
        <div className="auth-card" style={{ textAlign: 'center', padding: '40px' }}>
          <ServerOff size={40} color="#dc2626" style={{ marginBottom: 16 }} />
          <h2>Serveur indisponible</h2>
          <p style={{ color: '#666', margin: '12px 0 24px 0' }}>
            Impossible de contacter l'API. Si le service Render était en veille, la relance peut prendre environ 1 minute.
          </p>
          <button className="btn btn-primary" onClick={() => window.location.reload()}>
            Réessayer la connexion
          </button>
        </div>
      </div>
    );
  }

  if (!auth) {
    if (!premierAdminExiste) {
      return <PremierAdmin onSuccess={handleConnexionReussie} />;
    }
    return <Login onSuccess={handleConnexionReussie} />;
  }

  if (afficherChangerMotDePasse || auth.doitChangerMotDePasse) {
    return (
      <ChangerMotDePasse
        onSuccess={() => {
          const authAjour: LoginResponse = {
            ...auth,
            doitChangerMotDePasse: false,
            utilisateur: auth.utilisateur
              ? { ...auth.utilisateur, doitChangerMotDePasse: false }
              : undefined,
          };
          localStorage.setItem(CLE_STOCKAGE, JSON.stringify(authAjour));
          setAuth(authAjour);
          setAfficherChangerMotDePasse(false);
        }}
      />
    );
  }

  const active = nav.find((n) => n.id === page);
  const idEntreprise = auth.idEntreprise;
  const nomUtilisateur = auth.nom || auth.utilisateur?.nom || 'Utilisateur';

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
          Espace Entreprise
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
              <div className="avatar">{initiales(nomUtilisateur)}</div>
              <span>{nomUtilisateur}</span>
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