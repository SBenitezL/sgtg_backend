package com.sgtg.backend.domain.models.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

/** Períodos académicos: enero a junio y julio a diciembre. */
class PeriodoAcademicoTest {

    @Test
    void identificaElPeriodoDeUnaFecha() {
        assertEquals(new PeriodoAcademico(2026, 1), PeriodoAcademico.de(LocalDate.of(2026, 6, 30)));
        assertEquals(new PeriodoAcademico(2026, 2), PeriodoAcademico.de(LocalDate.of(2026, 7, 1)));
        assertEquals("2026-2", PeriodoAcademico.de(LocalDate.of(2026, 12, 31)).toString());
    }

    @Test
    void conoceSusLimitesYElSiguiente() {
        PeriodoAcademico segundo = new PeriodoAcademico(2026, 2);
        assertEquals(LocalDate.of(2026, 7, 1), segundo.inicio());
        assertEquals(LocalDate.of(2026, 12, 31), segundo.fin());
        assertEquals(new PeriodoAcademico(2027, 1), segundo.siguiente());
        assertEquals(LocalDate.of(2027, 6, 30), segundo.siguiente().fin());
        assertEquals(new PeriodoAcademico(2027, 2), segundo.siguiente().siguiente());
    }

    @Test
    void soloHayDosSemestres() {
        assertThrows(ReglaNegocioException.class, () -> new PeriodoAcademico(2026, 3));
        assertThrows(ReglaNegocioException.class, () -> PeriodoAcademico.de(null));
    }
}
