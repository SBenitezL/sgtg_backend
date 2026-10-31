package com.sgtg.backend.application.output;

import com.sgtg.backend.application.input.auth.TokensSesion;
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
     * Abre una sesión nueva para el usuario: su token de acceso (corto, con sus
     * roles) y el de renovación.
     *
     * @param email Correo electrónico del usuario, debe existir.
     */
    TokensSesion iniciarSesion(String email);

    /**
     * Renueva la sesión del token de renovación: un token de acceso nuevo y otro
     * de renovación (el presentado deja de servir). Si el token ya se había
     * usado, la sesión se cierra y se avisa a los administradores.
     *
     * @return null si el token no sirve (sesión cerrada, vencida o reutilizada,
     *         usuario inactivo o contraseña cambiada)
     */
    TokensSesion renovarSesion(String tokenRenovacion);

    /**
     * Cierra la sesión del token de acceso o, si ese ya venció, la del token de
     * renovación. Los tokens inválidos se ignoran.
     */
    void cerrarSesion(String tokenAcceso, String tokenRenovacion);

    /** Cierra todas las sesiones abiertas del usuario (ej. al inactivarlo). */
    void cerrarSesionesDe(Long usuarioId);

    /**
     * Olvida las sesiones que ya no pueden usarse.
     *
     * @return cantidad de sesiones borradas
     */
    int depurarSesiones();

    /**
     * @param usuarioId identificador del usuario
     * @param password  contraseña en texto plano
     * @return true si es la contraseña actual del usuario o una de las últimas
     *         que tuvo ({@link com.sgtg.backend.domain.models.acceso.PoliticaPassword#CONTRASENAS_RECORDADAS})
     */
    boolean esPasswordReciente(Long usuarioId, String password);

    /**
     * El usuario cambia su propia contraseña: se cifra y se guarda, y sus demás
     * sesiones se cierran.
     *
     * @param usuarioId identificador del usuario
     * @param password  contraseña nueva en texto plano
     * @param sesion    sesión desde la que la cambió, que sigue abierta
     * @return token de acceso nuevo para esa sesión
     */
    String cambiarPassword(Long usuarioId, String password, String sesion);

    /**
     * Un administrador asigna una contraseña al usuario: se cifra y se guarda,
     * las sesiones del usuario se cierran y debe cambiarla en su siguiente
     * ingreso.
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
