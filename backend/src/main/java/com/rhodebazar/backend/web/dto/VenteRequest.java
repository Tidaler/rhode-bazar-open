package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.vente.ModePaiement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VenteRequest(
        @NotNull Long depotId,
        @NotNull ModePaiement modePaiement,
        @Size(max = 150) String clientNom,
        @NotEmpty @Valid List<LigneVenteRequest> lignes) {
}
