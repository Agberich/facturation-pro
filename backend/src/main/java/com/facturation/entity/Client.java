package com.facturation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "client", schema = "app_facturation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_client", updatable = false, nullable = false)
    private UUID idClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entreprise", nullable = false)
    private Entreprise entreprise;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "date_entree", nullable = false)
    @Builder.Default
    private LocalDate dateEntree = LocalDate.now();

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    @Column(name = "tarif_par_defaut", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tarifParDefaut = BigDecimal.ZERO;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.dateEntree == null) this.dateEntree = LocalDate.now();
        if (this.tarifParDefaut == null) this.tarifParDefaut = BigDecimal.ZERO;
        if (this.actif == null) this.actif = true;
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }
}
