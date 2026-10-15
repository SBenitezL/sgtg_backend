package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.NotNull;

/**
 * Revisión del paz y salvo de Decanatura (parte {@code datos}); el formato F va
 * en la parte {@code archivo}.
 *
 * @param aprobado      true si el estudiante está a paz y salvo
 * @param observaciones obligatorias si no se aprueba
 */
public record RevisionPazYSalvoDTORequest(
        @NotNull(message = "Debe indicar si el paz y salvo se aprueba") Boolean aprobado,
        String observaciones) {
}
