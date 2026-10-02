/** Montant avec 2 décimales, sans symbole (ex. « 7 192,80 »). */
export const montant = (n: number | undefined | null): string =>
  (n ?? 0).toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Montant avec le symbole de la devise choisie dans les paramètres (ex. « 7 192,80 € »). */
export const montantDevise = (n: number | undefined | null, devise: string = 'EUR'): string => {
  try {
    return (n ?? 0).toLocaleString('fr-FR', { style: 'currency', currency: devise, minimumFractionDigits: 2, maximumFractionDigits: 2 });
  } catch {
    return `${montant(n)} ${devise}`;
  }
};
