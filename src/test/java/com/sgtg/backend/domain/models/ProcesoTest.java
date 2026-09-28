package com.sgtg.backend.domain.models;

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
            "EVALUACION, EN_CURSO",
            "EN_CURSO, SUSTENTACION",
            "EN_CURSO, PRORROGA",
            "PRORROGA, SUSTENTACION",
            "SUSTENTACION, FINALIZADO",
            "SUSTENTACION, CORRECCIONES_SUSTENTACION",
            "SUSTENTACION, PROPUESTA",
            "CORRECCIONES_SUSTENTACION, SUSTENTACION"
    })
    void permiteTransicionesValidas(Proceso origen, Proceso destino) {
        assertTrue(origen.puedeAvanzarA(destino));
    }

    @ParameterizedTest
    @CsvSource({
            "PROPUESTA, PROPUESTA",
            "PROPUESTA, EVALUACION",
            "ANTEPROYECTO, PROPUESTA",
            "ANTEPROYECTO, EN_CURSO",
            "EVALUACION, PROPUESTA",
            "EN_CURSO, ANTEPROYECTO",
            "EN_CURSO, FINALIZADO",
            "PRORROGA, EN_CURSO",
            "PRORROGA, PRORROGA",
            "SUSTENTACION, EN_CURSO",
            "CORRECCIONES_SUSTENTACION, FINALIZADO"
    })
    void rechazaTransicionesInvalidas(Proceso origen, Proceso destino) {
        assertFalse(origen.puedeAvanzarA(destino));
    }

    @ParameterizedTest
    @EnumSource(value = Proceso.class, names = { "FINALIZADO", "ABANDONADO" }, mode = EnumSource.Mode.EXCLUDE)
    void cualquierEtapaActivaPuedeAbandonarse(Proceso origen) {
        assertTrue(origen.puedeAvanzarA(Proceso.ABANDONADO));
    }

    @ParameterizedTest
    @EnumSource(value = Proceso.class, names = { "FINALIZADO", "ABANDONADO" })
    void etapasFinalesNoAdmitenTransiciones(Proceso origen) {
        assertTrue(origen.esFinal());
        for (Proceso destino : Proceso.values())
            assertFalse(origen.puedeAvanzarA(destino));
    }

    @Test
    void noPermiteAvanzarANulo() {
        assertFalse(Proceso.PROPUESTA.puedeAvanzarA(null));
    }
}
