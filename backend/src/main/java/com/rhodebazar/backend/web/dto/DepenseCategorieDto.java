package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.depense.CategorieDepense;
import java.math.BigDecimal;

public record DepenseCategorieDto(CategorieDepense categorie, BigDecimal total) {
}
