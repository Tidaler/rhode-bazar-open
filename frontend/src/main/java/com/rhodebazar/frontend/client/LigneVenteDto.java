package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record LigneVenteDto(Long produitId, String produitNom, int quantite, BigDecimal prixUnitaire, BigDecimal montantLigne) {
}
