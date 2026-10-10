package com.sgtg.backend.infraestructure.configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Reloj de la aplicación: el del sistema más un desplazamiento que solo se
 * modifica en desarrollo (ver {@code RelojController}, activo con
 * {@code sgtg.reloj.ajustable=true}). Permite probar flujos que dependen del
 * paso del tiempo, como la sustentación o los vencimientos. En producción el
 * desplazamiento siempre es cero.
 */
public class RelojAjustable extends Clock {

    private final Clock base;
    private volatile Duration desplazamiento = Duration.ZERO;

    public RelojAjustable(Clock base) {
        this.base = base;
    }

    /** Adelanta el reloj la cantidad de días indicada. */
    public void avanzarDias(long dias) {
        this.desplazamiento = this.desplazamiento.plusDays(dias);
    }

    /** Vuelve a la fecha real. */
    public void restablecer() {
        this.desplazamiento = Duration.ZERO;
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
