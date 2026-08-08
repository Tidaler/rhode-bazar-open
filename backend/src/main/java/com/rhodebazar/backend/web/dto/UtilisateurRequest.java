package com.rhodebazar.backend.web.dto;

import com.rhodebazar.backend.domain.utilisateur.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UtilisateurRequest(
        @NotBlank @Size(max = 150) String nom,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String motDePasse,
        @NotNull Role role) {
}
