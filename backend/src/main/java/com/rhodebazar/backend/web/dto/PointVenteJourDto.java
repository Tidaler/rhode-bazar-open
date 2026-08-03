package com.rhodebazar.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PointVenteJourDto(LocalDate date, BigDecimal total) {
}
