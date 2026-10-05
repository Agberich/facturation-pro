package com.facturation.service;

import com.facturation.dto.ParametreDTO;
import com.facturation.entity.Entreprise;
import com.facturation.entity.Parametre;
import com.facturation.repository.EntrepriseRepository;
import com.facturation.repository.ParametreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParametreService {

    private final ParametreRepository parametreRepository;
    private final EntrepriseRepository entrepriseRepository;

    @Transactional(readOnly = true)
    public ParametreDTO obtenirParametres(UUID idEntreprise) {
        Parametre parametre = parametreRepository.findByEntrepriseIdEntreprise(idEntreprise)
                .orElseGet(() -> {
                    Entreprise entreprise = entrepriseRepository.findById(idEntreprise)
                            .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable : " + idEntreprise));
                    return Parametre.builder()
                            .entreprise(entreprise)
                            .devise(Parametre.Devise.EUR)
                            .tarifJournalier(Parametre.TARIF_JOURNALIER_PAR_DEFAUT)
                            .tauxTva(BigDecimal.ZERO)
                            .prefixeFacture("FAC-")
                            .build();
                });
        return versDTO(parametre);
    }

    @Transactional
    public ParametreDTO mettreAJourParametres(UUID idEntreprise, ParametreDTO dto) {
        Parametre parametre = parametreRepository.findByEntrepriseIdEntreprise(idEntreprise)
                .orElseGet(() -> {
                    Entreprise entreprise = entrepriseRepository.findById(idEntreprise)
                            .orElseThrow(() -> new IllegalArgumentException("Entreprise introuvable : " + idEntreprise));
                    return Parametre.builder().entreprise(entreprise).build();
                });

        if (dto.getTauxTva() != null) {
            parametre.setTauxTva(dto.getTauxTva());
        }
        if (dto.getDevise() != null && !dto.getDevise().isBlank()) {
            try {
                parametre.setDevise(Parametre.Devise.valueOf(dto.getDevise().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Devise invalide : " + dto.getDevise());
            }
        }
        if (dto.getTarifJournalier() != null) {
            if (dto.getTarifJournalier().signum() < 0) {
                throw new IllegalArgumentException("Le tarif journalier ne peut pas être négatif.");
            }
            parametre.setTarifJournalier(dto.getTarifJournalier());
        }
        if (dto.getPrefixeFacture() != null && !dto.getPrefixeFacture().isBlank()) {
            parametre.setPrefixeFacture(dto.getPrefixeFacture());
        }
        if (dto.getLogoUrl() != null) {
            parametre.setLogo(validerImage(dto.getLogoUrl(), "logo"));
        }
        if (dto.getSignatureUrl() != null) {
            parametre.setSignatureUrl(validerImage(dto.getSignatureUrl(), "signature"));
        }
        // Informations de facture : null = inchangé, texte vide = effacé
        if (dto.getDispositif() != null) {
            parametre.setDispositif(nettoyer(dto.getDispositif(), 150));
        }
        if (dto.getTypePrestation() != null) {
            parametre.setTypePrestation(nettoyer(dto.getTypePrestation(), 200));
        }
        if (dto.getCategorieEtablissement() != null) {
            parametre.setCategorieEtablissement(nettoyer(dto.getCategorieEtablissement(), 200));
        }
        if (dto.getDiscipline() != null) {
            parametre.setDiscipline(nettoyer(dto.getDiscipline(), 200));
        }
        if (dto.getModeFonctionnement() != null) {
            parametre.setModeFonctionnement(nettoyer(dto.getModeFonctionnement(), 200));
        }
        if (dto.getPublicAccueilli() != null) {
            parametre.setPublicAccueilli(nettoyer(dto.getPublicAccueilli(), 200));
        }
        if (dto.getCentreProfit() != null) {
            parametre.setCentreProfit(nettoyer(dto.getCentreProfit(), 150));
        }
        if (dto.getFinanceurNom() != null) {
            parametre.setFinanceurNom(nettoyer(dto.getFinanceurNom(), 200));
        }
        if (dto.getFinanceurService() != null) {
            parametre.setFinanceurService(nettoyer(dto.getFinanceurService(), 300));
        }
        if (dto.getFinanceurAdresse() != null) {
            parametre.setFinanceurAdresse(nettoyer(dto.getFinanceurAdresse(), 2000));
        }
        if (dto.getFinanceurEmail() != null) {
            parametre.setFinanceurEmail(nettoyer(dto.getFinanceurEmail(), 150));
        }
        if (dto.getFinanceurSiret() != null) {
            parametre.setFinanceurSiret(nettoyer(dto.getFinanceurSiret(), 20));
        }
        if (dto.getNumeroEngagement() != null) {
            parametre.setNumeroEngagement(nettoyer(dto.getNumeroEngagement(), 60));
        }
        if (dto.getFournisseurSiret() != null) {
            parametre.setFournisseurSiret(nettoyer(dto.getFournisseurSiret(), 20));
        }
        if (dto.getDirectionTerritoriale() != null) {
            parametre.setDirectionTerritoriale(nettoyer(dto.getDirectionTerritoriale(), 200));
        }
        if (dto.getIban() != null) {
            parametre.setIban(nettoyer(dto.getIban(), 40));
        }
        if (dto.getMentionReglement() != null) {
            parametre.setMentionReglement(nettoyer(dto.getMentionReglement(), 200));
        }
        if (dto.getContactUt() != null) {
            parametre.setContactUt(nettoyer(dto.getContactUt(), 200));
        }
        if (dto.getInterlocuteur() != null) {
            parametre.setInterlocuteur(nettoyer(dto.getInterlocuteur(), 200));
        }
        if (dto.getFonctionInterlocuteur() != null) {
            parametre.setFonctionInterlocuteur(nettoyer(dto.getFonctionInterlocuteur(), 100));
        }
        if (dto.getContactDispositif() != null) {
            parametre.setContactDispositif(nettoyer(dto.getContactDispositif(), 200));
        }
        if (dto.getPremierMoisPrestation() != null) {
            if (dto.getPremierMoisPrestation() < 1 || dto.getPremierMoisPrestation() > 12) {
                throw new IllegalArgumentException("Le premier mois de la prestation doit être compris entre 1 et 12.");
            }
            parametre.setPremierMoisPrestation(dto.getPremierMoisPrestation());
        }
        if (dto.getCapacite() != null) {
            if (dto.getCapacite() < 0) {
                throw new IllegalArgumentException("La capacité ne peut pas être négative.");
            }
            parametre.setCapacite(dto.getCapacite());
        }

        Entreprise entreprise = parametre.getEntreprise();
        if (entreprise != null) {
            if (dto.getNomEntreprise() != null && !dto.getNomEntreprise().isBlank()) {
                entreprise.setNom(dto.getNomEntreprise());
            }
            entreprise.setAdresse(dto.getAdresse());
            entreprise.setTelephone(dto.getTelephone());
            entreprise.setEmail(dto.getEmail());
            entrepriseRepository.save(entreprise);
        }

        return versDTO(parametreRepository.save(parametre));
    }

    private ParametreDTO versDTO(Parametre p) {
        Entreprise entreprise = p.getEntreprise();
        UUID idEnt = (entreprise != null) ? entreprise.getIdEntreprise() : null;
        String deviseStr = (p.getDevise() != null) ? p.getDevise().name() : "EUR";

        return ParametreDTO.builder()
                .idParametre(p.getIdParametre())
                .idEntreprise(idEnt)
                .nomEntreprise(entreprise != null ? entreprise.getNom() : null)
                .tauxTva(p.getTauxTva())
                .devise(deviseStr)
                .tarifJournalier(p.getTarifJournalier() != null ? p.getTarifJournalier() : Parametre.TARIF_JOURNALIER_PAR_DEFAUT)
                .prefixeFacture(p.getPrefixeFacture())
                .adresse(entreprise != null ? entreprise.getAdresse() : null)
                .telephone(entreprise != null ? entreprise.getTelephone() : null)
                .email(entreprise != null ? entreprise.getEmail() : null)
                .logoUrl(p.getLogo())
                .signatureUrl(p.getSignatureUrl())
                .dispositif(p.getDispositif())
                .typePrestation(p.getTypePrestation())
                .categorieEtablissement(p.getCategorieEtablissement())
                .discipline(p.getDiscipline())
                .modeFonctionnement(p.getModeFonctionnement())
                .publicAccueilli(p.getPublicAccueilli())
                .centreProfit(p.getCentreProfit())
                .financeurNom(p.getFinanceurNom())
                .financeurService(p.getFinanceurService())
                .financeurAdresse(p.getFinanceurAdresse())
                .financeurEmail(p.getFinanceurEmail())
                .financeurSiret(p.getFinanceurSiret())
                .numeroEngagement(p.getNumeroEngagement())
                .fournisseurSiret(p.getFournisseurSiret())
                .directionTerritoriale(p.getDirectionTerritoriale())
                .iban(p.getIban())
                .mentionReglement(p.getMentionReglement())
                .contactUt(p.getContactUt())
                .interlocuteur(p.getInterlocuteur())
                .fonctionInterlocuteur(p.getFonctionInterlocuteur())
                .contactDispositif(p.getContactDispositif())
                .capacite(p.getCapacite())
                .premierMoisPrestation(p.getPremierMoisPrestation() != null ? p.getPremierMoisPrestation() : 4)
                .build();
    }

    /** Texte vide = champ effacé (null) ; texte trop long = refusé avec un message clair. */
    private String nettoyer(String valeur, int longueurMax) {
        String v = valeur.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.length() > longueurMax) {
            throw new IllegalArgumentException("Un champ dépasse " + longueurMax + " caractères.");
        }
        return v;
    }

    /**
     * Le logo et la signature sont des images PNG ou JPEG envoyées sous la forme d'une adresse
     * « data: » (base64), 350 Ko maximum. Une valeur vide supprime l'image.
     */
    private String validerImage(String valeur, String nom) {
        String v = valeur.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (!(v.startsWith("data:image/png;base64,") || v.startsWith("data:image/jpeg;base64,"))) {
            throw new IllegalArgumentException("Le " + nom + " doit être une image PNG ou JPEG.");
        }
        if (v.length() > 350_000) {
            throw new IllegalArgumentException("Le " + nom + " est trop volumineux (350 Ko maximum).");
        }
        return v;
    }
}
