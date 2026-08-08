package top.egon.cola.component.ddc.admin.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class DdcStringMapConverter
        implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, String>> TYPE =
            new TypeReference<>() {
            };

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return MAPPER.writeValueAsString(
                    attribute == null ? Map.of() : attribute
            );
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "Cannot serialize DDC instance metadata",
                    failure
            );
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return Map.copyOf(MAPPER.readValue(value, TYPE));
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "Cannot deserialize DDC instance metadata",
                    failure
            );
        }
    }
}
