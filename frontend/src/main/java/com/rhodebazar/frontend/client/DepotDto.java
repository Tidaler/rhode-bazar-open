package com.rhodebazar.frontend.client;

public record DepotDto(Long id, String nom, String adresse, boolean actif) {

    @Override
    public String toString() {
        return nom;
    }
}
