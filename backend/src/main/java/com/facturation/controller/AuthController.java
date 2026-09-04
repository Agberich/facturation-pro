package com.facturation.controller;

import com.facturation.dto.*;
import com.facturation.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/premier-admin/existe")
    public ResponseEntity<PremierAdminExisteResponseDTO> verifierPremierAdminExiste() {
        return ResponseEntity.ok(authService.verifierPremierAdminExiste());
    }

    @PostMapping("/premier-admin")
    public ResponseEntity<LoginResponseDTO> creerPremierAdmin(@RequestBody PremierAdminRequestDTO request) {
        return ResponseEntity.ok(authService.creerPremierAdmin(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/changer-mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(
            @RequestBody ChangerMotDePasseRequestDTO request,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        authService.changerMotDePasse(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }
}