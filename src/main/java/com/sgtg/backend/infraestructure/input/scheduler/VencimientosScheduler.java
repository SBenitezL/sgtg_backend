package com.sgtg.backend.infraestructure.input.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que aplica los plazos del reglamento: reanuda las
 * suspensiones que llegaron a su fecha máxima y deja NO APROBADO los trabajos
 * en desarrollo cuyo plazo terminó sin iniciar la sustentación. Por defecto se ejecuta todos los días a las 00:05 (hora de
 * Colombia); se configura con {@code sgtg.vencimientos.cron}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VencimientosScheduler {

    private final ManageTrabajoGradoCUIntPort manageTrabajoGrado;

    @Scheduled(cron = "${sgtg.vencimientos.cron:0 5 0 * * *}", zone = "America/Bogota")
    public void verificarVencimientos() {
        try {
            int actualizados = manageTrabajoGrado.verificarVencimientos();
            log.info("Verificación de vencimientos: {} trabajo(s) reanudado(s) o cerrado(s)", actualizados);
        } catch (RuntimeException e) {
            log.error("Error en la verificación de vencimientos", e);
        }
    }
}
