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
     * @return los tokens de la sesión nueva
     */
    public TokensSesion login(String email, String password);

    /**
     * Renueva la sesión con su token de renovación. Si no sirve (sesión cerrada,
     * vencida o reutilizada) responde no autenticado.
     *
     * @return token de acceso nuevo y el de renovación que reemplaza al presentado
     */
    public TokensSesion renovar(String tokenRenovacion);

    /**
     * Cierra la sesión del token de acceso o, si ese ya venció, la del token de
     * renovación: sus tokens dejan de ser aceptados. Los tokens inválidos se
     * ignoran.
     */
    public void logout(String tokenAcceso, String tokenRenovacion);

    /**
     * Borra los registros de acceso que ya no sirven: intentos fallidos
     * antiguos y sesiones que ya no pueden usarse.
     *
     * @return cantidad de registros borrados
     */
    public int depurarRegistrosDeAcceso();
}
