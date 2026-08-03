package com.rhodebazar.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnomalieDto(
        String type,
        Long referenceId,
        String description,
        BigDecimal valeur,
        BigDecimal valeurMoyenneReference,
        LocalDateTime date) {
}
