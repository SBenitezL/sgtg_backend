package com.sgtg.backend.application.input.auth;

/**
 * Interfaz para la gestión de operaciones de autenticación. La creación de
 * usuarios la hace un administrador (ver ManageUsuarioCUIntPort).
 */
public interface ManageAuthCUIntPort {
    /**
     * Realiza el proceso de inicio de sesión para un usuario dado su correo
     * electrónico y contraseña. Tras varios intentos fallidos seguidos el correo
     * queda bloqueado temporalmente.
     *
     * @param email    correo electrónico del usuario, es obligatorio y debe cumplir
     *                 con formato de correo electrónico
     * @param password contraseña del usuario, es obligatoria
     * @return token de autenticación si el inicio de sesión es exitoso, o null si
     *         falla
     */
    public String login(String email, String password);

    /**
     * Cierra la sesión del token: deja de ser aceptado aunque no haya vencido.
     * Un token inválido o vencido se ignora.
     *
     * @param token token de la sesión
     */
    public void logout(String token);

    /**
     * Borra los registros de acceso que ya no sirven: intentos fallidos
     * antiguos y sesiones cerradas cuyos tokens vencieron.
     *
     * @return cantidad de registros borrados
     */
    public int depurarRegistrosDeAcceso();
}
