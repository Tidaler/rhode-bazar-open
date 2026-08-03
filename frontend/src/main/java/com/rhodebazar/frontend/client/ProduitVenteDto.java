package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record ProduitVenteDto(Long produitId, String produitNom, int quantiteVendue, BigDecimal montantVendu) {
}
