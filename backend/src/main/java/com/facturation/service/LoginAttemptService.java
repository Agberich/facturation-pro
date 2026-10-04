package com.facturation.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les tentatives de connexion : après MAX_ECHECS échecs dans une fenêtre
 * de FENETRE, l'adresse e-mail est bloquée pendant BLOCAGE.
 *
 * Le compteur est conservé en mémoire (une seule instance du backend) : il est
 * remis à zéro au redémarrage. La clé est l'e-mail (et non l'adresse IP) car
 * derrière le proxy de l'hébergeur l'IP du client n'est pas fiable.
 * Une connexion réussie remet le compteur à zéro.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ECHECS = 5;
    private static final Duration FENETRE = Duration.ofMinutes(15);
    private static final Duration BLOCAGE = Duration.ofMinutes(10);
    private static final int TAILLE_MAX_TABLE = 5000;

    private static final class Compteur {
        int echecs;
        Instant debutFenetre;
        Instant bloqueJusqua;
    }

    private final ConcurrentHashMap<String, Compteur> compteurs = new ConcurrentHashMap<>();

    /** Lève une erreur 429 si l'e-mail est actuellement bloqué. */
    public void verifierNonBloque(String email) {
        Compteur c = compteurs.get(cle(email));
        Instant maintenant = Instant.now();
        if (c != null && c.bloqueJusqua != null && maintenant.isBefore(c.bloqueJusqua)) {
            long minutes = Math.max(1, Duration.between(maintenant, c.bloqueJusqua).toMinutes() + 1);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives de connexion. Réessayez dans " + minutes + " minute(s).");
        }
    }

    public void enregistrerEchec(String email) {
        if (compteurs.size() > TAILLE_MAX_TABLE) {
            purger();
        }
        Instant maintenant = Instant.now();
        compteurs.compute(cle(email), (k, c) -> {
            if (c == null || c.debutFenetre == null || maintenant.isAfter(c.debutFenetre.plus(FENETRE))) {
                c = new Compteur();
                c.debutFenetre = maintenant;
            }
            c.echecs++;
            if (c.echecs >= MAX_ECHECS) {
                c.bloqueJusqua = maintenant.plus(BLOCAGE);
                c.echecs = 0;
                c.debutFenetre = maintenant;
            }
            return c;
        });
    }

    public void enregistrerSucces(String email) {
        compteurs.remove(cle(email));
    }

    private void purger() {
        Instant maintenant = Instant.now();
        compteurs.entrySet().removeIf(e -> {
            Compteur c = e.getValue();
            boolean bloque = c.bloqueJusqua != null && maintenant.isBefore(c.bloqueJusqua);
            boolean fenetreActive = c.debutFenetre != null && maintenant.isBefore(c.debutFenetre.plus(FENETRE));
            return !bloque && !fenetreActive;
        });
    }

    private String cle(String email) {
        String e = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return e.length() > 254 ? e.substring(0, 254) : e;
    }
}
