package com.rhodebazar.frontend.client;

import java.math.BigDecimal;
import java.util.List;

public record TableauDeBordDto(
        String debut,
        String fin,
        BigDecimal chiffreAffaires,
        BigDecimal coutMarchandisesVendues,
        BigDecimal margeBrute,
        BigDecimal depensesTotales,
        BigDecimal beneficeNet,
        List<ProduitVenteDto> produitsPlusVendus,
        List<ProduitVenteDto> produitsMoinsVendus,
        List<DepenseCategorieDto> depensesParCategorie,
        List<PointVenteJourDto> ventesParJour,
        String resumeLocal) {
}
