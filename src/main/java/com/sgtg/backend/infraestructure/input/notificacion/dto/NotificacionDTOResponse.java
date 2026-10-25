package com.sgtg.backend.infraestructure.input.notificacion.dto;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.models.notificacion.EventoTrabajoGrado;
import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.domain.models.notificacion.TipoNotificacion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;

/**
 * Notificación del buzón del usuario autenticado. {@code evento} es el hecho que
 * la generó (ej. {@code DECISION_COMITE}); con la etapa, el front decide a qué
 * vista llevar. Es null en las notificaciones creadas antes de registrarlo.
 */
public record NotificacionDTOResponse(Long id, TipoNotificacion tipo, String titulo, String mensaje, Long trabajoId,
        Proceso etapa, EventoTrabajoGrado evento, LocalDateTime fecha, boolean leida) {

    public static NotificacionDTOResponse desde(Notificacion n) {
        return new NotificacionDTOResponse(n.id(), n.tipo(), n.titulo(), n.mensaje(), n.trabajoId(), n.etapa(), n.evento(),
                n.fecha(), n.leida());
    }
}
