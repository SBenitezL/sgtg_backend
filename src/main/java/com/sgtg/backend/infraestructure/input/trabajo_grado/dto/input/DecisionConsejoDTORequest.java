package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.validation.constraints.NotNull;

/**
 * Decisión del Consejo de Facultad (anteproyecto, cambio de objetivos,
 * prórroga, suspensión o cancelación). Se envía como la parte {@code datos}
 * de la petición; la resolución va en la parte {@code archivo}. La fecha es
 * la del registro.
 *
 * @param resultado     APROBADO o NO_APROBADO
 * @param observaciones comentario opcional
 */
public record DecisionConsejoDTORequest(
        @NotNull(message = "El resultado es obligatorio") ResultadoConcepto resultado,
        String observaciones) {
}
