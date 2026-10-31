package com.sgtg.backend.infraestructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Paso de migración de producción ({@code sgtg.solo-migrar=true}): la
 * aplicación arranca con el usuario administrador de la base y
 * {@code ddl-auto=update}, crea o ajusta el esquema, pasa los datos
 * ({@link MigracionCredenciales}), protege la auditoría
 * ({@link ProteccionAuditoria}), crea el administrador inicial y termina. Así
 * la aplicación corre después con {@code ddl-auto=validate} y un usuario sin
 * permisos de esquema. Va de último, después de los demás pasos de arranque.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class SoloMigrar implements CommandLineRunner {

    private final ApplicationContext contexto;

    @Value("${sgtg.solo-migrar:false}")
    private boolean soloMigrar;

    @Override
    public void run(String... args) {
        if (!soloMigrar)
            return;
        log.info("Migración terminada: el esquema y los datos quedaron al día");
        System.exit(SpringApplication.exit(contexto, () -> 0));
    }
}
