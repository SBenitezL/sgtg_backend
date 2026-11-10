package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.validation.constraints.NotNull;

/**
 * Decisión del Consejo de Facultad sobre el anteproyecto (parte {@code datos});
 * la resolución va en la parte {@code archivo}.
 *
 * @param resultado     APROBADO o NO_APROBADO
 * @param observaciones comentario opcional
 * @param fechaInicio   al aprobar, fecha de la reunión del Consejo en que se aprobó (inicio del plazo): no
 *                      futura ni anterior a la remisión con el formato C
 * @param fechaFin      al aprobar, fecha máxima de entrega: entre 3 y 9 meses después del inicio
 */
public record DecisionAnteproyectoDTORequest(
        @NotNull(message = "El resultado es obligatorio") ResultadoConcepto resultado,
        String observaciones,
        LocalDate fechaInicio,
        LocalDate fechaFin) {
}
