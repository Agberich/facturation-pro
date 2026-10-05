package com.facturation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "parametre", schema = "app_facturation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Parametre {

    @Id 
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_parametre", updatable = false, nullable = false)
    private UUID idParametre;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entreprise", nullable = true, unique = true)
    private Entreprise entreprise;

   @Column(name = "taux_tva", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxTva = new BigDecimal("18.00");

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "devise", nullable = false, length = 3, columnDefinition = "devise")
    @Builder.Default
    private Devise devise = Devise.EUR;

    @Column(name = "tarif_journalier", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tarifJournalier = new BigDecimal("79.92");

    @Column(name = "prefixe_facture", nullable = false, length = 20)
    @Builder.Default
    private String prefixeFacture = "FAC";

    @Column(name = "logo")
    private String logo;

    @Column(name = "signature")
    private String signature;

    @Column(name = "signature_url")
    private String signatureUrl;


    // --- Informations imprimées sur la facture (toutes facultatives) ---
    @Column(name = "dispositif", length = 150)
    private String dispositif;
    @Column(name = "type_prestation", length = 200)
    private String typePrestation;
    @Column(name = "categorie_etablissement", length = 200)
    private String categorieEtablissement;
    @Column(name = "discipline", length = 200)
    private String discipline;
    @Column(name = "mode_fonctionnement", length = 200)
    private String modeFonctionnement;
    @Column(name = "public_accueilli", length = 200)
    private String publicAccueilli;
    @Column(name = "centre_profit", length = 150)
    private String centreProfit;
    @Column(name = "financeur_nom", length = 200)
    private String financeurNom;
    @Column(name = "financeur_service", length = 300)
    private String financeurService;
    @Column(name = "financeur_adresse")
    private String financeurAdresse;
    @Column(name = "financeur_email", length = 150)
    private String financeurEmail;
    @Column(name = "financeur_siret", length = 20)
    private String financeurSiret;
    @Column(name = "numero_engagement", length = 60)
    private String numeroEngagement;
    @Column(name = "fournisseur_siret", length = 20)
    private String fournisseurSiret;
    @Column(name = "direction_territoriale", length = 200)
    private String directionTerritoriale;
    @Column(name = "iban", length = 40)
    private String iban;
    @Column(name = "mention_reglement", length = 200)
    private String mentionReglement;
    @Column(name = "contact_ut", length = 200)
    private String contactUt;
    @Column(name = "interlocuteur", length = 200)
    private String interlocuteur;
    @Column(name = "fonction_interlocuteur", length = 100)
    private String fonctionInterlocuteur;
    @Column(name = "contact_dispositif", length = 200)
    private String contactDispositif;
    @Column(name = "capacite")
    private Integer capacite;

    /** Mois (1-12) où commence le trimestre 1 de la prestation ; 4 = avril. */
    @Column(name = "premier_mois_prestation", nullable = false)
    @Builder.Default
    private Integer premierMoisPrestation = 4;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist 
    protected void onCreate() { 
        createdAt = OffsetDateTime.now(); 
        updatedAt = OffsetDateTime.now(); 
    }

    @PreUpdate 
    protected void onUpdate() { 
        updatedAt = OffsetDateTime.now(); 
    }

    /** Tarif par jour et par personne accueillie utilisé tant qu'aucun tarif n'est saisi. */
    public static final BigDecimal TARIF_JOURNALIER_PAR_DEFAUT = new BigDecimal("79.92");

    public enum Devise { XOF, EUR, USD }
}