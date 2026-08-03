package com.rhodebazar.backend.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime horodatage,
        int statut,
        String message,
        List<String> details) {

    public static ApiErrorResponse de(int statut, String message) {
        return new ApiErrorResponse(LocalDateTime.now(), statut, message, List.of());
    }

    public static ApiErrorResponse de(int statut, String message, List<String> details) {
        return new ApiErrorResponse(LocalDateTime.now(), statut, message, details);
    }
}
