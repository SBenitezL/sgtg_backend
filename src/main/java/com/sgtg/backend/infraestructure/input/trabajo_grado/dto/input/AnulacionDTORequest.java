package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.NotBlank;

/**
 * Anulación del trabajo por el director (borrado lógico).
 *
 * @param justificacion motivo de la anulación
 */
public record AnulacionDTORequest(
        @NotBlank(message = "La justificación es obligatoria") String justificacion) {
}
