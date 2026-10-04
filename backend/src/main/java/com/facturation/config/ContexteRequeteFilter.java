package com.facturation.config;

import com.facturation.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Capture l'entreprise et l'utilisateur de la requête, l'adresse IP et le user-agent,
 * et les rend disponibles via ContexteRequete pendant toute la requête HTTP.
 *
 * SÉCURITÉ : l'entreprise et l'utilisateur sont lus dans le jeton JWT signé par le
 * serveur, JAMAIS dans les en-têtes X-Id-Entreprise / X-Id-Utilisateur (qu'un client
 * peut falsifier pour usurper l'auteur d'une action dans l'historique).
 * Les chemins de la forme /entreprise/{id} sont refusés (403) si l'id n'est pas celui
 * de l'entreprise du jeton.
 */
@Component
@RequiredArgsConstructor
public class ContexteRequeteFilter extends OncePerRequestFilter {

    private static final Pattern CHEMIN_ENTREPRISE = Pattern.compile(
            "/entreprise/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(?:/|$)");
    private static final Pattern ADRESSE_IP_VALIDE = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");
    private static final int USER_AGENT_MAX = 500;

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String idEntreprise = null;
        String idUtilisateur = null;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                // Vérifie la signature et l'expiration : un jeton invalide ne donne aucune identité
                Claims claims = jwtService.extractAllClaims(authHeader.substring(7));
                idEntreprise = claims.get("idEntreprise", String.class);
                idUtilisateur = claims.get("idUtilisateur", String.class);
            } catch (Exception ignored) {
                // jeton invalide ou expiré : Spring Security refusera la requête
            }
        }

        if (idEntreprise != null) {
            Matcher m = CHEMIN_ENTREPRISE.matcher(request.getRequestURI());
            if (m.find() && !m.group(1).equalsIgnoreCase(idEntreprise)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        "{\"status\":403,\"message\":\"Accès refusé : cette entreprise n'est pas la vôtre.\"}");
                return;
            }
        }

        ContexteRequete.setIdEntreprise(idEntreprise);
        ContexteRequete.setIdUtilisateur(idUtilisateur);
        ContexteRequete.setAdresseIp(extraireAdresseIp(request));
        ContexteRequete.setUserAgent(tronquer(cleanHeader(request.getHeader("User-Agent")), USER_AGENT_MAX));
        try {
            chain.doFilter(request, response);
        } finally {
            ContexteRequete.clear();
        }
    }

    /**
     * Derrière le proxy de l'hébergeur, l'adresse du client est la DERNIÈRE entrée de
     * X-Forwarded-For (celle ajoutée par le proxy) : les entrées précédentes sont
     * envoyées par le client et peuvent être falsifiées. Une valeur qui n'a pas la forme
     * d'une adresse IP est ignorée (la colonne de l'historique est limitée à 45 caractères).
     */
    private String extraireAdresseIp(HttpServletRequest request) {
        String forwardedFor = cleanHeader(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            String[] parts = forwardedFor.split(",");
            String candidat = parts[parts.length - 1].trim();
            if (ADRESSE_IP_VALIDE.matcher(candidat).matches()) {
                return candidat;
            }
        }
        return tronquer(request.getRemoteAddr(), 45);
    }

    private String cleanHeader(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private String tronquer(String value, int max) {
        return (value != null && value.length() > max) ? value.substring(0, max) : value;
    }
}
