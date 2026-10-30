package com.sgtg.backend.infraestructure.output.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import com.sgtg.backend.infraestructure.output.percistence.entities.SesionCerradaEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.SesionCerradaRepository;

/** La clave de los tokens se valida al arrancar; cada token es una sesión que se puede cerrar. */
class JwtServiceTest {

    private static final String CLAVE = "5kMpK8pN2vQ9rX3sT6uW1yZ4aB7cD0eF3gH6iJ9lM2n5pOqRsT";

    private final SesionCerradaRepository sesiones = mock(SesionCerradaRepository.class);

    private JwtService conClave(String clave) {
        JwtService servicio = new JwtService(sesiones);
        ReflectionTestUtils.setField(servicio, "SECRET_KEY", clave);
        return servicio;
    }

    private static String token(JwtService servicio) {
        return servicio.getToken(new User("d1@unicauca.edu.co", "x", List.of(new SimpleGrantedAuthority("DOCENTE"))));
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
    void cadaTokenTieneSuPropiaSesion() {
        JwtService servicio = conClave(CLAVE);
        String sesion = servicio.getSesion(token(servicio));
        assertNotNull(sesion);
        assertNotEquals(sesion, servicio.getSesion(token(servicio)));
    }

    @Test
    void cerrarLaSesionLaGuardaHastaQueVenceElToken() {
        JwtService servicio = conClave(CLAVE);
        String token = token(servicio);

        servicio.cerrarSesion(token);

        ArgumentCaptor<SesionCerradaEntity> cerrada = ArgumentCaptor.forClass(SesionCerradaEntity.class);
        verify(sesiones).save(cerrada.capture());
        assertEquals(servicio.getSesion(token), cerrada.getValue().getSesion());
        // El token dura un día
        assertTrue(cerrada.getValue().getExpira().isAfter(LocalDateTime.now().plusHours(23)));
    }

    @Test
    void unTokenInvalidoNoSeRegistraAlCerrarSesion() {
        conClave(CLAVE).cerrarSesion("no-es-un-token");
        verify(sesiones, never()).save(any());
    }
}
