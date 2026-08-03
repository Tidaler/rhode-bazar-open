package com.rhodebazar.frontend.client;

public record MouvementRequest(Long produitId, Long depotId, TypeMouvement typeMouvement, int quantite, String motif) {
}
