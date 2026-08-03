package com.rhodebazar.backend.web.dto;

import java.math.BigDecimal;

public record ProduitVenteDto(Long produitId, String produitNom, int quantiteVendue, BigDecimal montantVendu) {
}
