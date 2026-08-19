package com.facturation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.facturation.config.JwtService;
import com.facturation.dto.LoginRequestDTO;
import com.facturation.dto.LoginResponseDTO;
import com.facturation.entity.Utilisateur;
import com.facturation.repository.UtilisateurRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (utilisateur == null || !passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasseHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects");
        }

        String roleStr = utilisateur.getRole() != null ? utilisateur.getRole().name() : null;

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", roleStr);
        claims.put("idEntreprise", utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getIdEntreprise() : null);

        String token = jwtService.generateToken(utilisateur.getEmail(), claims);

        LoginResponseDTO response = LoginResponseDTO.builder()
                .token(token)
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .email(utilisateur.getEmail())
                .role(roleStr)
                .idEntreprise(utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getIdEntreprise() : null)
                .build();

        return ResponseEntity.ok(response);
    }
}