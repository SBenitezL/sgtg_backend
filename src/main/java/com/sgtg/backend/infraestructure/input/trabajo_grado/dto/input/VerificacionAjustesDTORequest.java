package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.NotNull;

/**
 * Verificación de un jurado sobre los ajustes de la sustentación.
 *
 * @param cumple        true si los ajustes cumplen
 * @param observaciones obligatorias si no cumplen
 */
public record VerificacionAjustesDTORequest(
        @NotNull(message = "Debe indicar si los ajustes cumplen") Boolean cumple,
        String observaciones) {
}
