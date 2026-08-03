package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.depense.CategorieDepense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DepenseRequest(
        @NotNull CategorieDepense categorie,
        @NotBlank @Size(max = 255) String libelle,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal montant) {
}
