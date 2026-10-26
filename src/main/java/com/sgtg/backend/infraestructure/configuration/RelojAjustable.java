package com.sgtg.backend.infraestructure.configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import lombok.extern.slf4j.Slf4j;

/**
 * Reloj de la aplicación: el del sistema más un desplazamiento que solo se
 * modifica en desarrollo (ver {@code RelojController}, activo con
 * {@code sgtg.reloj.ajustable=true}). Permite probar flujos que dependen del
 * paso del tiempo, como la sustentación o los vencimientos. En producción el
 * desplazamiento siempre es cero.
 *
 * En desarrollo el desplazamiento se guarda en un archivo para que sobreviva a
 * los reinicios: si al reiniciar volviera a la fecha real, el tiempo de la
 * aplicación retrocedería y las fechas ya registradas (decisiones, plazos)
 * quedarían en el futuro.
 */
@Slf4j
public class RelojAjustable extends Clock {

    private final Clock base;
    /** Archivo con los días de desplazamiento; null si no se guarda (producción) */
    private final Path archivo;
    private volatile Duration desplazamiento;

    public RelojAjustable(Clock base) {
        this(base, null);
    }

    public RelojAjustable(Clock base, Path archivo) {
        this.base = base;
        this.archivo = archivo;
        this.desplazamiento = Duration.ofDays(leerDias());
    }

    /** Adelanta el reloj la cantidad de días indicada. */
    public void avanzarDias(long dias) {
        this.desplazamiento = this.desplazamiento.plusDays(dias);
        guardar();
    }

    /** Vuelve a la fecha real. */
    public void restablecer() {
        this.desplazamiento = Duration.ZERO;
        guardar();
    }

    private long leerDias() {
        if (this.archivo == null || !Files.exists(this.archivo))
            return 0;
        try {
            return Long.parseLong(Files.readString(this.archivo).trim());
        } catch (IOException | NumberFormatException e) {
            log.warn("No se pudo leer el desplazamiento del reloj de {}; se usa la fecha real", this.archivo, e);
            return 0;
        }
    }

    private void guardar() {
        if (this.archivo == null)
            return;
        try {
            Files.createDirectories(this.archivo.toAbsolutePath().getParent());
            Files.writeString(this.archivo, String.valueOf(getDesplazamientoDias()));
        } catch (IOException e) {
            log.warn("No se pudo guardar el desplazamiento del reloj en {}", this.archivo, e);
        }
    }

    /** @return días que el reloj está adelantado respecto a la fecha real */
    public long getDesplazamientoDias() {
        return this.desplazamiento.toDays();
    }

    @Override
    public ZoneId getZone() {
        return this.base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return Clock.offset(this.base.withZone(zona), this.desplazamiento);
    }

    @Override
    public Instant instant() {
        return this.base.instant().plus(this.desplazamiento);
    }
}
