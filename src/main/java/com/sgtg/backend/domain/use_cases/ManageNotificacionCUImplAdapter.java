package com.sgtg.backend.domain.use_cases;

import java.util.List;

import com.sgtg.backend.application.input.notificacion.ManageNotificacionCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.domain.models.notificacion.Notificacion;

/** Consulta y gestión del buzón: cada usuario solo accede a sus notificaciones. */
public class ManageNotificacionCUImplAdapter implements ManageNotificacionCUIntPort {

    private final ManageNotificacionGatewayIntPort notificacionGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageNotificacionCUImplAdapter(ManageNotificacionGatewayIntPort notificacionGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        this.notificacionGateway = notificacionGateway;
        this.exceptionFormatter = exceptionFormatter;
    }

    @Override
    public List<Notificacion> listarMias(Long usuarioId) {
        return this.notificacionGateway.listarPorUsuario(usuarioId);
    }

    @Override
    public Notificacion marcarLeida(Long notificacionId, Long usuarioId) {
        Notificacion notificacion = cargarPropia(notificacionId, usuarioId);
        return notificacion.leida() ? notificacion : this.notificacionGateway.guardar(notificacion.leer());
    }

    @Override
    public void marcarTodasLeidas(Long usuarioId) {
        this.notificacionGateway.marcarTodasLeidas(usuarioId);
    }

    @Override
    public void eliminar(Long notificacionId, Long usuarioId) {
        cargarPropia(notificacionId, usuarioId);
        this.notificacionGateway.eliminar(notificacionId);
    }

    @Override
    public void eliminarTodas(Long usuarioId) {
        this.notificacionGateway.eliminarPorUsuario(usuarioId);
    }

    private Notificacion cargarPropia(Long notificacionId, Long usuarioId) {
        Notificacion notificacion = this.notificacionGateway.buscarPorId(notificacionId);
        if (notificacion == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.notificacion.no_encontrada");
        if (!notificacion.esDe(usuarioId))
            this.exceptionFormatter.returnNoAccess("error.notificacion.sin_acceso");
        return notificacion;
    }
}
