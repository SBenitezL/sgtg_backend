package com.sgtg.backend.infraestructure.input.usuario.dto.input;

import jakarta.validation.constraints.NotNull;

/**
 * Cambio del Comité de un departamento.
 *
 * @param usuarioId docente que queda como Comité; el departamento es el de su perfil docente
 */
public record CambiarComiteDTORequest(@NotNull(message = "Se debe indicar el docente que queda como Comité") Long usuarioId) {
}
