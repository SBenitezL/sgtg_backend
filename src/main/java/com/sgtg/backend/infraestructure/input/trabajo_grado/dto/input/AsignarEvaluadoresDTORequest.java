package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.NotNull;

/**
 * Evaluadores del anteproyecto asignados por el Departamento.
 *
 * @param evaluador1Id usuario del primer evaluador
 * @param evaluador2Id usuario del segundo evaluador
 */
public record AsignarEvaluadoresDTORequest(
        @NotNull(message = "El primer evaluador es obligatorio") Long evaluador1Id,
        @NotNull(message = "El segundo evaluador es obligatorio") Long evaluador2Id) {
}
