package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud de prórroga del director. Se envía como la parte {@code datos} de
 * la petición; el documento de la solicitud va en la parte {@code archivo}.
 *
 * @param meses meses solicitados, de 1 a 3
 */
public record SolicitudProrrogaDTORequest(
        @NotNull(message = "Los meses son obligatorios") @Min(value = 1, message = "La prórroga debe ser de 1 a 3 meses")
        @Max(value = 3, message = "La prórroga debe ser de 1 a 3 meses") Integer meses) {
}
