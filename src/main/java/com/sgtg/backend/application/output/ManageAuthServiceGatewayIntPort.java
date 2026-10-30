package com.sgtg.backend.application.output;

import com.sgtg.backend.domain.models.Usuario;

/*
    * Puerto de salida para la gestión de autenticación, define las operaciones
    * relacionadas con las credenciales, los tokens y el registro de usuarios
*/
public interface ManageAuthServiceGatewayIntPort {

    /**
     * Verifica que exista un usuario con el correo indicado y que la contraseña
     * corresponda, sin importar si el usuario está activo. Toma un tiempo similar
     * exista o no el correo, para no revelar qué correos están registrados.
     *
     * @param email    Correo electrónico del usuario.
     * @param password Contraseña en texto plano.
     * @return true si las credenciales son correctas.
     */
    boolean verificarCredenciales(String email, String password);

    /**
     * Genera el token de autenticación (JWT) del usuario con sus roles.
     *
     * @param email Correo electrónico del usuario, debe existir.
     * @return Token de autenticación.
     */
    String generarToken(String email);

    /**
     * Cierra la sesión del token: deja de ser aceptado aunque no haya vencido.
     * Un token inválido o vencido se ignora.
     *
     * @param token token de la sesión
     */
    void cerrarSesion(String token);

    /**
     * Olvida las sesiones cerradas cuyos tokens ya vencieron.
     *
     * @return cantidad de sesiones borradas
     */
    int depurarSesionesCerradas();

    /**
     * Cifra y guarda una nueva contraseña para el usuario.
     *
     * @param usuarioId identificador del usuario
     * @param password  contraseña nueva en texto plano
     */
    void actualizarPassword(Long usuarioId, String password);

    /**
     * Realiza el registro de un nuevo usuario en el sistema, devuelve true si el
     * usuario se registró correctamente, de lo contrario, false o una excepción.
     *
     * @param registerRequest Objeto que contiene la información del usuario a
     *                        registrar.
     * @return true si el registro fue exitoso, de lo contrario, false o una
     *         excepción.
     */
    boolean register(Usuario registerRequest);
}
