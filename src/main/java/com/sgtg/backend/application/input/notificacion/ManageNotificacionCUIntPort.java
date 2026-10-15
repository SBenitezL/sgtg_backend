package com.sgtg.backend.application.input.notificacion;

import java.util.List;

import com.sgtg.backend.domain.models.notificacion.Notificacion;

/** Buzón de notificaciones del usuario autenticado. */
public interface ManageNotificacionCUIntPort {

    /** @return las notificaciones del usuario, de la más reciente a la más antigua */
    public List<Notificacion> listarMias(Long usuarioId);

    /** Marca como leída una notificación del usuario. */
    public Notificacion marcarLeida(Long notificacionId, Long usuarioId);

    public void marcarTodasLeidas(Long usuarioId);

    /** Elimina una notificación del usuario. */
    public void eliminar(Long notificacionId, Long usuarioId);

    public void eliminarTodas(Long usuarioId);
}
