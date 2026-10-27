package com.sgtg.backend.infraestructure.output.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** La clave de los tokens se valida al arrancar. */
class JwtServiceTest {

    private static JwtService conClave(String clave) {
        JwtService servicio = new JwtService();
        ReflectionTestUtils.setField(servicio, "SECRET_KEY", clave);
        return servicio;
    }

    @Test
    void unaClaveVaciaODebilImpideArrancar() {
        assertThrows(IllegalStateException.class, () -> conClave("").validarClave());
        // 16 bytes: menos de los 32 que exige la firma HS256
        assertThrows(IllegalStateException.class, () -> conClave("MDEyMzQ1Njc4OWFiY2RlZg==").validarClave());
    }

    @Test
    void unaClaveDeAlMenos32BytesEsValida() {
        assertDoesNotThrow(() -> conClave("5kMpK8pN2vQ9rX3sT6uW1yZ4aB7cD0eF3gH6iJ9lM2n5pOqRsT").validarClave());
    }
}
