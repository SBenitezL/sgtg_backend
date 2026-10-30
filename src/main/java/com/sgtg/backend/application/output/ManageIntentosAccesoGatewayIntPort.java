package com.sgtg.backend.application.output;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.models.acceso.IntentosAcceso;

/** Puerto de salida de los inicios de sesión fallidos por correo (bloqueo por intentos). */
public interface ManageIntentosAccesoGatewayIntPort {

    /**
     * @param correo correo en minúsculas
     * @return los intentos registrados del correo, o sin fallos si no tiene registro
     */
    IntentosAcceso obtener(String correo);

    void guardar(IntentosAcceso intentos);

    /** Borra el registro del correo (tras un inicio de sesión exitoso). */
    void eliminar(String correo);

    /**
     * Borra los registros cuyo último fallo y bloqueo son anteriores a la fecha:
     * ya no cuentan para ningún bloqueo.
     *
     * @return cantidad de registros borrados
     */
    int depurar(LocalDateTime antesDe);
}
