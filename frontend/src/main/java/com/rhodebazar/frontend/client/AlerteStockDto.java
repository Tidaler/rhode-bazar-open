package com.rhodebazar.frontend.client;

public record AlerteStockDto(
        Long produitId,
        String produitNom,
        Long depotId,
        String depotNom,
        int quantite,
        int stockMinimal) {
}
