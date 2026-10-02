package com.facturation.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Totaux d'une facturation mensuelle, utilisés par le tableau de bord. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ResumeFacturationDTO {
    private UUID idFacturation;
    private long nbPersonnes;
    private BigDecimal totalHt;
    private BigDecimal totalTva;
    private BigDecimal totalTtc;
}
