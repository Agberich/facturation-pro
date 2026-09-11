package com.facturation.controller;

import com.facturation.entity.Client;
import com.facturation.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/entreprise/{idEntreprise}")
    public ResponseEntity<List<Client>> obtenirTousLesClients(
            @PathVariable UUID idEntreprise,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(clientService.obtenirTousLesClients(idEntreprise, includeInactive));
    }

    @PostMapping("/entreprise/{idEntreprise}")
    public ResponseEntity<Client> ajouterClient(@PathVariable UUID idEntreprise, @RequestBody Client client) {
        Client nouveauClient = clientService.ajouterClient(idEntreprise, client);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouveauClient);
    }

    @PutMapping("/{idClient}")
    public ResponseEntity<Client> modifierClient(@PathVariable UUID idClient, @RequestBody Client client) {
        Client clientModifie = clientService.modifierClient(idClient, client);
        return ResponseEntity.ok(clientModifie);
    }

    @PutMapping("/{idClient}/desactiver")
    public ResponseEntity<Void> desactiverClient(
            @PathVariable UUID idClient,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateSortie) {
        clientService.desactiverClient(idClient, dateSortie);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{idClient}/reactiver")
    public ResponseEntity<Void> reactiverClient(@PathVariable UUID idClient) {
        clientService.reactiverClient(idClient);
        return ResponseEntity.noContent().build();
    }

    // --- ACTIONS EN MASSE (BULK) ---

    @PutMapping("/desactiver")
    public ResponseEntity<Void> desactiverClientsEnMasse(@RequestBody List<UUID> idsClients) {
        clientService.desactiverClientsEnMasse(idsClients);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reactiver")
    public ResponseEntity<Void> reactiverClientsEnMasse(@RequestBody List<UUID> idsClients) {
        clientService.reactiverClientsEnMasse(idsClients);
        return ResponseEntity.noContent().build();
    }
}