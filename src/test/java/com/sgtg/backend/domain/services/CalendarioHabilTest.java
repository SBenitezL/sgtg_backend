package com.sgtg.backend.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Suma y conteo de días hábiles (métodos por defecto de {@link CalendarioHabil}). */
class CalendarioHabilTest {

    private static final LocalDate VIERNES = LocalDate.of(2026, 3, 6);
    private static final LocalDate MARTES = LocalDate.of(2026, 3, 10);

    private final CalendarioHabil calendario = new CalendarioHabilFalso();

    @Test
    void sumarSaltaLosDiasNoHabiles() {
        assertEquals(LocalDate.of(2026, 3, 9), calendario.sumarDiasHabiles(VIERNES, 1));
        assertEquals(MARTES, calendario.sumarDiasHabiles(VIERNES, 2));
    }

    @Test
    void elMismoDiaNoTieneDiasRestantes() {
        assertEquals(0, calendario.contarDiasHabiles(MARTES, MARTES));
    }

    @Test
    void cuentaLosDiasHabilesQueFaltanSinElDeHoy() {
        // Del viernes al martes faltan el lunes y el martes; el fin de semana no cuenta
        assertEquals(2, calendario.contarDiasHabiles(VIERNES, MARTES));
    }

    @Test
    void unPlazoVencidoDaLosDiasHabilesDeRetrasoEnNegativo() {
        assertEquals(-2, calendario.contarDiasHabiles(MARTES, VIERNES));
    }

    @Test
    void contarDesdeElInicioDeUnPlazoDaLosDiasDelPlazo() {
        LocalDate limite = calendario.sumarDiasHabiles(VIERNES, 10);
        assertEquals(10, calendario.contarDiasHabiles(VIERNES, limite));
    }
}
