package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Programación de una sesión de sustentación por el Consejo de Facultad.
 *
 * @param jurado1Id usuario del primer jurado
 * @param jurado2Id usuario del segundo jurado
 * @param fechaHora fecha y hora de la sesión (ej. 2026-10-20T14:00:00)
 * @param salon     salón
 */
public record ProgramacionSustentacionDTORequest(
        @NotNull(message = "El primer jurado es obligatorio") Long jurado1Id,
        @NotNull(message = "El segundo jurado es obligatorio") Long jurado2Id,
        @NotNull(message = "La fecha y hora son obligatorias") LocalDateTime fechaHora,
        @NotBlank(message = "El salón es obligatorio") String salon) {
}
