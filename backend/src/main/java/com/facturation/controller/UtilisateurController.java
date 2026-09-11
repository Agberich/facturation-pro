package com.facturation.controller;

import com.facturation.dto.UtilisateurRequest;
import com.facturation.dto.UtilisateurResponse;
import com.facturation.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping("/entreprise/{idEntreprise}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<UtilisateurResponse>> listerParEntreprise(@PathVariable UUID idEntreprise) {
        return ResponseEntity.ok(utilisateurService.listerParEntreprise(idEntreprise));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UtilisateurResponse> obtenirParId(@PathVariable UUID id) {
        return ResponseEntity.ok(utilisateurService.obtenirParId(id));
    }

    @PostMapping("/entreprise/{idEntreprise}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UtilisateurResponse> creer(
            @PathVariable UUID idEntreprise,
            @Valid @RequestBody UtilisateurRequest request) {
        UtilisateurResponse nouvelUtilisateur = utilisateurService.creer(idEntreprise, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouvelUtilisateur);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UtilisateurResponse> modifier(
            @PathVariable UUID id,
            @Valid @RequestBody UtilisateurRequest request) {
        return ResponseEntity.ok(utilisateurService.modifier(id, request));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> desactiver(@PathVariable UUID id) {
        utilisateurService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactiver")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> reactiver(@PathVariable UUID id) {
        utilisateurService.reactiver(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        utilisateurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}