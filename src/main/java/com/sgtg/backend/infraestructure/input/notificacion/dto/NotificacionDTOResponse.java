package com.sgtg.backend.infraestructure.input.notificacion.dto;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.domain.models.notificacion.TipoNotificacion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;

/** Notificación del buzón del usuario autenticado. */
public record NotificacionDTOResponse(Long id, TipoNotificacion tipo, String titulo, String mensaje, Long trabajoId,
        Proceso etapa, LocalDateTime fecha, boolean leida) {

    public static NotificacionDTOResponse desde(Notificacion n) {
        return new NotificacionDTOResponse(n.id(), n.tipo(), n.titulo(), n.mensaje(), n.trabajoId(), n.etapa(),
                n.fecha(), n.leida());
    }
}
