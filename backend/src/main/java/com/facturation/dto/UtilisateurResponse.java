package com.facturation.dto;

import com.facturation.entity.Utilisateur;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponse {

    private UUID idUtilisateur;
    private String nom;
    private String email;
    private Utilisateur.RoleUtilisateur role;
    private Boolean actif;
    private UUID idEntreprise;
    private OffsetDateTime derniereConnexion;
    private OffsetDateTime createdAt;
}