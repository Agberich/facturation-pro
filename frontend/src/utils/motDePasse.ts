/** Même règle que le serveur (PolitiqueMotDePasse.java) : 10 caractères minimum, une lettre et un chiffre. */
export const LONGUEUR_MIN_MOT_DE_PASSE = 10;

export const AIDE_MOT_DE_PASSE = `${LONGUEUR_MIN_MOT_DE_PASSE} caractères minimum, avec au moins une lettre et un chiffre.`;

/** Retourne un message d'erreur, ou null si le mot de passe respecte la règle. */
export const verifierMotDePasse = (mdp: string): string | null => {
  if (mdp.length < LONGUEUR_MIN_MOT_DE_PASSE) {
    return `Le mot de passe doit contenir au moins ${LONGUEUR_MIN_MOT_DE_PASSE} caractères.`;
  }
  if (!/\p{L}/u.test(mdp) || !/\d/.test(mdp)) {
    return 'Le mot de passe doit contenir au moins une lettre et un chiffre.';
  }
  return null;
};
