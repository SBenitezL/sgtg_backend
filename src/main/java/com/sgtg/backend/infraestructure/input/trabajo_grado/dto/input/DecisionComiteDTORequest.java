package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.validation.constraints.NotNull;

/**
 * Concepto del Comité de Programa sobre la propuesta. Se envía como la parte
 * {@code datos} de la petición; el acta de soporte, opcional, va en la parte
 * {@code archivo}.
 *
 * @param resultado     APROBADO o NO_APROBADO
 * @param acta          número de acta, opcional
 * @param observaciones obligatorias si no es aprobatorio
 * @param fecha         fecha de la decisión; si se omite se usa la fecha actual
 */
public record DecisionComiteDTORequest(
        @NotNull(message = "El resultado es obligatorio") ResultadoConcepto resultado,
        String acta,
        String observaciones,
        LocalDate fecha) {
}
