package com.rhodebazar.frontend.client;

import java.math.BigDecimal;

public record DepenseRequest(CategorieDepense categorie, String libelle, BigDecimal montant) {
}
