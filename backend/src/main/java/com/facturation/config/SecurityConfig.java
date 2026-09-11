package com.facturation.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // Renseignée via la variable d'environnement CORS_ALLOWED_ORIGINS sur Railway.
    // Plusieurs origines séparées par des virgules, ex :
    // https://ton-projet.vercel.app,https://ton-projet-git-main-toncompte.vercel.app
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            // Gestion des utilisateurs : reservee a ADMIN, y compris la simple consultation.
            .requestMatchers("/api/utilisateurs", "/api/utilisateurs/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
            // CONSULTATION est un role lecture seule : toute action d'ecriture (creer, modifier,
            // valider, importer, desactiver, supprimer...) est reservee a ADMIN et COMPTABLE.
            // La lecture (GET : clients, facturations, exports PDF/Excel/CSV, parametres...)
            // reste ouverte a tout utilisateur authentifie via la regle anyRequest() plus bas.
            .requestMatchers(HttpMethod.POST, "/api/**")
                .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_COMPTABLE", "COMPTABLE")
            .requestMatchers(HttpMethod.PUT, "/api/**")
                .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_COMPTABLE", "COMPTABLE")
            .requestMatchers(HttpMethod.PATCH, "/api/**")
                .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_COMPTABLE", "COMPTABLE")
            .requestMatchers(HttpMethod.DELETE, "/api/**")
                .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "ROLE_COMPTABLE", "COMPTABLE")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Id-Entreprise", "X-Id-Utilisateur"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        // Pas de cookies ici : le JWT part en header Authorization (voir api.ts),
        // donc allowCredentials n'apporte rien et complique inutilement la config CORS.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}