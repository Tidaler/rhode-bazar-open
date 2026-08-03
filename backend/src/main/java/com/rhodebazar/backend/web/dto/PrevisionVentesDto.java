package com.rhodebazar.backend.web.dto;

import java.math.BigDecimal;

public record PrevisionVentesDto(
        int joursHistorique,
        BigDecimal moyenneJournaliere,
        BigDecimal previsionProchains7Jours,
        String tendance) {
}
