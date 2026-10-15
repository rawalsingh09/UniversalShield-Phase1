package com.universalshield.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universalshield.engine.Recommendation;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/** Stores recommendations as a JSON array in a TEXT column. */
@Converter
public class RecommendationListConverter implements AttributeConverter<List<Recommendation>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Recommendation> list) {
        try {
            return MAPPER.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialise recommendations", e);
        }
    }

    @Override
    public List<Recommendation> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialise recommendations", e);
        }
    }
}
