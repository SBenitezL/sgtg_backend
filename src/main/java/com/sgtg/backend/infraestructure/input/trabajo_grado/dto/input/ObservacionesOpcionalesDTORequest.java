package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.Size;

/**
 * Observaciones opcionales que acompañan un documento (ej. un informe mensual
 * o su revisión).
 *
 * @param observaciones observaciones, null si no las hay
 */
public record ObservacionesOpcionalesDTORequest(
        @Size(max = 2000, message = "Las observaciones admiten máximo 2000 caracteres") String observaciones) {
}
