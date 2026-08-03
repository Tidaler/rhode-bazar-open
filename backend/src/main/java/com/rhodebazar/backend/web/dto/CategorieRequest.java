package com.rhodebazar.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategorieRequest(
        @NotBlank @Size(max = 150) String nom,
        @Size(max = 500) String description) {
}
