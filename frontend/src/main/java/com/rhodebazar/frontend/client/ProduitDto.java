package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record ProduitDto(
        Long id,
        String nom,
        String description,
        Long categorieId,
        String categorieNom,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        int stockMinimal,
        String unite,
        String codeBarres,
        boolean actif,
        int stockTotal,
        boolean stockCritique) {
}
