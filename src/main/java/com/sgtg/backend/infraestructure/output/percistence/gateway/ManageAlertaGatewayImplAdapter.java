package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageAlertaGatewayIntPort;
import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.AlertaSeguridad;
import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.domain.models.notificacion.TipoNotificacion;
import com.sgtg.backend.infraestructure.output.percistence.entities.AlertaSeguridadEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity.RoleType;
import com.sgtg.backend.infraestructure.output.percistence.repositories.AlertaSeguridadRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Guarda las alertas de seguridad en {@code alertas_seguridad}, las escribe en
 * el log y las avisa en el buzón de cada administrador activo. Una alerta del
 * mismo tipo y clave no se repite antes de {@link #SIN_REPETIR}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageAlertaGatewayImplAdapter implements ManageAlertaGatewayIntPort {

    static final Duration SIN_REPETIR = Duration.ofHours(24);
    private static final int LONGITUD_CLAVE = 255;
    private static final int LONGITUD_DETALLE = 1000;

    private final AlertaSeguridadRepository alertaRepository;
    private final UserRepository userRepository;
    private final ManageNotificacionGatewayIntPort notificaciones;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean alertar(AlertaSeguridad alerta) {
        String clave = recortar(alerta.clave(), LONGITUD_CLAVE);
        if (alertaRepository.existsByTipoAndClaveAndFechaAfter(alerta.tipo(), clave,
                alerta.fecha().minus(SIN_REPETIR)))
            return false;
        String detalle = recortar(alerta.detalle(), LONGITUD_DETALLE);
        AlertaSeguridadEntity guardada = alertaRepository
                .save(new AlertaSeguridadEntity(null, alerta.fecha(), alerta.tipo(), clave, detalle));
        log.warn("ALERTA DE SEGURIDAD [{}] {}", alerta.tipo(), detalle);
        // Sin trabajo de grado: en el buzón no lleva a ninguna vista
        List<Notificacion> avisos = userRepository.activosConRol(RoleType.ADMINISTRADOR).stream()
                .map(admin -> Notificacion.nueva(admin.getCusuari(), TipoNotificacion.ERROR,
                        "Alerta de seguridad: " + alerta.tipo().getTitulo(), detalle, null, null, null,
                        "seguridad:" + guardada.getId(), alerta.fecha()))
                .toList();
        notificaciones.guardarTodas(avisos);
        return true;
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
