package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.NotBlank;

/**
 * Registro que solo requiere observaciones (ej. la inasistencia a la sustentación).
 *
 * @param observaciones soporte del registro
 */
public record ObservacionesDTORequest(
        @NotBlank(message = "Las observaciones son obligatorias") String observaciones) {
}
