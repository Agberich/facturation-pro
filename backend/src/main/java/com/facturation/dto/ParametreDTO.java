package com.facturation.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ParametreDTO {
    private UUID idParametre;
    private UUID idEntreprise;
    private String nomEntreprise;
    private BigDecimal tauxTva;
    private String devise;
    private BigDecimal tarifJournalier;
    private String prefixeFacture;
    private String adresse;
    private String telephone;
    private String email;
    private String logoUrl;
    private String signatureUrl;
    private String dispositif;
    private String typePrestation;
    private String categorieEtablissement;
    private String discipline;
    private String modeFonctionnement;
    private String publicAccueilli;
    private String centreProfit;
    private String financeurNom;
    private String financeurService;
    private String financeurAdresse;
    private String financeurEmail;
    private String financeurSiret;
    private String numeroEngagement;
    private String fournisseurSiret;
    private String directionTerritoriale;
    private String iban;
    private String mentionReglement;
    private String contactUt;
    private String interlocuteur;
    private String fonctionInterlocuteur;
    private String contactDispositif;
    private Integer capacite;
    private Integer premierMoisPrestation;
}