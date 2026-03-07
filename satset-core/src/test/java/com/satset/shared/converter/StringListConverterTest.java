package com.satset.shared.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    // ==================== convertToDatabaseColumn ====================

    @Test
    void convertToDatabaseColumn_NullList_ReturnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_EmptyList_ReturnsNull() {
        assertNull(converter.convertToDatabaseColumn(List.of()));
    }

    @Test
    void convertToDatabaseColumn_SingleElement_ReturnsJsonArray() {
        String result = converter.convertToDatabaseColumn(List.of("reseller"));

        assertEquals("[\"reseller\"]", result);
    }

    @Test
    void convertToDatabaseColumn_MultipleElements_ReturnsJsonArray() {
        String result = converter.convertToDatabaseColumn(List.of("reseller", "admin", "viewer"));

        assertEquals("[\"reseller\",\"admin\",\"viewer\"]", result);
    }

    // ==================== convertToEntityAttribute ====================

    @Test
    void convertToEntityAttribute_NullString_ReturnsEmptyList() {
        List<String> result = converter.convertToEntityAttribute(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_BlankString_ReturnsEmptyList() {
        List<String> result = converter.convertToEntityAttribute("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void convertToEntityAttribute_JsonArray_ParsesCorrectly() {
        List<String> result = converter.convertToEntityAttribute("[\"reseller\",\"admin\"]");

        assertEquals(2, result.size());
        assertEquals("reseller", result.get(0));
        assertEquals("admin", result.get(1));
    }

    @Test
    void convertToEntityAttribute_LegacyCommaSeparated_ParsesCorrectly() {
        List<String> result = converter.convertToEntityAttribute("reseller,admin");

        assertEquals(2, result.size());
        assertEquals("reseller", result.get(0));
        assertEquals("admin", result.get(1));
    }

    @Test
    void convertToEntityAttribute_SingleJsonValue_ReturnsOneElement() {
        List<String> result = converter.convertToEntityAttribute("[\"reseller\"]");

        assertEquals(1, result.size());
        assertEquals("reseller", result.getFirst());
    }

    // ==================== roundtrip ====================

    @Test
    void roundtrip_ListSavedAndLoaded_IsEqual() {
        List<String> original = List.of("role_a", "role_b", "role_c");

        String dbValue = converter.convertToDatabaseColumn(original);
        List<String> result = converter.convertToEntityAttribute(dbValue);

        assertEquals(original, result);
    }
}
