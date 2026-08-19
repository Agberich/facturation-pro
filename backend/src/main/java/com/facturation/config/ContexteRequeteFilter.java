package com.facturation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Capture X-Id-Entreprise (entreprise active), X-Id-Utilisateur (auteur de
 * l'action), l'adresse IP et le user-agent du client, et les rend
 * disponibles via ContexteRequete pendant toute la duree de la requete HTTP.
 */
@Component
public class ContexteRequeteFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String idEntreprise = cleanHeader(request.getHeader("X-Id-Entreprise"));
        String idUtilisateur = cleanHeader(request.getHeader("X-Id-Utilisateur"));

        ContexteRequete.setIdEntreprise(idEntreprise);
        ContexteRequete.setIdUtilisateur(idUtilisateur);
        ContexteRequete.setAdresseIp(extraireAdresseIp(request));
        ContexteRequete.setUserAgent(cleanHeader(request.getHeader("User-Agent")));

        try {
            chain.doFilter(request, response);
        } finally {
            ContexteRequete.clear();
        }
    }

    /**
     * Prend en compte un eventuel proxy/reverse-proxy (X-Forwarded-For) avant
     * de retomber sur l'adresse de connexion directe.
     */
    private String extraireAdresseIp(HttpServletRequest request) {
        String forwardedFor = cleanHeader(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            // Le header peut contenir une chaine "client, proxy1, proxy2"
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String cleanHeader(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }
}
