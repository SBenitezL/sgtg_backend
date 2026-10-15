package com.sgtg.backend.domain.models.notificacion;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Mensaje del buzón de un usuario sobre un trabajo de grado.
 *
 * @param id        identificador, null mientras no se haya persistido
 * @param usuarioId usuario que la recibe
 * @param tipo      relevancia del mensaje
 * @param titulo    título corto
 * @param mensaje   texto del mensaje
 * @param trabajoId trabajo de grado al que se refiere
 * @param etapa     etapa del trabajo al notificar (el front decide a qué vista llevar)
 * @param clave     identifica un aviso que no se debe repetir (ej. un plazo por
 *                  vencer); null en las demás
 * @param fecha     fecha y hora de creación
 * @param leida     true si el usuario ya la leyó
 */
public record Notificacion(Long id, Long usuarioId, TipoNotificacion tipo, String titulo, String mensaje,
        Long trabajoId, Proceso etapa, String clave, LocalDateTime fecha, boolean leida) {

    public Notificacion {
        if (usuarioId == null || tipo == null || CheckString.isBlank(titulo) || CheckString.isBlank(mensaje)
                || fecha == null)
            throw new ReglaNegocioException("error.dominio.notificacion.datos_incompletos");
    }

    /** Crea una notificación sin leer. */
    public static Notificacion nueva(Long usuarioId, TipoNotificacion tipo, String titulo, String mensaje,
            Long trabajoId, Proceso etapa, String clave, LocalDateTime fecha) {
        return new Notificacion(null, usuarioId, tipo, titulo, mensaje, trabajoId, etapa, clave, fecha, false);
    }

    /** @return la notificación marcada como leída */
    public Notificacion leer() {
        return new Notificacion(id, usuarioId, tipo, titulo, mensaje, trabajoId, etapa, clave, fecha, true);
    }

    /** @return true si la notificación es del usuario */
    public boolean esDe(Long usuario) {
        return this.usuarioId.equals(usuario);
    }
}
