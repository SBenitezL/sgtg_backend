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
     * @param usuarioId identificador del usuario
     * @param password  contraseña en texto plano
     * @return true si es la contraseña actual del usuario o una de las últimas
     *         que tuvo ({@link com.sgtg.backend.domain.models.acceso.PoliticaPassword#CONTRASENAS_RECORDADAS})
     */
    boolean esPasswordReciente(Long usuarioId, String password);

    /**
     * El usuario cambia su propia contraseña: se cifra y se guarda, y los tokens
     * emitidos antes (sus demás sesiones) dejan de servir.
     *
     * @param usuarioId identificador del usuario
     * @param password  contraseña nueva en texto plano
     * @return token nuevo, para que siga en la sesión desde la que la cambió
     */
    String cambiarPassword(Long usuarioId, String password);

    /**
     * Un administrador asigna una contraseña al usuario: se cifra y se guarda,
     * los tokens emitidos antes dejan de servir y el usuario debe cambiarla en
     * su siguiente ingreso.
     *
     * @param usuarioId identificador del usuario
     * @param password  contraseña nueva en texto plano
     */
    void asignarPassword(Long usuarioId, String password);

    /**
     * Registra un usuario nuevo con sus credenciales. Como la contraseña la
     * asigna un administrador, el usuario debe cambiarla en su primer ingreso.
     *
     * @param registerRequest datos del usuario
     * @param password        contraseña inicial en texto plano
     * @return true si el registro fue exitoso, de lo contrario, false o una
     *         excepción.
     */
    boolean register(Usuario registerRequest, String password);
}
