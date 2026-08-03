package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.stock.TypeMouvement;
import java.time.LocalDateTime;

public record MouvementDto(
        Long id,
        Long produitId,
        String produitNom,
        Long depotId,
        String depotNom,
        TypeMouvement typeMouvement,
        int quantite,
        String motif,
        String utilisateurNom,
        LocalDateTime dateMouvement) {
}
