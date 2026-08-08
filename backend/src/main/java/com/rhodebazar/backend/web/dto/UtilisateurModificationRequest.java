package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.utilisateur.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UtilisateurModificationRequest(
        @NotBlank @Size(max = 150) String nom,
        @NotNull Role role,
        boolean actif) {
}
