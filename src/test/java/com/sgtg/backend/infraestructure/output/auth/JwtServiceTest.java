package com.sgtg.backend.infraestructure.output.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** La clave de los tokens se valida al arrancar; cada token es de una sesión y dura poco. */
class JwtServiceTest {

    private static final String CLAVE = "5kMpK8pN2vQ9rX3sT6uW1yZ4aB7cD0eF3gH6iJ9lM2n5pOqRsT";

    private JwtService conClave(String clave) {
        JwtService servicio = new JwtService();
        ReflectionTestUtils.setField(servicio, "SECRET_KEY", clave);
        return servicio;
    }

    private static String token(JwtService servicio, String sesion) {
        return servicio.getToken(new User("d1@unicauca.edu.co", "x", List.of(new SimpleGrantedAuthority("DOCENTE"))),
                3, false, sesion);
    }

    private static JsonNode contenido(String token) throws Exception {
        return new ObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
    }

    @Test
    void unaClaveVaciaODebilImpideArrancar() {
        assertThrows(IllegalStateException.class, () -> conClave("").validarClave());
        // 16 bytes: menos de los 32 que exige la firma HS256
        assertThrows(IllegalStateException.class, () -> conClave("MDEyMzQ1Njc4OWFiY2RlZg==").validarClave());
    }

    @Test
    void unaClaveDeAlMenos32BytesEsValida() {
        assertDoesNotThrow(() -> conClave(CLAVE).validarClave());
    }

    @Test
    void losTokensDeUnaSesionLlevanSuIdentificadorYCadaUnoEsDistinto() throws Exception {
        JwtService servicio = conClave(CLAVE);
        String primero = token(servicio, "sesion-1");
        String segundo = token(servicio, "sesion-1");

        assertEquals("sesion-1", servicio.getSesion(primero));
        assertEquals("sesion-1", servicio.getSesion(segundo));
        assertNotEquals(contenido(primero).get("jti").asText(), contenido(segundo).get("jti").asText());
    }

    @Test
    void elTokenDuraQuinceMinutos() throws Exception {
        JsonNode token = contenido(token(conClave(CLAVE), "sesion-1"));
        assertEquals(15 * 60, token.get("exp").asLong() - token.get("iat").asLong());
    }

    @Test
    void elTokenLlevaLaVersionDeLasCredenciales() {
        JwtService servicio = conClave(CLAVE);
        assertEquals(3, servicio.getVersionCredencial(token(servicio, "sesion-1")));
        assertTrue(servicio.isTokenValid(token(servicio, "sesion-1"),
                new User("d1@unicauca.edu.co", "x", List.of())));
    }
}
