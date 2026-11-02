package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.validation.constraints.NotNull;

/**
 * Aval del Comité del programa sobre el cambio de título u objetivos. Se envía
 * como la parte {@code datos} de la petición; el acta o concepto del Comité,
 * opcional, va en la parte {@code archivo}. La fecha es la del registro.
 *
 * @param resultado     APROBADO (lo avala) o NO_APROBADO (no lo avala)
 * @param observaciones obligatorias si no lo avala
 */
public record AvalComiteDTORequest(
        @NotNull(message = "El resultado es obligatorio") ResultadoConcepto resultado,
        String observaciones) {
}
