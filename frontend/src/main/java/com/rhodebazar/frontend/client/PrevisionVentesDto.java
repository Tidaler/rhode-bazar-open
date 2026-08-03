package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record PrevisionVentesDto(
        int joursHistorique,
        BigDecimal moyenneJournaliere,
        BigDecimal previsionProchains7Jours,
        String tendance) {
}
