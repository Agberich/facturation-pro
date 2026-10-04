package com.facturation.service;

import java.nio.charset.StandardCharsets;

/**
 * Règles appliquées à tout mot de passe défini dans l'application
 * (premier administrateur, création/modification d'utilisateur, changement).
 * Les mots de passe déjà existants ne sont pas remis en cause : la règle ne
 * s'applique qu'au moment où un mot de passe est défini.
 */
public final class PolitiqueMotDePasse {

    public static final int LONGUEUR_MIN = 10;
    /** BCrypt ne prend en compte que les 72 premiers octets du mot de passe. */
    public static final int LONGUEUR_MAX_OCTETS = 72;

    private PolitiqueMotDePasse() {
    }

    public static void valider(String motDePasse) {
        if (motDePasse == null || motDePasse.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }
        if (motDePasse.length() < LONGUEUR_MIN) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MIN + " caractères.");
        }
        if (motDePasse.getBytes(StandardCharsets.UTF_8).length > LONGUEUR_MAX_OCTETS) {
            throw new IllegalArgumentException("Le mot de passe est trop long (72 octets maximum).");
        }
        boolean contientLettre = motDePasse.chars().anyMatch(Character::isLetter);
        boolean contientChiffre = motDePasse.chars().anyMatch(Character::isDigit);
        if (!contientLettre || !contientChiffre) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins une lettre et un chiffre.");
        }
    }
}
