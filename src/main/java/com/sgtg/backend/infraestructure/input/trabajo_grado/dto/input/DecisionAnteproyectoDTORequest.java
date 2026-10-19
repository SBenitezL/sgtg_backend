package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.validation.constraints.NotNull;

/**
 * Decisión del Consejo de Facultad sobre el anteproyecto (parte {@code datos});
 * la resolución va en la parte {@code archivo}.
 *
 * @param resultado      APROBADO o NO_APROBADO
 * @param observaciones  comentario opcional
 * @param mesesEjecucion plazo de ejecución al aprobar: 6 o 9 meses en trabajo de investigación;
 *                       6, 9 o 12 en práctica profesional. Se cuenta desde el día del registro.
 */
public record DecisionAnteproyectoDTORequest(
        @NotNull(message = "El resultado es obligatorio") ResultadoConcepto resultado,
        String observaciones,
        Integer mesesEjecucion) {
}
