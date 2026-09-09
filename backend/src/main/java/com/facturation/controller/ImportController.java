package com.facturation.controller;

import com.facturation.dto.ImportClientDTO;
import com.facturation.entity.Client;
import com.facturation.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private final ImportService importService;

    /**
     * Aperçu du contenu d'un fichier (xlsx/xls/csv/ods) sans enregistrement en base.
     */
    @PostMapping("/clients/apercu")
    public ResponseEntity<List<ImportClientDTO>> apercuClients(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("Aperçu du fichier d'import : {}", file != null ? file.getOriginalFilename() : "null");
        List<ImportClientDTO> apercu = importService.importerClients(file);
        return ResponseEntity.ok(apercu);
    }

    /**
     * Importe le fichier et enregistre les clients pour l'entreprise du contexte.
     */
    @PostMapping("/clients")
    public ResponseEntity<List<Client>> importerClients(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("Importation et sauvegarde des clients : {}", file != null ? file.getOriginalFilename() : "null");
        List<Client> clientsSauvegardes = importService.importerEtSauvegarderClients(file);
        return ResponseEntity.ok(clientsSauvegardes);
    }
}