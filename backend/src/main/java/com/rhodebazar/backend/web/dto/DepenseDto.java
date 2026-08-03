package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.depense.CategorieDepense;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DepenseDto(
        Long id,
        CategorieDepense categorie,
        String libelle,
        BigDecimal montant,
        LocalDateTime dateDepense,
        String utilisateurNom) {
}
