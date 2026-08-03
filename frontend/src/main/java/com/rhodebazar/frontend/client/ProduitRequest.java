package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record ProduitRequest(
        String nom,
        String description,
        Long categorieId,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        int stockMinimal,
        String unite) {
}
