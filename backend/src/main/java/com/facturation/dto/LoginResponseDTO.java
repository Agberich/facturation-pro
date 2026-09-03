package com.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private String token;
    private UUID idUtilisateur;
    private String nom;
    private String email;
    private String role;
    private UUID idEntreprise;
    private Boolean doitChangerMotDePasse;
}