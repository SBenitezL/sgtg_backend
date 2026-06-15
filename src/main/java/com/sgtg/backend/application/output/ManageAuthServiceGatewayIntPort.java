package com.sgtg.backend.application.output;

import com.sgtg.backend.domain.models.Usuario;

/*
    * Puerto de salida para la gestión de autenticación, define las operaciones
    * relacionadas con el inicio de sesión y registro de usuarios
*/
public interface ManageAuthServiceGatewayIntPort {
    /**
     * Inicia sesión con el correo electrónico y la contraseña proporcionados,
     * devuelve un token de autenticación si las credenciales son válidas.
     * 
     * @param email    Correo electrónico del usuario.
     * @param password Contraseña del usuario.
     * @return Token de autenticación si las credenciales son válidas, de lo
     *         contrario, null o una excepción.
     */
    String login(String email, String password);

    /**
     * Registra un nuevo usuario con la información proporcionada, devuelve true si
     * el registro es exitoso, de lo contrario, false o una excepción.
     * 
     * @param registerRequest Objeto que contiene la información del usuario a
     *                        registrar, como nombre, correo electrónico y
     *                        contraseña.
     * @return true si el registro es exitoso, de lo contrario, false o una
     *         excepción.
     */
    boolean register(Usuario registerRequest);
}
