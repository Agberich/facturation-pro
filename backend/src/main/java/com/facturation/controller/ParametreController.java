package com.facturation.controller;

import com.facturation.dto.ParametreDTO;
import com.facturation.service.ParametreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parametres")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ParametreController {

    private final ParametreService parametreService;

    @GetMapping("/entreprise/{idEntreprise}")
    public ResponseEntity<ParametreDTO> obtenirParametres(@PathVariable UUID idEntreprise) {
        return ResponseEntity.ok(parametreService.obtenirParametres(idEntreprise));
    }

    @PutMapping("/entreprise/{idEntreprise}")
    public ResponseEntity<ParametreDTO> mettreAJourParametres(
            @PathVariable UUID idEntreprise,
            @RequestBody ParametreDTO parametre) {
        return ResponseEntity.ok(parametreService.mettreAJourParametres(idEntreprise, parametre));
    }
}
