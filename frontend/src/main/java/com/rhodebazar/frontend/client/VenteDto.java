package com.rhodebazar.frontend.client;

import java.math.BigDecimal;
import java.util.List;

public record VenteDto(
        Long id,
        String numero,
        String dateVente,
        String utilisateurNom,
        Long depotId,
        String depotNom,
        ModePaiement modePaiement,
        String clientNom,
        BigDecimal montantTotal,
        List<LigneVenteDto> lignes) {
}
