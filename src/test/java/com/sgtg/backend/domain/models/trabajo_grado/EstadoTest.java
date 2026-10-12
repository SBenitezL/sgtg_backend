package com.sgtg.backend.domain.models.trabajo_grado;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EstadoTest {

    @ParameterizedTest
    @CsvSource({
            "EN_REVISION, CON_CORRECCIONES",
            "EN_REVISION, APROBADO",
            "EN_REVISION, RECHAZADO",
            "EN_REVISION, NO_APROBADO",
            "CON_CORRECCIONES, EN_REVISION"
    })
    void permiteCambiosValidos(Estado origen, Estado destino) {
        assertTrue(origen.puedeCambiarA(destino));
    }

    @ParameterizedTest
    @CsvSource({
            "CON_CORRECCIONES, APROBADO",
            "CON_CORRECCIONES, NO_APROBADO",
            "APROBADO, EN_REVISION",
            "RECHAZADO, EN_REVISION",
            "NO_APROBADO, EN_REVISION",
            "EN_REVISION, EN_REVISION"
    })
    void rechazaCambiosInvalidos(Estado origen, Estado destino) {
        assertFalse(origen.puedeCambiarA(destino));
    }
}
