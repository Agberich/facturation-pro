import type { CSSProperties } from 'react';

/**
 * Tokens de design partagés pour les écrans ajoutés à l'application.
 * N'est importé par aucun fichier existant (App.tsx, TableauFacturation.tsx) :
 * purement additif, aucun risque de régression visuelle.
 */
export const theme = {
  color: {
    slate900: '#0f172a',
    slate800: '#1e293b', // en-tête existant, conservé à l'identique
    slate500: '#64748b',
    slate400: '#94a3b8', // statut inactif
    slate200: '#e2e8f0',
    slate50: '#f8fafc',
    blue600: '#2563eb', // accent (proche du bleu déjà utilisé)
    blue700: '#1d4ed8',
    green600: '#16a34a', // statut actif
    green50: '#f0fdf4',
    red600: '#dc2626',
    red50: '#fef2f2',
    white: '#ffffff',
  },
  font: {
    base: `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`,
    tabular: `'Segoe UI', Roboto, sans-serif`, // avec fontVariantNumeric ci-dessous
  },
  radius: '6px',
  shadow: '0 1px 2px rgba(15, 23, 42, 0.06)',
} as const;

/** À appliquer sur toute cellule affichant un montant ou un nombre de jours. */
export const chiffresAlignes: CSSProperties = {
  fontVariantNumeric: 'tabular-nums',
  fontFeatureSettings: '"tnum"',
};
