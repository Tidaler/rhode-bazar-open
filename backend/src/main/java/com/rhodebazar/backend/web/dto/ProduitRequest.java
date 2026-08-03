package com.rhodebazar.backend.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProduitRequest(
        @NotBlank @Size(max = 200) String nom,
        @Size(max = 1000) String description,
        Long categorieId,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal prixAchat,
        @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal prixVente,
        @Min(0) int stockMinimal,
        @NotBlank @Size(max = 30) String unite) {
}
