package com.sgtg.backend.application.input.auth;

import com.sgtg.backend.domain.models.Usuario;

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

    /**
     * Realiza el proceso de registro de un nuevo usuario en el sistema.
     * 
     * @param usuario objeto Usuario que contiene la información del nuevo usuario a
     *                registrar, debe incluir al menos el correo electrónico,
     *                contraseña y nombre de usuario.
     * @return retorna true si el registro fue exitoso, o false si el registro falla
     *         (por ejemplo, si el correo electrónico ya está registrado o si los
     *         datos son inválidos)
     */
    public boolean register(Usuario usuario);
}
