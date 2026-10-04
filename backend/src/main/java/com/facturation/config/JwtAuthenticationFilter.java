package com.facturation.config;

import com.facturation.entity.Utilisateur;
import com.facturation.repository.UtilisateurRepository;
import com.facturation.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            String email = jwtService.extractUsername(jwt);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Le jeton seul ne suffit pas : l'utilisateur doit toujours exister et être actif,
                // et son rôle est celui d'aujourd'hui (pas celui d'il y a 12 h).
                Utilisateur utilisateur = utilisateurRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);

                if (utilisateur != null && Boolean.TRUE.equals(utilisateur.getActif())) {
                    List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                    if (utilisateur.getRole() != null) {
                        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // En cas de jeton invalide, l'authentification n'est pas définie
        }

        filterChain.doFilter(request, response);
    }
}