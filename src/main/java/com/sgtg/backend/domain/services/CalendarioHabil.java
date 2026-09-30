package com.sgtg.backend.domain.services;

import java.time.LocalDate;

/**
 * Calendario de días hábiles de la universidad. El dominio lo usa para
 * calcular los plazos expresados en días hábiles; la implementación (con los
 * festivos de Colombia) pertenece a la infraestructura.
 */
public interface CalendarioHabil {

    /**
     * Calcula la fecha que resulta de sumar días hábiles a una fecha.
     *
     * @param desde fecha desde la que se cuenta, no se incluye
     * @param dias  cantidad de días hábiles a sumar
     * @return fecha del último día hábil del plazo
     */
    LocalDate sumarDiasHabiles(LocalDate desde, int dias);
}
