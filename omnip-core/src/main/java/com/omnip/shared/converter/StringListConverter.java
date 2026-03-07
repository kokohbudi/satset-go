package com.omnip.shared.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Collections;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting list to JSON string", e);
        }
    }
    
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Handle case where data might be stored as JSON array or simple string
            if (dbData.startsWith("[") && dbData.endsWith("]")) {
                return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
            } else {
                // Handle legacy data that might be stored as comma-separated string
                return List.of(dbData.split(","));
            }
        } catch (JsonProcessingException e) {
            // If JSON parsing fails, try to handle as simple string
            try {
                return List.of(dbData.split(","));
            } catch (Exception ex) {
                throw new RuntimeException("Error converting JSON string to list: " + dbData, e);
            }
        }
    }
}
