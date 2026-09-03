package com.facturation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangerMotDePasseRequestDTO {
    private String ancienMotDePasse;
    private String nouveauMotDePasse;
}