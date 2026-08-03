package com.rhodebazar.backend.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stocke les LocalDateTime en texte ISO-8601 plutot que via le type TIMESTAMP natif.
 * Evite un bug de compatibilite entre le pilote sqlite-jdbc et le dialecte communautaire
 * Hibernate lors de la relecture des timestamps (precision des fractions de seconde).
 */
@Converter(autoApply = true)
public class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute == null ? null : attribute.format(FORMAT);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LocalDateTime.parse(dbData, FORMAT);
    }
}
