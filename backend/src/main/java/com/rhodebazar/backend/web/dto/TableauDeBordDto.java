package com.rhodebazar.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TableauDeBordDto(
        LocalDate debut,
        LocalDate fin,
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
