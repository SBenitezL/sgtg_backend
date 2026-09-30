package com.sgtg.backend.domain.models.trabajo_grado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ProcesoTest {

    @ParameterizedTest
    @CsvSource({
            "PROPUESTA, ANTEPROYECTO",
            "ANTEPROYECTO, EVALUACION",
            "EVALUACION, ANTEPROYECTO",
            "EVALUACION, APROBACION_CONSEJO",
            "APROBACION_CONSEJO, EN_CURSO",
            "APROBACION_CONSEJO, ANTEPROYECTO",
            "EN_CURSO, PRORROGA",
            "EN_CURSO, SUSPENDIDO",
            "EN_CURSO, SUSTENTACION",
            "EN_CURSO, NO_APROBADO",
            "PRORROGA, SUSPENDIDO",
            "PRORROGA, SUSTENTACION",
            "PRORROGA, NO_APROBADO",
            "SUSPENDIDO, EN_CURSO",
            "SUSPENDIDO, PRORROGA",
            "SUSTENTACION, FINALIZADO",
            "SUSTENTACION, APLAZADO",
            "SUSTENTACION, NO_APROBADO",
            "APLAZADO, SUSTENTACION",
            "APLAZADO, NO_APROBADO"
    })
    void permiteTransicionesValidas(Proceso origen, Proceso destino) {
        assertTrue(origen.puedeAvanzarA(destino));
    }

    @ParameterizedTest
    @CsvSource({
            "PROPUESTA, PROPUESTA",
            "PROPUESTA, EVALUACION",
            "PROPUESTA, NO_APROBADO",
            "ANTEPROYECTO, APROBACION_CONSEJO",
            "EVALUACION, EN_CURSO",
            "APROBACION_CONSEJO, NO_APROBADO",
            "EN_CURSO, ANTEPROYECTO",
            "EN_CURSO, FINALIZADO",
            "PRORROGA, EN_CURSO",
            "PRORROGA, PRORROGA",
            "SUSPENDIDO, SUSTENTACION",
            "SUSPENDIDO, NO_APROBADO",
            "SUSTENTACION, EN_CURSO",
            "SUSTENTACION, PROPUESTA",
            "APLAZADO, FINALIZADO"
    })
    void rechazaTransicionesInvalidas(Proceso origen, Proceso destino) {
        assertFalse(origen.puedeAvanzarA(destino));
    }

    @ParameterizedTest
    @EnumSource(value = Proceso.class, names = { "FINALIZADO", "NO_APROBADO", "CANCELADO" },
            mode = EnumSource.Mode.EXCLUDE)
    void cualquierEtapaActivaPuedeCancelarse(Proceso origen) {
        assertFalse(origen.esFinal());
        assertTrue(origen.puedeAvanzarA(Proceso.CANCELADO));
    }

    @ParameterizedTest
    @EnumSource(value = Proceso.class, names = { "FINALIZADO", "NO_APROBADO", "CANCELADO" })
    void etapasFinalesNoAdmitenTransiciones(Proceso origen) {
        assertTrue(origen.esFinal());
        for (Proceso destino : Proceso.values())
            assertFalse(origen.puedeAvanzarA(destino));
    }

    @Test
    void noPermiteAvanzarANulo() {
        assertFalse(Proceso.PROPUESTA.puedeAvanzarA(null));
    }

    @Test
    void motivoCierreDeterminaLaEtapaFinal() {
        assertEquals(Proceso.NO_APROBADO, MotivoCierre.VENCIMIENTO_PLAZO.getEtapaCierre());
        assertEquals(Proceso.NO_APROBADO, MotivoCierre.INASISTENCIA_SUSTENTACION.getEtapaCierre());
        assertEquals(Proceso.CANCELADO, MotivoCierre.CANCELACION_DIRECTOR.getEtapaCierre());
        for (MotivoCierre motivo : MotivoCierre.values())
            assertTrue(motivo.getEtapaCierre().esFinal());
    }
}
