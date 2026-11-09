package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.validation.constraints.NotNull;

/**
 * Concepto de un evaluador sobre el anteproyecto. Se envía como la parte
 * {@code datos} de la petición; el formato B, opcional, va en la parte
 * {@code archivo}. La fecha es la del registro.
 *
 * @param resultado     APROBADO, CON_CORRECCIONES o NO_APROBADO
 * @param observaciones obligatorias si no aprueba (con correcciones o no aprobado)
 */
public record ConceptoEvaluadorDTORequest(
        @NotNull(message = "El resultado es obligatorio") ResultadoConcepto resultado,
        String observaciones) {
}
