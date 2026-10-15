package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Programación de una sesión de sustentación por el Consejo de Facultad. Se
 * envía como la parte {@code datos} de la petición; el formato E va en la
 * parte {@code archivo}.
 *
 * @param juradoCoordinadorId usuario del jurado coordinador (califica y registra las decisiones)
 * @param juradoId            usuario del otro jurado (solo consulta)
 * @param fechaHora           fecha y hora de la sesión (ej. 2026-10-20T14:00:00)
 * @param salon               salón
 */
public record ProgramacionSustentacionDTORequest(
        @NotNull(message = "El jurado coordinador es obligatorio") Long juradoCoordinadorId,
        @NotNull(message = "El segundo jurado es obligatorio") Long juradoId,
        @NotNull(message = "La fecha y hora son obligatorias") LocalDateTime fechaHora,
        @NotBlank(message = "El salón es obligatorio") String salon) {
}
