package com.sgtg.backend.infraestructure.input.usuario.dto.input;

import jakarta.validation.constraints.NotNull;

/**
 * Activación o inactivación de un usuario por el administrador.
 *
 * @param activo true para activar, false para inactivar
 */
public record CambiarEstadoDTORequest(@NotNull(message = "El estado es obligatorio") Boolean activo) {
}
