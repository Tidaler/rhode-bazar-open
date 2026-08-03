package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record AnomalieDto(
        String type,
        Long referenceId,
        String description,
        BigDecimal valeur,
        BigDecimal valeurMoyenneReference,
        String date) {
}
