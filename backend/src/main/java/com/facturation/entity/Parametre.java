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
    private Devise devise = Devise.XOF;

    @Column(name = "prefixe_facture", nullable = false, length = 20)
    @Builder.Default
    private String prefixeFacture = "FAC";

    @Column(name = "logo")
    private String logo;

    @Column(name = "signature")
    private String signature;

    @Column(name = "signature_url")
    private String signatureUrl;

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

    public enum Devise { XOF, EUR, USD }
}