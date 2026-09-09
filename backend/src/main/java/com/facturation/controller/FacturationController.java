package com.facturation.controller;

import com.facturation.entity.Facturation;
import com.facturation.entity.LigneFacturation;
import com.facturation.service.FacturationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/facturations")
@RequiredArgsConstructor
public class FacturationController {

    private final FacturationService facturationService;

    @GetMapping("/entreprise/{idEntreprise}")
    public ResponseEntity<List<Facturation>> listerFacturations(@PathVariable UUID idEntreprise) {
        return ResponseEntity.ok(facturationService.listerFacturations(idEntreprise));
    }

    @GetMapping("/{idFacturation}")
    public ResponseEntity<Facturation> obtenirFacturation(@PathVariable UUID idFacturation) {
        return ResponseEntity.ok(facturationService.obtenirFacturation(idFacturation));
    }

    @GetMapping("/{idFacturation}/lignes")
    public ResponseEntity<List<LigneFacturation>> listerLignes(@PathVariable UUID idFacturation) {
        return ResponseEntity.ok(facturationService.listerLignes(idFacturation));
    }

    @PostMapping("/entreprise/{idEntreprise}/initialiser")
    public ResponseEntity<Facturation> initialiserMois(
            @PathVariable UUID idEntreprise,
            @RequestParam Integer annee,
            @RequestParam Integer mois) {
        
        Facturation facturation = facturationService.initialiserMoisFacturation(idEntreprise, annee, mois);
        return ResponseEntity.status(HttpStatus.CREATED).body(facturation);
    }

    @PostMapping("/{idFacturation}/generer-lignes")
    public ResponseEntity<List<LigneFacturation>> genererLignes(@PathVariable UUID idFacturation) {
        return ResponseEntity.ok(facturationService.genererLignesPourFacturation(idFacturation));
    }

    @PutMapping("/{idFacturation}/valider")
    public ResponseEntity<Facturation> validerFacture(@PathVariable UUID idFacturation) {
        Facturation facturationValidee = facturationService.validerFacture(idFacturation);
        return ResponseEntity.ok(facturationValidee);
    }

    @PutMapping("/{idFacturation}/reouvrir")
    public ResponseEntity<Facturation> reouvrirFacture(@PathVariable UUID idFacturation) {
        return ResponseEntity.ok(facturationService.reouvrirFacture(idFacturation));
    }
}