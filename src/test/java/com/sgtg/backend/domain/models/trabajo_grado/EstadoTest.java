package com.sgtg.backend.domain.models.trabajo_grado;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EstadoTest {

    @ParameterizedTest
    @CsvSource({
            "RADICADO, EN_REVISION",
            "EN_REVISION, CON_CORRECCIONES",
            "EN_REVISION, APROBADO",
            "EN_REVISION, RECHAZADO",
            "CON_CORRECCIONES, RADICADO"
    })
    void permiteCambiosValidos(Estado origen, Estado destino) {
        assertTrue(origen.puedeCambiarA(destino));
    }

    @ParameterizedTest
    @CsvSource({
            "RADICADO, APROBADO",
            "CON_CORRECCIONES, APROBADO",
            "APROBADO, EN_REVISION",
            "RECHAZADO, RADICADO"
    })
    void rechazaCambiosInvalidos(Estado origen, Estado destino) {
        assertFalse(origen.puedeCambiarA(destino));
    }
}
