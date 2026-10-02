import { StatutFacturation } from '../types/facturation';

/** Classe CSS et libellé d'un statut de facturation (source unique pour tous les écrans). */
export const statutFacturation = (s: StatutFacturation): [string, string] => {
  switch (s) {
    case 'PAYEE':
      return ['status-paid', 'Payée'];
    case 'VALIDEE':
      return ['status-valid', 'Validée'];
    case 'ARCHIVE':
      return ['status-arch', 'Archivée'];
    default:
      return ['status-draft', 'Brouillon'];
  }
};
