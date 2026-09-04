package com.facturation.dto;

import lombok.Data;

@Data
public class PremierAdminRequestDTO {
    private String nomEntreprise;
    private String nomAdmin;
    private String emailAdmin;
    private String motDePasse;
}