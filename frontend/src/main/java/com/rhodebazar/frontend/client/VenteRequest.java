package com.rhodebazar.frontend.client;

import java.util.List;

public record VenteRequest(Long depotId, ModePaiement modePaiement, String clientNom, List<LigneVenteRequest> lignes) {
}
