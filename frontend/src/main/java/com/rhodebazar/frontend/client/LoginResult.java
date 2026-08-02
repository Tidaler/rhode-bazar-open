package com.rhodebazar.frontend.client;

public record LoginResult(String token, String nom, String email, String role) {
}
