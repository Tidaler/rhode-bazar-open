package com.rhodebazar.backend.web.dto;

public record ResumeIaDto(boolean disponible, String texte) {

    public static ResumeIaDto indisponible(String raison) {
        return new ResumeIaDto(false, raison);
    }

    public static ResumeIaDto de(String texte) {
        return new ResumeIaDto(true, texte);
    }
}
