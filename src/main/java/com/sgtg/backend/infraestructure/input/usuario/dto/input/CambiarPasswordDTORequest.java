package com.sgtg.backend.infraestructure.input.usuario.dto.input;

import jakarta.validation.constraints.NotBlank;

/**
 * Cambio de contraseña del usuario autenticado.
 *
 * @param passwordActual contraseña actual
 * @param passwordNueva  contraseña nueva, al menos 8 caracteres
 */
public record CambiarPasswordDTORequest(
        @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
        @NotBlank(message = "La contraseña nueva es obligatoria") String passwordNueva) {
}
