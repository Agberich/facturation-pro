package com.facturation.dto;

import lombok.Data;

@Data
public class PremierAdminRequestDTO {
    private String nomEntreprise;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String motDePasse;
}