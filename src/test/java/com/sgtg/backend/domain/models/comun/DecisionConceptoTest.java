package com.sgtg.backend.domain.models.comun;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Evaluador;

class DecisionConceptoTest {

    private final LocalDate hoy = LocalDate.of(2026, 3, 1);

    @Test
    void decisionNoAprobatoriaExigeObservaciones() {
        assertThrows(ReglaNegocioException.class,
                () -> new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.RECHAZADO, "Acta 12", null, " ", hoy));
        assertDoesNotThrow(() -> new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, null, null, hoy));
    }

    @Test
    void decisionExigeOrganoResultadoYFecha() {
        assertThrows(ReglaNegocioException.class,
                () -> new Decision(null, ResultadoConcepto.APROBADO, null, null, null, hoy));
        assertThrows(ReglaNegocioException.class,
                () -> new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, null, null, null));
    }

    @Test
    void decisionIdentificaOrganoYResultado() {
        Decision decision = new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, "Res. 100", null, null, hoy);
        assertTrue(decision.esAprobatoria());
        assertTrue(decision.emitidaPor(Organo.CONSEJO_FACULTAD));
        assertFalse(decision.emitidaPor(Organo.COMITE_PROGRAMA));
    }

    @Test
    void conceptoAceptaCualquierParticipanteComoEmisor() {
        Evaluador evaluador = new Evaluador(1L, "D001", new NombrePersona("Ana", null, "Pérez", null), "ana@unicauca.edu.co");
        Asesor asesor = new Asesor(2L, "A001", new NombrePersona("Luis", null, "Mora", null), "luis@empresa.com", "Empresa S.A.S.", true);

        Concepto correcciones = new Concepto(evaluador, ResultadoConcepto.CON_CORRECCIONES, "Ajustar objetivos", hoy);
        assertTrue(correcciones.requiereCorrecciones());
        assertFalse(correcciones.esAprobado());
        assertTrue(new Concepto(asesor, ResultadoConcepto.APROBADO, null, hoy).esAprobado());
    }

    @Test
    void conceptoNoAprobatorioExigeObservaciones() {
        Evaluador evaluador = new Evaluador(1L, "D001", new NombrePersona("Ana", null, "Pérez", null), "ana@unicauca.edu.co");
        assertThrows(ReglaNegocioException.class,
                () -> new Concepto(evaluador, ResultadoConcepto.CON_CORRECCIONES, "", hoy));
        assertThrows(ReglaNegocioException.class,
                () -> new Concepto(null, ResultadoConcepto.APROBADO, null, hoy));
    }
}
