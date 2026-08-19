package com.facturation.service;

import com.facturation.entity.Client;
import com.facturation.entity.Entreprise;
import com.facturation.entity.Facturation;
import com.facturation.entity.LigneFacturation;
import com.facturation.entity.Parametre;
import com.facturation.repository.FacturationRepository;
import com.facturation.repository.LigneFacturationRepository;
import com.facturation.repository.ParametreRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] ENTETES_EXPORT = {
            "Index", "Date de facture", "Numero de facture", "Date d'entree", "Date de sortie",
            "Nom", "Prenom", "Date de naissance", "Tarif journalier", "Dernier jour du mois",
            "Nb de jours presents", "Montant total H.T", "Code TVA", "Montant TVA", "Montant TTC"
    };

    private static final Set<Integer> COLONNES_CENTREES = Set.of(0, 1, 3, 4, 7, 9, 10, 12);
    private static final Set<Integer> COLONNES_MONTANTS = Set.of(8, 11, 13, 14);

    private static final float[] LARGEURS_COLONNES = {
            3f, 6f, 8f, 6f, 6f, 7f, 7f, 6f, 6f, 6f, 4f, 7f, 4f, 7f, 7f
    };

    private final FacturationRepository facturationRepository;
    private final LigneFacturationRepository ligneFacturationRepository;
    private final ParametreRepository parametreRepository;

    // =========================================================================
    // PDF EXPORT
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] exporterFacturePdf(UUID idFacturation) throws Exception {
        Facturation facture = facturationRepository.findById(idFacturation)
                .orElseThrow(() -> new IllegalArgumentException("Facture introuvable : " + idFacturation));

        List<LigneFacturation> lignes = ligneFacturationRepository
                .findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation);
        Parametre parametre = obtenirParametre(facture);
        String codeTva = formatCodeTva(parametre);
        String devise = parametre != null && parametre.getDevise() != null ? parametre.getDevise().name() : "";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 60);
        PdfWriter writer = PdfWriter.getInstance(document, out);

        Entreprise entreprise = facture.getEntreprise();
        String nomAffiche = entreprise != null ? valeurOuVide(entreprise.getNom()) : "";
        String numeroAffiche = facture.getNumeroFacture() != null ? facture.getNumeroFacture() : "BROUILLON";
        String texteBasDePage = nomAffiche + " — Facture " + numeroAffiche;
        
        PiedDePage eventPiedDePage = new PiedDePage(texteBasDePage);
        writer.setPageEvent(eventPiedDePage);

        document.open();

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
        com.lowagie.text.Font entrepriseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        com.lowagie.text.Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(80, 80, 80));
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
        com.lowagie.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
        com.lowagie.text.Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);

        // --- En-tête ---
        PdfPTable enTete = new PdfPTable(2);
        enTete.setWidthPercentage(100);
        enTete.setWidths(new float[]{1f, 1f});

        PdfPCell celluleEntreprise = new PdfPCell();
        celluleEntreprise.setBorder(Rectangle.NO_BORDER);
        if (entreprise != null) {
            celluleEntreprise.addElement(new Paragraph(nomAffiche, entrepriseFont));
            if (entreprise.getAdresse() != null && !entreprise.getAdresse().isBlank()) {
                celluleEntreprise.addElement(new Paragraph(entreprise.getAdresse(), infoFont));
            }
            if (entreprise.getTelephone() != null && !entreprise.getTelephone().isBlank()) {
                celluleEntreprise.addElement(new Paragraph("Tél : " + entreprise.getTelephone(), infoFont));
            }
            if (entreprise.getEmail() != null && !entreprise.getEmail().isBlank()) {
                celluleEntreprise.addElement(new Paragraph(entreprise.getEmail(), infoFont));
            }
        }
        enTete.addCell(celluleEntreprise);

        PdfPCell celluleFacture = new PdfPCell();
        celluleFacture.setBorder(Rectangle.NO_BORDER);
        celluleFacture.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        Paragraph titre = new Paragraph("FACTURE " + numeroAffiche, titleFont);
        titre.setAlignment(Element.ALIGN_RIGHT);
        celluleFacture.addElement(titre);
        
        Paragraph periode = new Paragraph("Période : " + facture.getMois() + "/" + facture.getAnnee(), infoFont);
        periode.setAlignment(Element.ALIGN_RIGHT);
        celluleFacture.addElement(periode);
        
        Paragraph statut = new Paragraph("Statut : " + facture.getStatut(), infoFont);
        statut.setAlignment(Element.ALIGN_RIGHT);
        celluleFacture.addElement(statut);
        
        if (!devise.isBlank()) {
            Paragraph deviseParagraphe = new Paragraph("Devise : " + devise, infoFont);
            deviseParagraphe.setAlignment(Element.ALIGN_RIGHT);
            celluleFacture.addElement(deviseParagraphe);
        }
        enTete.addCell(celluleFacture);

        document.add(enTete);
        document.add(new Paragraph(" "));

        // --- Tableau principal ---
        PdfPTable table = new PdfPTable(ENTETES_EXPORT.length);
        table.setWidthPercentage(100);
        table.setWidths(LARGEURS_COLONNES);

        for (String entete : ENTETES_EXPORT) {
            addCellToHeader(table, entete, headerFont);
        }

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        int index = 1;
        for (LigneFacturation ligne : lignes) {
            String[] valeurs = construireLignePdf(index++, facture, ligne, codeTva);
            for (int i = 0; i < valeurs.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(valeurs[i], bodyFont));
                cell.setPadding(3);
                if (COLONNES_MONTANTS.contains(i)) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                } else if (COLONNES_CENTREES.contains(i)) {
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                } else {
                    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                }
                table.addCell(cell);
            }
            totalHt = totalHt.add(valeurSure(ligne.getMontantHt()));
            totalTva = totalTva.add(valeurSure(ligne.getMontantTva()));
            totalTtc = totalTtc.add(valeurSure(ligne.getMontantTtc()));
        }

        // Totaux
        PdfPCell celluleTotalLabel = new PdfPCell(new Phrase("TOTAL", totalFont));
        celluleTotalLabel.setColspan(11);
        celluleTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celluleTotalLabel.setPadding(4);
        celluleTotalLabel.setBackgroundColor(new Color(235, 235, 235));
        table.addCell(celluleTotalLabel);

        table.addCell(celluleTotal(formatMontantAffichage(totalHt), totalFont));
        table.addCell(celluleTotal("", totalFont));
        table.addCell(celluleTotal(formatMontantAffichage(totalTva), totalFont));
        table.addCell(celluleTotal(formatMontantAffichage(totalTtc), totalFont));

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    private PdfPCell celluleTotal(String texte, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(4);
        cell.setBackgroundColor(new Color(235, 235, 235));
        return cell;
    }

    private static class PiedDePage extends PdfPageEventHelper {
        private final String texteGauche;
        private final com.lowagie.text.Font police = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(120, 120, 120));
        private PdfTemplate totalPages;

        PiedDePage(String texteGauche) {
            this.texteGauche = texteGauche;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPages = writer.getDirectContent().createTemplate(30, 16);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            
            cb.saveState();
            cb.beginText();
            cb.setFontAndSize(police.getBaseFont(), police.getSize());
            
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, texteGauche, document.left(), document.bottom() - 20, 0);
            
            String texteDroit = "Page " + writer.getPageNumber() + " / ";
            float largeur = police.getBaseFont().getWidthPoint(texteDroit, police.getSize());
            float x = document.right() - largeur - 15;
            
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, texteDroit, x, document.bottom() - 20, 0);
            cb.endText();

            cb.addTemplate(totalPages, x + largeur, document.bottom() - 20);
            cb.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPages.beginText();
            totalPages.setFontAndSize(police.getBaseFont(), police.getSize());
            totalPages.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPages.endText();
        }
    }

    // =========================================================================
    // EXCEL EXPORT
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] exporterFactureExcel(UUID idFacturation) throws Exception {
        Facturation facture = facturationRepository.findById(idFacturation)
                .orElseThrow(() -> new IllegalArgumentException("Facture introuvable : " + idFacturation));

        List<LigneFacturation> lignes = ligneFacturationRepository
                .findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation);
        Parametre parametre = obtenirParametre(facture);
        String codeTva = formatCodeTva(parametre);
        String devise = parametre != null && parametre.getDevise() != null ? parametre.getDevise().name() : "";

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Facture");

            org.apache.poi.ss.usermodel.Font gras = workbook.createFont();
            gras.setBold(true);

            CellStyle styleTitre = workbook.createCellStyle();
            styleTitre.setFont(gras);

            CellStyle styleEnTete = workbook.createCellStyle();
            styleEnTete.setFont(gras);
            styleEnTete.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            styleEnTete.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle styleMontant = workbook.createCellStyle();
            styleMontant.setAlignment(HorizontalAlignment.RIGHT);
            styleMontant.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            CellStyle styleMontantTotal = workbook.createCellStyle();
            styleMontantTotal.cloneStyleFrom(styleMontant);
            styleMontantTotal.setFont(gras);

            CellStyle styleCentre = workbook.createCellStyle();
            styleCentre.setAlignment(HorizontalAlignment.CENTER);

            int r = 0;
            org.apache.poi.ss.usermodel.Row ligneEntreprise = sheet.createRow(r++);
            ligneEntreprise.createCell(0).setCellValue("Entreprise :");
            ligneEntreprise.getCell(0).setCellStyle(styleTitre);
            
            Entreprise entreprise = facture.getEntreprise();
            ligneEntreprise.createCell(1).setCellValue(entreprise != null ? valeurOuVide(entreprise.getNom()) : "");

            org.apache.poi.ss.usermodel.Row ligneFacture = sheet.createRow(r++);
            ligneFacture.createCell(0).setCellValue("Facture :");
            ligneFacture.getCell(0).setCellStyle(styleTitre);
            String numeroAffiche = facture.getNumeroFacture() != null ? facture.getNumeroFacture() : "BROUILLON";
            ligneFacture.createCell(1).setCellValue(numeroAffiche + " - Période " + facture.getMois() + "/" + facture.getAnnee() + " - Statut " + facture.getStatut());

            org.apache.poi.ss.usermodel.Row ligneDevise = sheet.createRow(r++);
            ligneDevise.createCell(0).setCellValue("Devise :");
            ligneDevise.getCell(0).setCellStyle(styleTitre);
            ligneDevise.createCell(1).setCellValue(devise);

            r++;

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(r++);
            for (int i = 0; i < ENTETES_EXPORT.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(ENTETES_EXPORT[i]);
                cell.setCellStyle(styleEnTete);
            }

            int ligneDebutDonnees = r;
            int index = 1;
            for (LigneFacturation ligne : lignes) {
                Client client = ligne.getClient();
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);

                setCelluleTexte(row, 0, String.valueOf(index++), styleCentre);
                setCelluleTexte(row, 1, formatDate(ligne.getDateFacture()), styleCentre);
                row.createCell(2).setCellValue(valeurOuVide(numeroAffiche));
                setCelluleTexte(row, 3, formatDate(ligne.getDateEntreeEffective()), styleCentre);
                setCelluleTexte(row, 4, formatDate(ligne.getDateSortieEffective()), styleCentre);
                row.createCell(5).setCellValue(client != null ? valeurOuVide(client.getNom()) : "");
                row.createCell(6).setCellValue(client != null ? valeurOuVide(client.getPrenom()) : "");
                setCelluleTexte(row, 7, client != null ? formatDate(client.getDateNaissance()) : "", styleCentre);
                setCelluleNumerique(row, 8, ligne.getTarifApplique(), styleMontant);
                setCelluleTexte(row, 9, formatDate(ligne.getDernierJourMois()), styleCentre);
                setCelluleTexte(row, 10, ligne.getNbJours() != null ? String.valueOf(ligne.getNbJours()) : "0", styleCentre);
                setCelluleNumerique(row, 11, ligne.getMontantHt(), styleMontant);
                setCelluleTexte(row, 12, codeTva, styleCentre);
                setCelluleNumerique(row, 13, ligne.getMontantTva(), styleMontant);
                setCelluleNumerique(row, 14, ligne.getMontantTtc(), styleMontant);
            }
            int ligneFinDonnees = r - 1;

            if (ligneFinDonnees >= ligneDebutDonnees) {
                r++;
                org.apache.poi.ss.usermodel.Row ligneTotal = sheet.createRow(r++);
                org.apache.poi.ss.usermodel.Cell celluleLabelTotal = ligneTotal.createCell(0);
                celluleLabelTotal.setCellValue("TOTAL");
                celluleLabelTotal.setCellStyle(styleTitre);

                String plageHt = colonneExcel(11) + (ligneDebutDonnees + 1) + ":" + colonneExcel(11) + (ligneFinDonnees + 1);
                String plageTva = colonneExcel(13) + (ligneDebutDonnees + 1) + ":" + colonneExcel(13) + (ligneFinDonnees + 1);
                String plageTtc = colonneExcel(14) + (ligneDebutDonnees + 1) + ":" + colonneExcel(14) + (ligneFinDonnees + 1);

                org.apache.poi.ss.usermodel.Cell celluleTotalHt = ligneTotal.createCell(11);
                celluleTotalHt.setCellFormula("SUM(" + plageHt + ")");
                celluleTotalHt.setCellStyle(styleMontantTotal);

                org.apache.poi.ss.usermodel.Cell celluleTotalTva = ligneTotal.createCell(13);
                celluleTotalTva.setCellFormula("SUM(" + plageTva + ")");
                celluleTotalTva.setCellStyle(styleMontantTotal);

                org.apache.poi.ss.usermodel.Cell celluleTotalTtc = ligneTotal.createCell(14);
                celluleTotalTtc.setCellFormula("SUM(" + plageTtc + ")");
                celluleTotalTtc.setCellStyle(styleMontantTotal);
            }

            for (int i = 0; i < ENTETES_EXPORT.length; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String colonneExcel(int index0Based) {
        StringBuilder sb = new StringBuilder();
        int n = index0Based;
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    // =========================================================================
    // CSV EXPORT
    // =========================================================================

    @Transactional(readOnly = true)
    public byte[] exporterFactureCsv(UUID idFacturation) throws Exception {
        Facturation facture = facturationRepository.findById(idFacturation)
                .orElseThrow(() -> new IllegalArgumentException("Facture introuvable : " + idFacturation));

        List<LigneFacturation> lignes = ligneFacturationRepository
                .findByFacturationIdFacturationOrderByOrdreAffichageAsc(idFacturation);
        Parametre parametre = obtenirParametre(facture);
        String codeTva = formatCodeTva(parametre);

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(ENTETES_EXPORT);

            int index = 1;
            for (LigneFacturation ligne : lignes) {
                writer.writeNext(construireLigneCsv(index++, facture, ligne, codeTva));
                totalHt = totalHt.add(valeurSure(ligne.getMontantHt()));
                totalTva = totalTva.add(valeurSure(ligne.getMontantTva()));
                totalTtc = totalTtc.add(valeurSure(ligne.getMontantTtc()));
            }

            String[] ligneTotal = new String[ENTETES_EXPORT.length];
            java.util.Arrays.fill(ligneTotal, "");
            ligneTotal[0] = "TOTAL";
            ligneTotal[11] = formatMontantCsv(totalHt);
            ligneTotal[13] = formatMontantCsv(totalTva);
            ligneTotal[14] = formatMontantCsv(totalTtc);
            writer.writeNext(ligneTotal);
        }

        return out.toByteArray();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String[] construireLignePdf(int index, Facturation facture, LigneFacturation ligne, String codeTva) {
        Client client = ligne.getClient();
        return new String[]{
                String.valueOf(index),
                formatDate(ligne.getDateFacture()),
                valeurOuVide(facture.getNumeroFacture()),
                formatDate(ligne.getDateEntreeEffective()),
                formatDate(ligne.getDateSortieEffective()),
                client != null ? valeurOuVide(client.getNom()) : "",
                client != null ? valeurOuVide(client.getPrenom()) : "",
                client != null ? formatDate(client.getDateNaissance()) : "",
                formatMontantAffichage(ligne.getTarifApplique()),
                formatDate(ligne.getDernierJourMois()),
                ligne.getNbJours() != null ? String.valueOf(ligne.getNbJours()) : "0",
                formatMontantAffichage(ligne.getMontantHt()),
                codeTva,
                formatMontantAffichage(ligne.getMontantTva()),
                formatMontantAffichage(ligne.getMontantTtc())
        };
    }

    private String[] construireLigneCsv(int index, Facturation facture, LigneFacturation ligne, String codeTva) {
        Client client = ligne.getClient();
        return new String[]{
                String.valueOf(index),
                formatDate(ligne.getDateFacture()),
                valeurOuVide(facture.getNumeroFacture()),
                formatDate(ligne.getDateEntreeEffective()),
                formatDate(ligne.getDateSortieEffective()),
                client != null ? valeurOuVide(client.getNom()) : "",
                client != null ? valeurOuVide(client.getPrenom()) : "",
                client != null ? formatDate(client.getDateNaissance()) : "",
                formatMontantCsv(ligne.getTarifApplique()),
                formatDate(ligne.getDernierJourMois()),
                ligne.getNbJours() != null ? String.valueOf(ligne.getNbJours()) : "0",
                formatMontantCsv(ligne.getMontantHt()),
                codeTva,
                formatMontantCsv(ligne.getMontantTva()),
                formatMontantCsv(ligne.getMontantTtc())
        };
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FR) : "";
    }

    private BigDecimal valeurSure(BigDecimal montant) {
        return montant != null ? montant : BigDecimal.ZERO;
    }

    private String formatMontantAffichage(BigDecimal montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(valeurSure(montant).setScale(2, RoundingMode.HALF_UP));
    }

    private String formatMontantCsv(BigDecimal montant) {
        return valeurSure(montant).setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String valeurOuVide(String valeur) {
        return valeur != null ? valeur : "";
    }

    private void setCelluleTexte(org.apache.poi.ss.usermodel.Row row, int colonne, String valeur, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(colonne);
        cell.setCellValue(valeur);
        cell.setCellStyle(style);
    }

    private void setCelluleNumerique(org.apache.poi.ss.usermodel.Row row, int colonne, Number valeur, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(colonne);
        cell.setCellValue(valeur != null ? valeur.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    private Parametre obtenirParametre(Facturation facture) {
        if (facture.getEntreprise() == null) {
            return null;
        }
        return parametreRepository.findByEntrepriseIdEntreprise(facture.getEntreprise().getIdEntreprise())
                .orElse(null);
    }

    private String formatCodeTva(Parametre parametre) {
        if (parametre == null || parametre.getTauxTva() == null) {
            return "";
        }
        return parametre.getTauxTva().stripTrailingZeros().toPlainString() + "%";
    }

    private void addCellToHeader(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(60, 60, 60));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        table.addCell(cell);
    }
}