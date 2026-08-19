import React from 'react';

interface Props { children: React.ReactNode }
interface State { hasError: boolean; message: string }

// Filet de securite : si un composant plante (ex. donnee inattendue venant de l'API),
// on affiche un message au lieu de laisser React demonter toute l'app (page blanche).
export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, message: '' };
  }

  static getDerivedStateFromError(error: unknown): State {
    return { hasError: true, message: error instanceof Error ? error.message : String(error) };
  }

  componentDidCatch(error: unknown, info: React.ErrorInfo) {
    console.error('Erreur interceptee par ErrorBoundary :', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 32, fontFamily: 'sans-serif' }}>
          <h2 style={{ marginBottom: 8 }}>Une erreur est survenue</h2>
          <p style={{ color: '#666', marginBottom: 16 }}>{this.state.message}</p>
          <button
            className="btn"
            onClick={() => { this.setState({ hasError: false, message: '' }); window.location.reload(); }}
          >
            Recharger la page
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
