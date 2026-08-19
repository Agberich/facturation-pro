package com.facturation.service;

import com.facturation.config.ContexteRequete;
import com.facturation.dto.ImportClientDTO;
import com.facturation.entity.Client;
import com.facturation.entity.Entreprise;
import com.facturation.repository.ClientRepository;
import com.facturation.repository.EntrepriseRepository;
import com.github.miachm.sods.SpreadSheet;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    // Mots-clés pour le mapping tolérant des en-têtes
    private static final List<String> NOM_KEYWORDS = List.of("nom", "résident", "resident", "client", "bénéficiaire", "beneficiaire");
    private static final List<String> PRENOM_KEYWORDS = List.of("prenom", "prénom");
    private static final List<String> NAISSANCE_KEYWORDS = List.of("naiss", "birth", "naissance");
    private static final List<String> ENTREE_KEYWORDS = List.of("entree", "entrée", "arrivee", "arrivée", "début", "debut");
    private static final List<String> SORTIE_KEYWORDS = List.of("sortie", "depart", "départ", "fin");
    private static final List<String> TARIF_KEYWORDS = List.of("tarif", "prix", "montant");
    private static final List<String> COMMENTAIRE_KEYWORDS = List.of("comment", "note", "obs");

    private final ClientRepository clientRepository;
    private final EntrepriseRepository entrepriseRepository;

    @Transactional
    public List<Client> importerEtSauvegarderClients(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier fourni est vide ou invalide.");
        }

        List<ImportClientDTO> dtos = importerClients(file);

        UUID idEntreprise = ContexteRequete.getIdEntreprise();
        if (idEntreprise == null) {
            throw new IllegalStateException("Aucune entreprise sélectionnée dans le contexte de la requête (en-tête X-Id-Entreprise manquant).");
        }

        Entreprise entreprise = entrepriseRepository.findById(idEntreprise)
                .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable : " + idEntreprise));

        List<Client> clientsAEnregistrer = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            ImportClientDTO dto = dtos.get(i);
            if (dto.getNom() == null || dto.getNom().isBlank()) {
                continue;
            }

            LocalDate dateEntree = parseDate(dto.getDateEntree());
            if (dateEntree == null) {
                dateEntree = LocalDate.now();
            }

            LocalDate dateSortie = parseDate(dto.getDateSortie());

            // Contrôle métier
            if (dateSortie != null && dateSortie.isBefore(dateEntree)) {
                throw new IllegalArgumentException(String.format(
                        "Ligne %d : La date de sortie (%s) ne peut pas être antérieure à la date d'entrée (%s) pour le client %s %s.",
                        i + 2, dto.getDateSortie(), dto.getDateEntree(), dto.getNom(), dto.getPrenom()));
            }

            Client client = Client.builder()
                    .entreprise(entreprise)
                    .nom(dto.getNom())
                    .prenom(dto.getPrenom())
                    .dateNaissance(parseDate(dto.getDateNaissance()))
                    .dateEntree(dateEntree)
                    .dateSortie(dateSortie)
                    .tarifParDefaut(parseMontant(dto.getTarifParDefaut()))
                    .commentaire(dto.getCommentaire())
                    .actif(true)
                    .build();

            clientsAEnregistrer.add(client);
        }

        List<Client> clientsSauvegardes = clientRepository.saveAll(clientsAEnregistrer);
        log.info("{} clients ont été importés et enregistrés pour l'entreprise ID: {}", clientsSauvegardes.size(), idEntreprise);
        return clientsSauvegardes;
    }

    public List<ImportClientDTO> importerClients(MultipartFile file) throws Exception {
        if (file == null || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Fichier invalide ou nom de fichier manquant");
        }

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Format de fichier invalide");
        }

        switch (extension.toLowerCase()) {
            case "xlsx":
            case "xls":
                return parseExcel(file.getInputStream());
            case "csv":
                return parseCsv(file.getInputStream());
            case "ods":
                return parseOds(file.getInputStream());
            default:
                throw new IllegalArgumentException("Format non supporté (.xlsx, .xls, .csv, .ods uniquement)");
        }
    }

    // --- MAPPAGE DYNAMIQUE DES EN-TÊTES ---

    private Map<String, Integer> cartographierEnTetes(List<String> enTetes) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < enTetes.size(); i++) {
            if (enTetes.get(i) == null) continue;
            String header = enTetes.get(i).trim().toLowerCase();

            if (containsAny(header, NOM_KEYWORDS) && !header.contains("prenom") && !header.contains("prénom") && !map.containsKey("nom")) {
                map.put("nom", i);
            } else if (containsAny(header, PRENOM_KEYWORDS) && !map.containsKey("prenom")) {
                map.put("prenom", i);
            } else if (containsAny(header, NAISSANCE_KEYWORDS) && !map.containsKey("dateNaissance")) {
                map.put("dateNaissance", i);
            } else if (containsAny(header, SORTIE_KEYWORDS) && !map.containsKey("dateSortie")) {
                map.put("dateSortie", i);
            } else if (containsAny(header, ENTREE_KEYWORDS) && !map.containsKey("dateEntree")) {
                map.put("dateEntree", i);
            } else if (containsAny(header, TARIF_KEYWORDS) && !map.containsKey("tarifParDefaut")) {
                map.put("tarifParDefaut", i);
            } else if (containsAny(header, COMMENTAIRE_KEYWORDS) && !map.containsKey("commentaire")) {
                map.put("commentaire", i);
            }
        }
        return map;
    }

    private String getValeurParChamp(List<String> ligne, Map<String, Integer> map, String champ) {
        Integer index = map.get(champ);
        if (index != null && index < ligne.size() && ligne.get(index) != null) {
            return ligne.get(index).trim();
        }
        return "";
    }

    // --- PARSERS DYNAMIQUES & TOLÉRANTS ---

    private List<ImportClientDTO> parseExcel(InputStream is) throws Exception {
        List<ImportClientDTO> clients = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            // Parcours de TOUTES les feuilles
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sheet.getLastRowNum() < 0) continue;

                // Recherche dynamique de la ligne d'en-tête (20 premières lignes)
                int headerRowIndex = -1;
                Map<String, Integer> colMap = new HashMap<>();

                for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 20); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    List<String> candidateHeaders = new ArrayList<>();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        candidateHeaders.add(getCellValue(row.getCell(c)));
                    }

                    Map<String, Integer> potentialMap = cartographierEnTetes(candidateHeaders);
                    if (potentialMap.containsKey("nom")) {
                        headerRowIndex = r;
                        colMap = potentialMap;
                        break;
                    }
                }

                if (headerRowIndex == -1) continue; // Pas d'en-tête trouvé sur cette feuille

                int emptyRowCount = 0;
                for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (isExcelRowEmpty(row)) {
                        emptyRowCount++;
                        if (emptyRowCount >= 3) break; // Arrêt après 3 lignes vides
                        continue;
                    }
                    emptyRowCount = 0;

                    List<String> rowValues = new ArrayList<>();
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        rowValues.add(getCellValue(row.getCell(j)));
                    }

                    ImportClientDTO dto = buildDtoFromRow(rowValues, colMap);
                    if (dto.getNom() != null && !dto.getNom().isBlank()) {
                        clients.add(dto);
                    }
                }
            }
        }
        return clients;
    }

    private List<ImportClientDTO> parseCsv(InputStream is) throws Exception {
        List<ImportClientDTO> clients = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            List<String[]> lines = reader.readAll();
            if (lines.isEmpty()) return clients;

            int headerRowIndex = -1;
            Map<String, Integer> colMap = new HashMap<>();

            for (int r = 0; r < Math.min(lines.size(), 20); r++) {
                List<String> candidate = Arrays.asList(lines.get(r));
                Map<String, Integer> potentialMap = cartographierEnTetes(candidate);
                if (potentialMap.containsKey("nom")) {
                    headerRowIndex = r;
                    colMap = potentialMap;
                    break;
                }
            }

            if (headerRowIndex == -1) return clients;

            int emptyRowCount = 0;
            for (int i = headerRowIndex + 1; i < lines.size(); i++) {
                List<String> rowValues = Arrays.asList(lines.get(i));
                if (isStringListEmpty(rowValues)) {
                    emptyRowCount++;
                    if (emptyRowCount >= 3) break;
                    continue;
                }
                emptyRowCount = 0;

                ImportClientDTO dto = buildDtoFromRow(rowValues, colMap);
                if (dto.getNom() != null && !dto.getNom().isBlank()) {
                    clients.add(dto);
                }
            }
        }
        return clients;
    }

    private List<ImportClientDTO> parseOds(InputStream is) throws Exception {
        List<ImportClientDTO> clients = new ArrayList<>();
        SpreadSheet spreadSheet = new SpreadSheet(is);

        for (int s = 0; s < spreadSheet.getNumSheets(); s++) {
            com.github.miachm.sods.Sheet sheet = spreadSheet.getSheet(s);
            if (sheet.getMaxRows() <= 0) continue;

            int headerRowIndex = -1;
            Map<String, Integer> colMap = new HashMap<>();

            for (int r = 0; r < Math.min(sheet.getMaxRows(), 20); r++) {
                List<String> candidateHeaders = new ArrayList<>();
                for (int j = 0; j < sheet.getMaxColumns(); j++) {
                    candidateHeaders.add(getOdsString(sheet, r, j));
                }

                Map<String, Integer> potentialMap = cartographierEnTetes(candidateHeaders);
                if (potentialMap.containsKey("nom")) {
                    headerRowIndex = r;
                    colMap = potentialMap;
                    break;
                }
            }

            if (headerRowIndex == -1) continue;

            int emptyRowCount = 0;
            for (int i = headerRowIndex + 1; i < sheet.getMaxRows(); i++) {
                List<String> rowValues = new ArrayList<>();
                for (int j = 0; j < sheet.getMaxColumns(); j++) {
                    rowValues.add(getOdsString(sheet, i, j));
                }

                if (isStringListEmpty(rowValues)) {
                    emptyRowCount++;
                    if (emptyRowCount >= 3) break;
                    continue;
                }
                emptyRowCount = 0;

                ImportClientDTO dto = buildDtoFromRow(rowValues, colMap);
                if (dto.getNom() != null && !dto.getNom().isBlank()) {
                    clients.add(dto);
                }
            }
        }
        return clients;
    }

    // --- CONSTRUCTEUR DE DTO ---

    private ImportClientDTO buildDtoFromRow(List<String> rowValues, Map<String, Integer> colMap) {
        return ImportClientDTO.builder()
                .nom(getValeurParChamp(rowValues, colMap, "nom"))
                .prenom(getValeurParChamp(rowValues, colMap, "prenom"))
                .dateNaissance(getValeurParChamp(rowValues, colMap, "dateNaissance"))
                .dateEntree(getValeurParChamp(rowValues, colMap, "dateEntree"))
                .dateSortie(getValeurParChamp(rowValues, colMap, "dateSortie"))
                .tarifParDefaut(getValeurParChamp(rowValues, colMap, "tarifParDefaut"))
                .commentaire(getValeurParChamp(rowValues, colMap, "commentaire"))
                .build();
    }

    // --- UTILITAIRES ---

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private boolean isExcelRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !getCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean isStringListEmpty(List<String> list) {
        if (list == null || list.isEmpty()) return true;
        return list.stream().allMatch(s -> s == null || s.isBlank());
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                double value = cell.getNumericCellValue();
                yield (value == Math.floor(value)) ? String.valueOf((long) value) : String.valueOf(value);
            }
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double value = cell.getNumericCellValue();
                        yield (value == Math.floor(value)) ? String.valueOf((long) value) : String.valueOf(value);
                    } catch (Exception ex) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private String getOdsString(com.github.miachm.sods.Sheet sheet, int row, int col) {
        Object val = sheet.getRange(row, col).getValue();
        if (val == null) return "";
        String str = val.toString().trim();
        if (str.contains("T")) {
            str = str.split("T")[0];
        }
        return str;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleanRaw = raw.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleanRaw, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        log.warn("Format de date invalide ou non reconnu : {}", raw);
        return null;
    }

    private BigDecimal parseMontant(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw.trim().replace(",", ".").replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            log.warn("Erreur lors de la conversion du montant: {}", raw);
            return BigDecimal.ZERO;
        }
    }
}