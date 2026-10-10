package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import com.sgtg.backend.domain.models.trabajo_grado.Calificacion;

import jakarta.validation.constraints.NotNull;

/**
 * Calificación de un jurado. Se envía como la parte {@code datos} de la
 * petición; el acta de sustentación (formato G), opcional, va en la parte
 * {@code archivo}.
 *
 * @param calificacion  APROBADO, APROBADO_CON_OBSERVACIONES, APLAZADO o NO_APROBADO
 * @param observaciones obligatorias si no es APROBADO
 */
public record CalificacionSustentacionDTORequest(
        @NotNull(message = "La calificación es obligatoria") Calificacion calificacion,
        String observaciones) {
}
