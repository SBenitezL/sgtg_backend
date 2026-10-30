package com.sgtg.backend.domain.models.acceso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** Bloqueo de un correo por inicios de sesión fallidos seguidos. */
class IntentosAccesoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 30, 10, 0);

    @Test
    void alQuintoFalloSeguidoSeBloqueaQuinceMinutos() {
        IntentosAcceso intentos = IntentosAcceso.sinFallos("d1@unicauca.edu.co");
        for (int i = 0; i < IntentosAcceso.MAXIMO_FALLOS - 1; i++) {
            intentos.registrarFallo(AHORA.plusMinutes(i));
            assertFalse(intentos.estaBloqueado(AHORA.plusMinutes(i)));
        }
        LocalDateTime quinto = AHORA.plusMinutes(4);
        intentos.registrarFallo(quinto);

        assertTrue(intentos.estaBloqueado(quinto));
        assertTrue(intentos.estaBloqueado(quinto.plusMinutes(14)));
        assertFalse(intentos.estaBloqueado(quinto.plusMinutes(15)));
        // Tras el bloqueo la cuenta empieza de nuevo
        assertEquals(0, intentos.getFallos());
    }

    @Test
    void losFallosSeparadosPorMasDeLaVentanaNoSeAcumulan() {
        IntentosAcceso intentos = IntentosAcceso.sinFallos("d1@unicauca.edu.co");
        for (int i = 0; i < IntentosAcceso.MAXIMO_FALLOS; i++)
            intentos.registrarFallo(AHORA.plusMinutes(16L * i));

        assertFalse(intentos.estaBloqueado(AHORA.plusMinutes(16L * (IntentosAcceso.MAXIMO_FALLOS - 1))));
        assertEquals(1, intentos.getFallos());
    }

    @Test
    void sinFallosNoHayNadaQueRegistrar() {
        IntentosAcceso intentos = IntentosAcceso.sinFallos("d1@unicauca.edu.co");
        assertFalse(intentos.tieneRegistro());
        intentos.registrarFallo(AHORA);
        assertTrue(intentos.tieneRegistro());
    }
}
