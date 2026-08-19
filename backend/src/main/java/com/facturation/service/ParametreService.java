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
                            .devise(Parametre.Devise.XOF)
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
        if (dto.getPrefixeFacture() != null && !dto.getPrefixeFacture().isBlank()) {
            parametre.setPrefixeFacture(dto.getPrefixeFacture());
        }
        if (dto.getLogoUrl() != null) {
            parametre.setLogo(dto.getLogoUrl());
        }
        if (dto.getSignatureUrl() != null) {
            parametre.setSignatureUrl(dto.getSignatureUrl());
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
        String deviseStr = (p.getDevise() != null) ? p.getDevise().name() : "XOF";

        return ParametreDTO.builder()
                .idParametre(p.getIdParametre())
                .idEntreprise(idEnt)
                .nomEntreprise(entreprise != null ? entreprise.getNom() : null)
                .tauxTva(p.getTauxTva())
                .devise(deviseStr)
                .prefixeFacture(p.getPrefixeFacture())
                .adresse(entreprise != null ? entreprise.getAdresse() : null)
                .telephone(entreprise != null ? entreprise.getTelephone() : null)
                .email(entreprise != null ? entreprise.getEmail() : null)
                .logoUrl(p.getLogo())
                .signatureUrl(p.getSignatureUrl())
                .build();
    }
}