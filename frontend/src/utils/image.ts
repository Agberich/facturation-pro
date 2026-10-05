/**
 * Lit une image (PNG ou JPEG), la réduit pour qu'elle reste légère et la renvoie sous la forme
 * d'une adresse « data: » enregistrable dans les paramètres. Le serveur refuse au-delà de 350 Ko.
 */
const TAILLE_MAX = 250_000;

export const lireImage = (fichier: File, largeurMax: number): Promise<string> =>
  new Promise((resolve, reject) => {
    if (!['image/png', 'image/jpeg'].includes(fichier.type)) {
      reject(new Error('Choisissez une image PNG ou JPEG.'));
      return;
    }
    const lecteur = new FileReader();
    lecteur.onerror = () => reject(new Error('Impossible de lire ce fichier.'));
    lecteur.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error('Image illisible.'));
      img.onload = () => {
        // On essaie des tailles de plus en plus petites jusqu'à passer sous la limite
        for (const largeur of [largeurMax, Math.round(largeurMax * 0.7), Math.round(largeurMax * 0.45), 150]) {
          const ratio = Math.min(1, largeur / img.width);
          const canvas = document.createElement('canvas');
          canvas.width = Math.max(1, Math.round(img.width * ratio));
          canvas.height = Math.max(1, Math.round(img.height * ratio));
          const ctx = canvas.getContext('2d');
          if (!ctx) {
            reject(new Error('Image non prise en charge par ce navigateur.'));
            return;
          }
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
          // PNG : garde la transparence (utile pour une signature)
          const resultat = canvas.toDataURL('image/png');
          if (resultat.length <= TAILLE_MAX) {
            resolve(resultat);
            return;
          }
        }
        reject(new Error('Image trop volumineuse, même après réduction.'));
      };
      img.src = String(lecteur.result);
    };
    lecteur.readAsDataURL(fichier);
  });
