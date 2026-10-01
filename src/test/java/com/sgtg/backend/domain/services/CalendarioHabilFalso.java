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
    public LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate fecha = desde;
        int contados = 0;
        while (contados < dias) {
            fecha = fecha.plusDays(1);
            if (!FIN_DE_SEMANA.contains(fecha.getDayOfWeek()))
                contados++;
        }
        return fecha;
    }
}
