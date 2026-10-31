package com.sgtg.backend.infraestructure.input.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sgtg.backend.application.input.seguridad.ManageSeguridadCUIntPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tareas programadas de seguridad (hora de Colombia):
 * <ul>
 * <li>detección de alertas cada 5 minutos ({@code sgtg.alertas.cron});</li>
 * <li>verificación de la cadena de la auditoría y de las huellas de los archivos
 * al arrancar y todos los días a las 03:30 ({@code sgtg.integridad.cron}). Al
 * arrancar, además, los archivos guardados antes de calcular huellas reciben
 * la suya.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeguridadScheduler {

    private final ManageSeguridadCUIntPort seguridad;

    @Scheduled(cron = "${sgtg.alertas.cron:0 */5 * * * *}", zone = "America/Bogota")
    public void detectarAlertas() {
        try {
            int nuevas = seguridad.detectarAlertas();
            if (nuevas > 0)
                log.warn("Detección de alertas: {} alerta(s) nueva(s)", nuevas);
        } catch (RuntimeException e) {
            log.error("Error en la detección de alertas de seguridad", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${sgtg.integridad.cron:0 30 3 * * *}", zone = "America/Bogota")
    public void verificarIntegridad() {
        try {
            seguridad.verificarIntegridad();
        } catch (RuntimeException e) {
            log.error("Error en la verificación de integridad", e);
        }
    }
}
