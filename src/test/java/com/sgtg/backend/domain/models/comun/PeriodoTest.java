package com.sgtg.backend.domain.models.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

class PeriodoTest {

    private final LocalDate inicio = LocalDate.of(2026, 1, 15);

    @Test
    void creaPeriodoPorMeses() {
        Periodo periodo = Periodo.desde(inicio, 9);
        assertEquals(LocalDate.of(2026, 10, 15), periodo.fin());
    }

    @Test
    void rechazaFinAnteriorOIgualAlInicio() {
        assertThrows(ReglaNegocioException.class, () -> new Periodo(inicio, inicio));
        assertThrows(ReglaNegocioException.class, () -> new Periodo(inicio, inicio.minusDays(1)));
    }

    @Test
    void rechazaMesesNoPositivos() {
        assertThrows(ReglaNegocioException.class, () -> Periodo.desde(inicio, 0));
        assertThrows(ReglaNegocioException.class, () -> Periodo.desde(inicio, 9).extenderMeses(-1));
    }

    @Test
    void extenderConservaInicioYNoModificaElOriginal() {
        Periodo original = Periodo.desde(inicio, 9);
        Periodo extendido = original.extenderMeses(3);
        assertEquals(inicio, extendido.inicio());
        assertEquals(LocalDate.of(2027, 1, 15), extendido.fin());
        assertEquals(LocalDate.of(2026, 10, 15), original.fin());
    }

    @Test
    void diasRestantesCuentaDiasCalendario() {
        Periodo periodo = Periodo.desde(inicio, 9); // fin 2026-10-15
        assertEquals(15, periodo.diasRestantes(LocalDate.of(2026, 9, 30)));
        assertEquals(14, periodo.diasRestantes(LocalDate.of(2026, 10, 1)));
        assertEquals(0, periodo.diasRestantes(LocalDate.of(2026, 11, 1)));
    }

    @Test
    void suspenderYReanudarConservaElTiempoQueFaltaba() {
        // Se suspende a los 3 meses: faltan 6 meses (los días del 15-abr al 15-oct)
        Periodo periodo = Periodo.desde(inicio, 9);
        LocalDate suspension = LocalDate.of(2026, 4, 15);
        long restantes = periodo.diasRestantes(suspension);

        LocalDate reanudacion = LocalDate.of(2026, 8, 1);
        Periodo reanudado = periodo.reanudarDesde(reanudacion, restantes);

        assertEquals(inicio, reanudado.inicio());
        assertEquals(reanudacion.plusDays(restantes), reanudado.fin());
        assertEquals(restantes, reanudado.diasRestantes(reanudacion));
        assertThrows(ReglaNegocioException.class, () -> periodo.reanudarDesde(reanudacion, 0));
    }

    @Test
    void haTranscurridoRespetaElLimiteExacto() {
        Periodo periodo = Periodo.desde(inicio, 9);
        assertFalse(periodo.haTranscurrido(3, LocalDate.of(2026, 4, 14)));
        assertTrue(periodo.haTranscurrido(3, LocalDate.of(2026, 4, 15)));
    }

    @Test
    void vencidoYContieneIncluyenElUltimoDia() {
        Periodo periodo = Periodo.desde(inicio, 9);
        assertFalse(periodo.vencido(periodo.fin()));
        assertTrue(periodo.vencido(periodo.fin().plusDays(1)));
        assertTrue(periodo.contiene(inicio));
        assertTrue(periodo.contiene(periodo.fin()));
        assertFalse(periodo.contiene(inicio.minusDays(1)));
    }
}
