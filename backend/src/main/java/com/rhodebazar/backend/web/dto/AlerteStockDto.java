package com.rhodebazar.backend.web.dto;

public record AlerteStockDto(
        Long produitId,
        String produitNom,
        Long depotId,
        String depotNom,
        int quantite,
        int stockMinimal) {
}
