package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record DepenseDto(
        Long id,
        CategorieDepense categorie,
        String libelle,
        BigDecimal montant,
        String dateDepense,
        String utilisateurNom) {
}
