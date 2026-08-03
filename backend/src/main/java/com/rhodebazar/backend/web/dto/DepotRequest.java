package com.rhodebazar.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepotRequest(
        @NotBlank @Size(max = 150) String nom,
        @Size(max = 255) String adresse) {
}
