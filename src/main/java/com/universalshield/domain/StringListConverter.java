package com.universalshield.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/** Stores a List&lt;String&gt; as a JSON array in a TEXT column (portable across H2 and PostgreSQL). */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        try {
            return MAPPER.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialise list", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() { });
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialise list", e);
        }
    }
}
