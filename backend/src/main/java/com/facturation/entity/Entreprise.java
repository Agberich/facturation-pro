package com.facturation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "entreprise", schema = "app_facturation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Entreprise {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_entreprise", updatable = false, nullable = false)
    private UUID idEntreprise;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "raison_sociale", length = 200)
    private String raisonSociale;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "email", length = 150)
    private String email;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
