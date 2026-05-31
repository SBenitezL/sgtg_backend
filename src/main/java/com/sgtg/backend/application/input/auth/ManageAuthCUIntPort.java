package com.sgtg.backend.application.input.auth;

/** Interfaz para la gestión de operaciones de autenticación */
public interface ManageAuthCUIntPort {
    /**
     * Realiza el proceso de inicio de sesión para un usuario dado su correo
     * electrónico y contraseña.
     * 
     * @param email    correo electrónico del usuario, es obligatorio y debe cumplir
     *                 con formato de correo electrónico
     * @param password contraseña del usuario, es obligatoria
     * @return token de autenticación si el inicio de sesión es exitoso, o null si
     *         falla
     */
    public String login(String email, String password);
}
