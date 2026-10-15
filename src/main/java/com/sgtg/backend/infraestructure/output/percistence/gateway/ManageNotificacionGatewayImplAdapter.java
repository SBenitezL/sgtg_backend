package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.infraestructure.output.percistence.entities.NotificacionEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.NotificacionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageNotificacionGatewayImplAdapter implements ManageNotificacionGatewayIntPort {

    private final NotificacionRepository notificacionRepository;

    @Override
    public List<Notificacion> guardarTodas(List<Notificacion> notificaciones) {
        return notificacionRepository.saveAll(notificaciones.stream().map(this::aEntidad).toList()).stream()
                .map(this::aDominio).toList();
    }

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        return aDominio(notificacionRepository.save(aEntidad(notificacion)));
    }

    @Override
    public Notificacion buscarPorId(Long id) {
        return id == null ? null : notificacionRepository.findById(id).map(this::aDominio).orElse(null);
    }

    @Override
    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByIdDesc(usuarioId).stream()
                .map(this::aDominio).toList();
    }

    @Override
    public boolean existeClave(Long usuarioId, String clave) {
        return notificacionRepository.existsByUsuarioIdAndClave(usuarioId, clave);
    }

    @Override
    public void marcarTodasLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasLeidas(usuarioId);
    }

    @Override
    public void eliminar(Long id) {
        notificacionRepository.deleteById(id);
    }

    @Override
    public void eliminarPorUsuario(Long usuarioId) {
        notificacionRepository.eliminarPorUsuario(usuarioId);
    }

    private NotificacionEntity aEntidad(Notificacion n) {
        return new NotificacionEntity(n.id(), n.usuarioId(), n.tipo(), n.titulo(), n.mensaje(), n.trabajoId(),
                n.etapa(), n.clave(), n.fecha(), n.leida());
    }

    private Notificacion aDominio(NotificacionEntity e) {
        return new Notificacion(e.getId(), e.getUsuarioId(), e.getTipo(), e.getTitulo(), e.getMensaje(),
                e.getTrabajoId(), e.getEtapa(), e.getClave(), e.getFecha(), e.isLeida());
    }
}
