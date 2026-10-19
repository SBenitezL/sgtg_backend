package com.sgtg.backend.infraestructure.output.calendario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgtg.backend.infraestructure.output.percistence.entities.DiaNoHabilEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.DiaNoHabilRepository;

/** Calendario de la universidad: fines de semana, festivos de ley y días institucionales en memoria. */
class CalendarioHabilImplAdapterTest {

    /** Lunes festivo (Epifanía trasladada) */
    private static final LocalDate EPIFANIA = LocalDate.of(2026, 1, 12);
    /** Viernes declarado no hábil por la universidad */
    private static final LocalDate DIA_INSTITUCIONAL = LocalDate.of(2026, 1, 16);

    private DiaNoHabilRepository repositorio;
    private CalendarioHabilImplAdapter calendario;

    @BeforeEach
    void crear() {
        repositorio = mock(DiaNoHabilRepository.class);
        when(repositorio.findAll()).thenReturn(List.of(new DiaNoHabilEntity(DIA_INSTITUCIONAL, "Semana universitaria")));
        calendario = new CalendarioHabilImplAdapter(repositorio);
    }

    @Test
    void excluyeFinesDeSemanaFestivosYDiasInstitucionales() {
        assertFalse(calendario.esHabil(LocalDate.of(2026, 1, 10)));
        assertFalse(calendario.esHabil(EPIFANIA));
        assertFalse(calendario.esHabil(DIA_INSTITUCIONAL));
        assertTrue(calendario.esHabil(LocalDate.of(2026, 1, 13)));
    }

    @Test
    void losPlazosSaltanLosFestivos() {
        // Del viernes 9 de enero, un día hábil es el martes 13 (el lunes 12 es festivo)
        assertEquals(LocalDate.of(2026, 1, 13), calendario.sumarDiasHabiles(LocalDate.of(2026, 1, 9), 1));
        // Del viernes 9 al lunes 19 quedan 4 días hábiles: 13, 14, 15 y 19
        assertEquals(4, calendario.contarDiasHabiles(LocalDate.of(2026, 1, 9), LocalDate.of(2026, 1, 19)));
    }

    @Test
    void consultaLosDiasInstitucionalesUnaSolaVez() {
        for (int dia = 1; dia <= 31; dia++)
            calendario.esHabil(LocalDate.of(2026, 1, dia));
        calendario.sumarDiasHabiles(LocalDate.of(2026, 1, 2), 40);
        verify(repositorio, times(1)).findAll();
    }

    @Test
    void alInvalidarRecargaLosDiasInstitucionales() {
        LocalDate nuevo = LocalDate.of(2026, 1, 20);
        assertTrue(calendario.esHabil(nuevo));

        when(repositorio.findAll()).thenReturn(List.of(new DiaNoHabilEntity(DIA_INSTITUCIONAL, "Semana universitaria"),
                new DiaNoHabilEntity(nuevo, "Paro")));
        calendario.invalidar();

        assertFalse(calendario.esHabil(nuevo));
        verify(repositorio, times(2)).findAll();
    }
}
