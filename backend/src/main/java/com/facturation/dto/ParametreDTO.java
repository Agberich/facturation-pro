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
    private String prefixeFacture;
    private String adresse;
    private String telephone;
    private String email;
    private String logoUrl;
    private String signatureUrl;
}