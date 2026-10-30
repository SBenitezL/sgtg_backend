package com.sgtg.backend.application.input.usuario;

/**
 * Usuario que aparece en la gestión de usuarios de un gestor (administrador,
 * Comité, jefe de departamento, Consejo o Decanatura).
 *
 * @param ficha       usuario con sus perfiles académicos
 * @param modificable true si el gestor puede editarlo, cambiar sus roles o
 *                    activarlo e inactivarlo; a los usuarios con roles
 *                    administrativos solo los modifica el administrador
 */
public record UsuarioGestionado(FichaUsuario ficha, boolean modificable) {
}
