package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record SuggestionReapprovisionnementDto(
        Long produitId,
        String produitNom,
        int stockActuel,
        BigDecimal venteJournaliereMoyenne,
        BigDecimal joursDeCouverture,
        int quantiteSuggeree) {
}
