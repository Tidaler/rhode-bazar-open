package com.rhodebazar.backend.web.dto;

import java.math.BigDecimal;

public record LigneVenteDto(
        Long produitId,
        String produitNom,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal montantLigne) {
}
