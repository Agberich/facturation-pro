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

    // Couleurs du PDF (alignées sur le bleu de l'application)
    private static final Color BLEU = new Color(23, 92, 211);
    private static final Color BLEU_FONCE = new Color(16, 24, 40);
    private static final Color BLEU_TRES_CLAIR = new Color(239, 248, 255);
    private static final Color BLEU_TOTAL = new Color(209, 233, 255);
    private static final Color VERT_PAYEE = new Color(2, 122, 72);
    private static final Color ORANGE_BROUILLON = new Color(181, 71, 8);
    private static final Color GRIS_ARCHIVE = new Color(102, 112, 133);

    // PDF et Excel : le numéro de facture est déjà dans l'en-tête du document, on ne le répète
    // pas dans le tableau (14 colonnes). Le CSV n'a pas d'en-tête : il garde la colonne.
    private static final String[] ENTETES_PDF_EXCEL = {
            "Index", "Date de facture", "Date d'entree", "Date de sortie",
            "Nom", "Prenom", "Date de naissance", "Tarif journalier TTC", "Dernier jour du mois",
            "Nb de jours presents", "Montant total H.T", "Code TVA", "Montant TVA", "Montant TTC"
    };

    private static final Set<Integer> COLONNES_CENTREES = Set.of(0, 1, 2, 3, 6, 8, 9, 11);
    private static final Set<Integer> COLONNES_MONTANTS = Set.of(7, 10, 12, 13);

    private static final float[] LARGEURS_COLONNES = {
            3f, 6f, 6f, 6f, 7f, 7f, 6f, 8f, 6f, 4f, 8f, 4f, 8f, 8f
    };

    // Espace insécable : empêche « 7 192,80 € » de se couper sur deux lignes dans une cellule.
    private static final char ESPACE_INSECABLE = '\u00A0';

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
        String symbole = symboleDevise(parametre);

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

        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BLEU);
        com.lowagie.text.Font entrepriseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_FONCE);
        com.lowagie.text.Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(80, 80, 80));
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
        com.lowagie.text.Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
        com.lowagie.text.Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BLEU_FONCE);

        // --- En-tête ---
        PdfPTable enTete = new PdfPTable(2);
        enTete.setWidthPercentage(100);
        enTete.setWidths(new float[]{1f, 1f});

        PdfPCell celluleEntreprise = new PdfPCell();
        celluleEntreprise.setBorder(Rectangle.NO_BORDER);
        com.lowagie.text.Image logo = chargerImage(parametre != null ? parametre.getLogo() : null);
        if (logo != null) {
            logo.scaleToFit(130, 55);
            celluleEntreprise.addElement(logo);
        }
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
        if (parametre != null) {
            if (estRenseigne(parametre.getFournisseurSiret())) {
                celluleEntreprise.addElement(new Paragraph("SIRET : " + parametre.getFournisseurSiret(), infoFont));
            }
            if (estRenseigne(parametre.getDirectionTerritoriale())) {
                celluleEntreprise.addElement(new Paragraph(parametre.getDirectionTerritoriale(), infoFont));
            }
        }
        enTete.addCell(celluleEntreprise);

        PdfPCell celluleFacture = new PdfPCell();
        celluleFacture.setBorder(Rectangle.NO_BORDER);
        celluleFacture.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        Paragraph titre = new Paragraph("FACTURE " + numeroAffiche, titleFont);
        titre.setAlignment(Element.ALIGN_RIGHT);
        celluleFacture.addElement(titre);
        
        String textePeriode = (facture.getDateDebutPeriode() != null && facture.getDateFinPeriode() != null)
                ? "Période de facturation : du " + formatDate(facture.getDateDebutPeriode())
                        + " au " + formatDate(facture.getDateFinPeriode())
                : "Période : " + facture.getMois() + "/" + facture.getAnnee();
        Paragraph periode = new Paragraph(textePeriode, infoFont);
        periode.setAlignment(Element.ALIGN_RIGHT);
        celluleFacture.addElement(periode);

        if (parametre != null && parametre.getTarifJournalier() != null) {
            Paragraph prixJournee = new Paragraph(
                    "Prix de journée TTC : " + formatMontantAvecDevise(parametre.getTarifJournalier(), symbole), infoFont);
            prixJournee.setAlignment(Element.ALIGN_RIGHT);
            celluleFacture.addElement(prixJournee);
        }
        
        Paragraph statut = new Paragraph();
        statut.add(new Chunk("Statut : ", infoFont));
        statut.add(new Chunk(libelleStatut(facture.getStatut()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, couleurStatut(facture.getStatut()))));
        statut.setAlignment(Element.ALIGN_RIGHT);
        celluleFacture.addElement(statut);

        if (facture.getStatut() == Facturation.StatutFacturation.PAYEE && facture.getDatePaiement() != null) {
            Paragraph paiement = new Paragraph(
                    "Payée le : " + facture.getDatePaiement().toLocalDate().format(DATE_FR), infoFont);
            paiement.setAlignment(Element.ALIGN_RIGHT);
            celluleFacture.addElement(paiement);
        }
        
        if (!devise.isBlank()) {
            Paragraph deviseParagraphe = new Paragraph("Devise : " + devise, infoFont);
            deviseParagraphe.setAlignment(Element.ALIGN_RIGHT);
            celluleFacture.addElement(deviseParagraphe);
        }
        enTete.addCell(celluleFacture);

        document.add(enTete);
        document.add(new Paragraph(" "));

        // --- Blocs « Prestation » et « Facturé à » (modèle FACTURATION_2026.xlsx) ---
        com.lowagie.text.Font blocTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BLEU);
        com.lowagie.text.Font blocTexte = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(60, 60, 60));
        com.lowagie.text.Font blocGras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BLEU_FONCE);

        List<String> lignesPrestation = new java.util.ArrayList<>();
        if (parametre != null) {
            ajouterSiRenseigne(lignesPrestation, "Dispositif", parametre.getDispositif());
            ajouterSiRenseigne(lignesPrestation, "Type de prestation", parametre.getTypePrestation());
            ajouterSiRenseigne(lignesPrestation, "Catégorie d'établissement", parametre.getCategorieEtablissement());
            ajouterSiRenseigne(lignesPrestation, "Discipline", parametre.getDiscipline());
            ajouterSiRenseigne(lignesPrestation, "Mode de fonctionnement", parametre.getModeFonctionnement());
            ajouterSiRenseigne(lignesPrestation, "Public", parametre.getPublicAccueilli());
            if (parametre.getCapacite() != null) {
                lignesPrestation.add("Capacité : " + parametre.getCapacite());
            }
            ajouterSiRenseigne(lignesPrestation, "Centre de profit", parametre.getCentreProfit());
        }
        lignesPrestation.add("Exercice : " + facture.getAnnee());
        if (facture.getMois() != null && facture.getMois() >= 1 && facture.getMois() <= 12) {
            String nomMois = java.time.Month.of(facture.getMois())
                    .getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH);
            lignesPrestation.add("Période d'exécution : " + nomMois + " " + facture.getAnnee());
            // Trimestre de la prestation : le trimestre 1 commence au « premier mois de la prestation »
            // (avril par défaut) -> avr-juin = T1, juil-sept = T2, oct-déc = T3, janv-mars = T4.
            int premierMois = (parametre != null && parametre.getPremierMoisPrestation() != null)
                    ? parametre.getPremierMoisPrestation() : 4;
            int trimestre = ((facture.getMois() - premierMois + 12) % 12) / 3 + 1;
            lignesPrestation.add("Trimestre de la prestation : Trimestre " + trimestre);
        }

        List<String> lignesFinanceur = new java.util.ArrayList<>();
        if (parametre != null) {
            if (estRenseigne(parametre.getFinanceurService())) {
                lignesFinanceur.add(parametre.getFinanceurService());
            }
            if (estRenseigne(parametre.getFinanceurAdresse())) {
                lignesFinanceur.add(parametre.getFinanceurAdresse());
            }
            ajouterSiRenseigne(lignesFinanceur, "E-mail", parametre.getFinanceurEmail());
            ajouterSiRenseigne(lignesFinanceur, "SIRET", parametre.getFinanceurSiret());
            ajouterSiRenseigne(lignesFinanceur, "N° d'engagement", parametre.getNumeroEngagement());
        }
        boolean aUnFinanceur = parametre != null && estRenseigne(parametre.getFinanceurNom());

        PdfPTable blocs = new PdfPTable((aUnFinanceur || !lignesFinanceur.isEmpty()) ? 2 : 1);
        blocs.setWidthPercentage(100);
        blocs.addCell(celluleBloc("PRESTATION", null, lignesPrestation, blocTitre, blocGras, blocTexte));
        if (aUnFinanceur || !lignesFinanceur.isEmpty()) {
            blocs.addCell(celluleBloc("FACTURÉ À",
                    aUnFinanceur ? parametre.getFinanceurNom() : null, lignesFinanceur, blocTitre, blocGras, blocTexte));
        }
        document.add(blocs);
        document.add(new Paragraph(" "));

        // --- Tableau principal ---
        PdfPTable table = new PdfPTable(ENTETES_PDF_EXCEL.length);
        table.setWidthPercentage(100);
        table.setWidths(LARGEURS_COLONNES);

        for (String entete : ENTETES_PDF_EXCEL) {
            addCellToHeader(table, entete, headerFont);
        }

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        int index = 1;
        for (LigneFacturation ligne : lignes) {
            boolean ligneAlternee = index % 2 == 0;
            String[] valeurs = construireLignePdf(index++, ligne, codeTva, symbole);
            for (int i = 0; i < valeurs.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(valeurs[i], bodyFont));
                cell.setPadding(3);
                cell.setBorderColor(BLEU_TOTAL);
                if (ligneAlternee) {
                    cell.setBackgroundColor(BLEU_TRES_CLAIR);
                }
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
        celluleTotalLabel.setColspan(10);
        celluleTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celluleTotalLabel.setPadding(4);
        celluleTotalLabel.setBackgroundColor(BLEU_TOTAL);
        table.addCell(celluleTotalLabel);

        table.addCell(celluleTotal(formatMontantAvecDevise(totalHt, symbole), totalFont));
        table.addCell(celluleTotal("", totalFont));
        table.addCell(celluleTotal(formatMontantAvecDevise(totalTva, symbole), totalFont));
        table.addCell(celluleTotal(formatMontantAvecDevise(totalTtc, symbole), totalFont));

        document.add(table);

        // --- Pied de facture : contacts, références bancaires, signature ---
        List<String> lignesContacts = new java.util.ArrayList<>();
        List<String> lignesBanque = new java.util.ArrayList<>();
        com.lowagie.text.Image signature = null;
        if (parametre != null) {
            ajouterSiRenseigne(lignesContacts, "UT", parametre.getContactUt());
            if (estRenseigne(parametre.getInterlocuteur())) {
                String fonction = estRenseigne(parametre.getFonctionInterlocuteur())
                        ? " (" + parametre.getFonctionInterlocuteur() + ")" : "";
                lignesContacts.add("Interlocuteur : " + parametre.getInterlocuteur() + fonction);
            }
            ajouterSiRenseigne(lignesContacts, "Contact", parametre.getContactDispositif());
            if (estRenseigne(parametre.getIban())) {
                if (estRenseigne(parametre.getMentionReglement())) {
                    lignesBanque.add(parametre.getMentionReglement());
                }
                lignesBanque.add("IBAN : " + formaterIban(parametre.getIban()));
            }
            signature = chargerImage(parametre.getSignatureUrl());
        }

        int nbColonnesPied = (lignesContacts.isEmpty() ? 0 : 1) + (lignesBanque.isEmpty() ? 0 : 1) + (signature != null ? 1 : 0);
        if (nbColonnesPied > 0) {
            document.add(new Paragraph(" "));
            PdfPTable pied = new PdfPTable(nbColonnesPied);
            pied.setWidthPercentage(100);
            pied.setKeepTogether(true);
            if (!lignesContacts.isEmpty()) {
                pied.addCell(celluleBloc("CONTACTS", null, lignesContacts, blocTitre, blocGras, blocTexte));
            }
            if (!lignesBanque.isEmpty()) {
                pied.addCell(celluleBloc("RÉFÉRENCES BANCAIRES", null, lignesBanque, blocTitre, blocGras, blocTexte));
            }
            if (signature != null) {
                signature.scaleToFit(150, 65);
                PdfPCell celluleSignature = new PdfPCell();
                celluleSignature.setBorderColor(BLEU_TOTAL);
                celluleSignature.setPadding(6);
                celluleSignature.addElement(new Paragraph("SIGNATURE", blocTitre));
                celluleSignature.addElement(signature);
                pied.addCell(celluleSignature);
            }
            document.add(pied);
        }

        document.close();

        return out.toByteArray();
    }

    private boolean estRenseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private void ajouterSiRenseigne(List<String> lignes, String libelle, String valeur) {
        if (estRenseigne(valeur)) {
            lignes.add(libelle + " : " + valeur);
        }
    }

    /** Bloc encadré : titre bleu, éventuelle première ligne en gras (nom), puis les lignes de texte. */
    private PdfPCell celluleBloc(String titre, String ligneGras, List<String> lignes,
                                 com.lowagie.text.Font fontTitre, com.lowagie.text.Font fontGras,
                                 com.lowagie.text.Font fontTexte) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BLEU_TOTAL);
        cell.setBackgroundColor(BLEU_TRES_CLAIR);
        cell.setPadding(6);
        cell.addElement(new Paragraph(titre, fontTitre));
        if (estRenseigne(ligneGras)) {
            cell.addElement(new Paragraph(ligneGras, fontGras));
        }
        for (String ligne : lignes) {
            cell.addElement(new Paragraph(ligne, fontTexte));
        }
        return cell;
    }

    /** IBAN affiché par groupes de 4 caractères, ex. « FR76 3000 6000 ». */
    private String formaterIban(String iban) {
        String compact = iban.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return compact.replaceAll("(.{4})(?!$)", "$1 ");
    }

    /**
     * Décode le logo ou la signature enregistrés dans les paramètres (adresse « data: » base64).
     * Une image absente ou illisible ne doit jamais empêcher d'imprimer la facture : on renvoie null.
     */
    private com.lowagie.text.Image chargerImage(String adresseData) {
        if (adresseData == null || adresseData.isBlank()) {
            return null;
        }
        try {
            int virgule = adresseData.indexOf(',');
            if (!adresseData.startsWith("data:image/") || virgule < 0) {
                return null;
            }
            byte[] octets = java.util.Base64.getDecoder().decode(adresseData.substring(virgule + 1));
            return com.lowagie.text.Image.getInstance(octets);
        } catch (Exception e) {
            return null;
        }
    }

    private PdfPCell celluleTotal(String texte, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(4);
        cell.setBackgroundColor(BLEU_TOTAL);
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
        String symbole = symboleDevise(parametre);

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
            // Cellules numériques (les SUM restent valides) avec le symbole de la devise des paramètres
            styleMontant.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00 \"" + symbole + "\""));

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
            ligneFacture.createCell(1).setCellValue(numeroAffiche + " - Période " + facture.getMois() + "/" + facture.getAnnee() + " - Statut " + libelleStatut(facture.getStatut()));

            org.apache.poi.ss.usermodel.Row ligneDevise = sheet.createRow(r++);
            ligneDevise.createCell(0).setCellValue("Devise :");
            ligneDevise.getCell(0).setCellStyle(styleTitre);
            ligneDevise.createCell(1).setCellValue(devise);

            r++;

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(r++);
            for (int i = 0; i < ENTETES_PDF_EXCEL.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(ENTETES_PDF_EXCEL[i]);
                cell.setCellStyle(styleEnTete);
            }

            int ligneDebutDonnees = r;
            int index = 1;
            for (LigneFacturation ligne : lignes) {
                Client client = ligne.getClient();
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);

                setCelluleTexte(row, 0, String.valueOf(index++), styleCentre);
                setCelluleTexte(row, 1, formatDate(ligne.getDateFacture()), styleCentre);
                setCelluleTexte(row, 2, formatDate(ligne.getDateEntreeEffective()), styleCentre);
                setCelluleTexte(row, 3, formatDate(ligne.getDateSortieEffective()), styleCentre);
                row.createCell(4).setCellValue(client != null ? valeurOuVide(client.getNom()) : "");
                row.createCell(5).setCellValue(client != null ? valeurOuVide(client.getPrenom()) : "");
                setCelluleTexte(row, 6, client != null ? formatDate(client.getDateNaissance()) : "", styleCentre);
                setCelluleNumerique(row, 7, ligne.getTarifApplique(), styleMontant);
                setCelluleTexte(row, 8, formatDate(ligne.getDernierJourMois()), styleCentre);
                setCelluleTexte(row, 9, ligne.getNbJours() != null ? String.valueOf(ligne.getNbJours()) : "0", styleCentre);
                setCelluleNumerique(row, 10, ligne.getMontantHt(), styleMontant);
                setCelluleTexte(row, 11, codeTva, styleCentre);
                setCelluleNumerique(row, 12, ligne.getMontantTva(), styleMontant);
                setCelluleNumerique(row, 13, ligne.getMontantTtc(), styleMontant);
            }
            int ligneFinDonnees = r - 1;

            if (ligneFinDonnees >= ligneDebutDonnees) {
                r++;
                org.apache.poi.ss.usermodel.Row ligneTotal = sheet.createRow(r++);
                org.apache.poi.ss.usermodel.Cell celluleLabelTotal = ligneTotal.createCell(0);
                celluleLabelTotal.setCellValue("TOTAL");
                celluleLabelTotal.setCellStyle(styleTitre);

                String plageHt = colonneExcel(10) + (ligneDebutDonnees + 1) + ":" + colonneExcel(10) + (ligneFinDonnees + 1);
                String plageTva = colonneExcel(12) + (ligneDebutDonnees + 1) + ":" + colonneExcel(12) + (ligneFinDonnees + 1);
                String plageTtc = colonneExcel(13) + (ligneDebutDonnees + 1) + ":" + colonneExcel(13) + (ligneFinDonnees + 1);

                org.apache.poi.ss.usermodel.Cell celluleTotalHt = ligneTotal.createCell(10);
                celluleTotalHt.setCellFormula("SUM(" + plageHt + ")");
                celluleTotalHt.setCellStyle(styleMontantTotal);

                org.apache.poi.ss.usermodel.Cell celluleTotalTva = ligneTotal.createCell(12);
                celluleTotalTva.setCellFormula("SUM(" + plageTva + ")");
                celluleTotalTva.setCellStyle(styleMontantTotal);

                org.apache.poi.ss.usermodel.Cell celluleTotalTtc = ligneTotal.createCell(13);
                celluleTotalTtc.setCellFormula("SUM(" + plageTtc + ")");
                celluleTotalTtc.setCellStyle(styleMontantTotal);
            }

            for (int i = 0; i < ENTETES_PDF_EXCEL.length; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            // Les totaux sont des formules SUM. POI ne les calcule pas : sans valeur enregistrée dans
            // le fichier, la ligne TOTAL paraît vide dans l'aperçu, en mode protégé (fichier
            // téléchargé), sur téléphone ou dans un navigateur. On calcule donc les formules
            // maintenant (la valeur est écrite dans le fichier) et on demande aussi à Excel de
            // recalculer à l'ouverture. Les formules restent vivantes si on modifie des lignes.
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.setForceFormulaRecalculation(true);

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
        String codeDevise = parametre != null && parametre.getDevise() != null ? parametre.getDevise().name() : "EUR";

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;
        BigDecimal totalTtc = BigDecimal.ZERO;

        // Les montants restent des nombres (le CSV doit rester exploitable) ; la devise est
        // indiquée dans le titre des colonnes concernées.
        String[] entetesCsv = ENTETES_EXPORT.clone();
        entetesCsv[8] = "Tarif journalier TTC (" + codeDevise + ")";
        entetesCsv[11] = "Montant total H.T (" + codeDevise + ")";
        entetesCsv[13] = "Montant TVA (" + codeDevise + ")";
        entetesCsv[14] = "Montant TTC (" + codeDevise + ")";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.writeNext(entetesCsv);

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

    private String libelleStatut(Facturation.StatutFacturation statut) {
        if (statut == null) {
            return "";
        }
        return switch (statut) {
            case BROUILLON -> "Brouillon";
            case VALIDEE -> "Validée";
            case PAYEE -> "Payée";
            case ARCHIVE -> "Archivée";
        };
    }

    private Color couleurStatut(Facturation.StatutFacturation statut) {
        if (statut == null) {
            return GRIS_ARCHIVE;
        }
        return switch (statut) {
            case BROUILLON -> ORANGE_BROUILLON;
            case VALIDEE -> BLEU;
            case PAYEE -> VERT_PAYEE;
            case ARCHIVE -> GRIS_ARCHIVE;
        };
    }

    private String[] construireLignePdf(int index, LigneFacturation ligne, String codeTva, String symbole) {
        Client client = ligne.getClient();
        return new String[]{
                String.valueOf(index),
                formatDate(ligne.getDateFacture()),
                formatDate(ligne.getDateEntreeEffective()),
                formatDate(ligne.getDateSortieEffective()),
                client != null ? valeurOuVide(client.getNom()) : "",
                client != null ? valeurOuVide(client.getPrenom()) : "",
                client != null ? formatDate(client.getDateNaissance()) : "",
                formatMontantAvecDevise(ligne.getTarifApplique(), symbole),
                formatDate(ligne.getDernierJourMois()),
                ligne.getNbJours() != null ? String.valueOf(ligne.getNbJours()) : "0",
                formatMontantAvecDevise(ligne.getMontantHt(), symbole),
                codeTva,
                formatMontantAvecDevise(ligne.getMontantTva(), symbole),
                formatMontantAvecDevise(ligne.getMontantTtc(), symbole)
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
                client != null ? neutraliserFormule(client.getNom()) : "",
                client != null ? neutraliserFormule(client.getPrenom()) : "",
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

    /**
     * Un texte qui commence par = + - @ (ou tabulation / retour chariot) est interprété comme une
     * formule quand le CSV est ouvert dans Excel. On le neutralise avec une apostrophe en tête.
     * (Dans le fichier Excel .xlsx, ces textes sont stockés comme du texte et ne sont pas évalués.)
     */
    private String neutraliserFormule(String valeur) {
        if (valeur == null || valeur.isEmpty()) {
            return "";
        }
        char c = valeur.charAt(0);
        if (c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r') {
            return "'" + valeur;
        }
        return valeur;
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

    /** Montant suivi du symbole de la devise des paramètres, ex. « 7 192,80 € ». */
    private String formatMontantAvecDevise(BigDecimal montant, String symbole) {
        return formatMontantAffichage(montant).replace(' ', ESPACE_INSECABLE) + ESPACE_INSECABLE + symbole;
    }

    /** Symbole de la devise choisie dans les paramètres (EUR par défaut). */
    private String symboleDevise(Parametre parametre) {
        if (parametre == null || parametre.getDevise() == null) {
            return "€";
        }
        return switch (parametre.getDevise()) {
            case EUR -> "€";
            case USD -> "$";
            case XOF -> "F CFA";
        };
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
        cell.setBackgroundColor(BLEU);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        table.addCell(cell);
    }
}