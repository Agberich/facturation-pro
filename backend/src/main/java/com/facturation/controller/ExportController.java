package com.facturation.controller;

import com.facturation.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/factures/{id}/pdf")
    public ResponseEntity<byte[]> telechargerPdf(@PathVariable UUID id) throws Exception {
        byte[] pdfContent = exportService.exporterFacturePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facture_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    @GetMapping("/factures/{id}/excel")
    public ResponseEntity<byte[]> telechargerExcel(@PathVariable UUID id) throws Exception {
        byte[] excelContent = exportService.exporterFactureExcel(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facture_" + id + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    @GetMapping("/factures/{id}/csv")
    public ResponseEntity<byte[]> telechargerCsv(@PathVariable UUID id) throws Exception {
        byte[] csvContent = exportService.exporterFactureCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facture_" + id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvContent);
    }
}