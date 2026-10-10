package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda una lista de textos como JSON en una sola columna. Se usa donde una
 * colección de valores contiene a su vez una lista (ej. los objetivos de una
 * solicitud de cambio), que JPA no permite anidar.
 */
@Converter
public class ListaTextoConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<String> lista) {
        if (lista == null)
            return null;
        try {
            return JSON.writeValueAsString(lista);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo convertir la lista a JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        if (json == null)
            return null;
        try {
            return JSON.readValue(json, TIPO);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("La columna no contiene una lista JSON válida", e);
        }
    }
}
