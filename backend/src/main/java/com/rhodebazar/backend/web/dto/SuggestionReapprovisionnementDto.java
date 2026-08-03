package com.rhodebazar.backend.web.dto;

import java.math.BigDecimal;

public record SuggestionReapprovisionnementDto(
        Long produitId,
        String produitNom,
        int stockActuel,
        BigDecimal venteJournaliereMoyenne,
        BigDecimal joursDeCouverture,
        int quantiteSuggeree) {
}
