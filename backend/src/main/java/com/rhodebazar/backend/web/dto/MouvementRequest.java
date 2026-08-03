package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.stock.TypeMouvement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MouvementRequest(
        @NotNull Long produitId,
        @NotNull Long depotId,
        @NotNull TypeMouvement typeMouvement,
        @Positive int quantite,
        @Size(max = 500) String motif) {
}
