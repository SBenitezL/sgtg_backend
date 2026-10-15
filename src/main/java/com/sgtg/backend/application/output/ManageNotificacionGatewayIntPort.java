package com.sgtg.backend.application.output;

import java.util.List;

import com.sgtg.backend.domain.models.notificacion.Notificacion;

/** Persistencia de las notificaciones del buzón. */
public interface ManageNotificacionGatewayIntPort {

    public List<Notificacion> guardarTodas(List<Notificacion> notificaciones);

    public Notificacion guardar(Notificacion notificacion);

    /** @return la notificación o null si no existe */
    public Notificacion buscarPorId(Long id);

    /** @return las notificaciones del usuario, de la última creada a la primera */
    public List<Notificacion> listarPorUsuario(Long usuarioId);

    /** @return true si el usuario ya recibió un aviso con esa clave */
    public boolean existeClave(Long usuarioId, String clave);

    public void marcarTodasLeidas(Long usuarioId);

    public void eliminar(Long id);

    public void eliminarPorUsuario(Long usuarioId);
}
