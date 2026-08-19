package com.facturation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ligne_facturation", schema = "app_facturation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LigneFacturation {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_ligne", updatable = false, nullable = false)
    private UUID idLigne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_facturation", nullable = false)
    private Facturation facturation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @Column(name = "date_facture", nullable = false)
    @Builder.Default
    private LocalDate dateFacture = LocalDate.now();

    @Column(name = "date_entree_effective", nullable = false)
    private LocalDate dateEntreeEffective;

    @Column(name = "date_sortie_effective")
    private LocalDate dateSortieEffective;

    @Column(name = "dernier_jour_mois", nullable = false)
    private LocalDate dernierJourMois;

    @Column(name = "tarif_applique", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifApplique;

    @Column(name = "nb_jours", nullable = false)
    @Builder.Default
    private Integer nbJours = 0;

    @Column(name = "montant_ht", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantHt = BigDecimal.ZERO;

    @Column(name = "montant_tva", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantTva = BigDecimal.ZERO;

    @Column(name = "montant_ttc", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montantTtc = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_ligne_facturation")
    @Builder.Default
    private StatutLigne statut = StatutLigne.ACTIF;

    @Column(name = "ordre_affichage", nullable = false)
    @Builder.Default
    private Integer ordreAffichage = 1;

    @Column(name = "observation")
    private String observation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public enum StatutLigne { ACTIF, NOUVEAU, SORTI, SUSPENDU }

    @PrePersist protected void onCreate() { createdAt = OffsetDateTime.now(); updatedAt = OffsetDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
