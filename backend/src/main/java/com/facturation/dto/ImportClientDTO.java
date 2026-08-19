package com.facturation.dto;

import lombok.*;

/**
 * Representation brute (non typee) d'une ligne importee depuis un fichier
 * Excel / CSV / ODS, avant conversion en entite Client.
 *
 * Colonnes attendues, dans cet ordre : nom, prenom, date_naissance,
 * date_entree, tarif_par_defaut, commentaire.
 * Les dates sont au format ISO (yyyy-MM-dd).
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ImportClientDTO {
    private String nom;
    private String prenom;
    private String dateNaissance;
    private String dateEntree;
    private String dateSortie; 
    private String tarifParDefaut;
    private String commentaire;
}
