package com.sgtg.backend.infraestructure.input.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tarea programada que borra los registros de acceso que ya no sirven:
 * intentos fallidos de inicio de sesión antiguos y sesiones cerradas cuyos
 * tokens ya vencieron. Por defecto se ejecuta todos los días a las 03:00 (hora
 * de Colombia); se configura con {@code sgtg.limpieza-accesos.cron}. La
 * auditoría no se toca.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LimpiezaAccesosScheduler {

    private final ManageAuthCUIntPort manageAuth;

    @Scheduled(cron = "${sgtg.limpieza-accesos.cron:0 0 3 * * *}", zone = "America/Bogota")
    public void depurar() {
        try {
            log.info("Limpieza de accesos: {} registro(s) borrado(s)", manageAuth.depurarRegistrosDeAcceso());
        } catch (RuntimeException e) {
            log.error("Error en la limpieza de accesos", e);
        }
    }
}
