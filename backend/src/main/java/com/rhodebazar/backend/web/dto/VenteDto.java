package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.vente.ModePaiement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VenteDto(
        Long id,
        String numero,
        LocalDateTime dateVente,
        String utilisateurNom,
        Long depotId,
        String depotNom,
        ModePaiement modePaiement,
        String clientNom,
        BigDecimal montantTotal,
        List<LigneVenteDto> lignes) {
}
