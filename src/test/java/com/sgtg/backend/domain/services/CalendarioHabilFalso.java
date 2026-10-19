package com.sgtg.backend.domain.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Calendario para pruebas: considera hábiles los días de lunes a viernes, sin
 * festivos.
 */
public class CalendarioHabilFalso implements CalendarioHabil {

    private static final Set<DayOfWeek> FIN_DE_SEMANA = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    @Override
    public boolean esHabil(LocalDate fecha) {
        return !FIN_DE_SEMANA.contains(fecha.getDayOfWeek());
    }
}
