package com.rhodebazar.frontend.client;

public record CategorieDto(Long id, String nom, String description) {

    @Override
    public String toString() {
        return nom;
    }
}
