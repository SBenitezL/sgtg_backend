package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.validation.constraints.NotNull;

/**
 * Decisión del Consejo de Facultad sobre la nueva fecha de sustentación. Se
 * envía como la parte {@code datos}; la resolución va en la parte
 * {@code archivo}.
 *
 * @param resultado     APROBADO o NO_APROBADO
 * @param fechaHora     nueva fecha y hora de la sesión; obligatoria si se aprueba
 * @param salon         nuevo salón; si se omite se conserva el actual
 * @param observaciones comentario opcional
 */
public record DecisionNuevaFechaDTORequest(
        @NotNull(message = "El resultado es obligatorio") ResultadoConcepto resultado,
        LocalDateTime fechaHora,
        String salon,
        String observaciones) {
}
