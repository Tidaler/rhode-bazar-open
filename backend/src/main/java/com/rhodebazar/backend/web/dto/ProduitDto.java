package com.rhodebazar.backend.web.dto;

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
        boolean actif,
        int stockTotal,
        boolean stockCritique) {
}
