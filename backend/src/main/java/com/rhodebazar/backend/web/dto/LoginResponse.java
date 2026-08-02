package com.rhodebazar.backend.web.dto;

public record LoginResponse(
        String token,
        String nom,
        String email,
        String role) {
}
