package com.sgtg.backend.domain.models.acceso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sgtg.backend.domain.models.acceso.SesionUsuario.Renovacion;

class SesionUsuarioTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 31, 8, 0);

    private static SesionUsuario sesion() {
        return SesionUsuario.nueva("s1", 5L, 2, "h1", INICIO);
    }

    @Test
    void alRenovarseRotaElTokenYCuentaComoUso() {
        SesionUsuario sesion = sesion();
        LocalDateTime despues = INICIO.plusMinutes(14);

        assertEquals(Renovacion.RENOVADA, sesion.renovar("h1", 2, "h2", despues));
        assertEquals("h2", sesion.getHuellaActual());
        assertEquals(despues, sesion.getUltimoUso());
        assertTrue(sesion.estaActiva(despues.plusMinutes(29)));
    }

    @Test
    void elTokenAnteriorSeAceptaSoloDuranteLaGracia() {
        SesionUsuario sesion = sesion();
        sesion.renovar("h1", 2, "h2", INICIO.plusMinutes(1));
        // Otra pestaña renovó a la vez con el mismo token
        assertEquals(Renovacion.RENOVADA, sesion.renovar("h1", 2, "h3", INICIO.plusMinutes(1).plusSeconds(10)));
        assertEquals("h3", sesion.getHuellaActual());

        SesionUsuario otra = sesion();
        otra.renovar("h1", 2, "h2", INICIO.plusMinutes(1));
        assertEquals(Renovacion.REUTILIZADA, otra.renovar("h1", 2, "h3", INICIO.plusMinutes(2)));
    }

    @Test
    void reutilizarUnTokenYaUsadoCierraLaSesion() {
        SesionUsuario sesion = sesion();
        sesion.renovar("h1", 2, "h2", INICIO.plusMinutes(1));
        sesion.renovar("h2", 2, "h3", INICIO.plusMinutes(2));

        // h1 ya no es ni el vigente ni el anterior: alguien lo copió
        assertEquals(Renovacion.REUTILIZADA, sesion.renovar("h1", 2, "h4", INICIO.plusMinutes(3)));
        assertFalse(sesion.estaActiva(INICIO.plusMinutes(3)));
        // Ni el dueño puede seguir con el vigente
        assertEquals(Renovacion.VENCIDA, sesion.renovar("h3", 2, "h5", INICIO.plusMinutes(4)));
    }

    @Test
    void venceTrasTreintaMinutosSinUsoOAlCumplirDoceHoras() {
        SesionUsuario inactiva = sesion();
        assertTrue(inactiva.estaActiva(INICIO.plusMinutes(29)));
        assertFalse(inactiva.estaActiva(INICIO.plusMinutes(30)));
        assertEquals(Renovacion.VENCIDA, inactiva.renovar("h1", 2, "h2", INICIO.plusMinutes(31)));

        SesionUsuario larga = sesion();
        LocalDateTime momento = INICIO;
        String huella = "h1";
        for (int i = 0; i < 48; i++) {
            momento = momento.plusMinutes(14);
            larga.renovar(huella, 2, huella + i, momento);
            huella = huella + i;
        }
        assertFalse(larga.estaActiva(INICIO.plusHours(12)));
        assertEquals(INICIO.plusHours(12), larga.expira());
    }

    @Test
    void conOtraVersionDeLasCredencialesNoSeRenueva() {
        SesionUsuario sesion = sesion();
        assertEquals(Renovacion.VENCIDA, sesion.renovar("h1", 3, "h2", INICIO.plusMinutes(1)));
        assertFalse(sesion.estaActiva(INICIO.plusMinutes(1)));

        SesionUsuario actualizada = sesion();
        actualizada.actualizarVersion(3);
        assertEquals(Renovacion.RENOVADA, actualizada.renovar("h1", 3, "h2", INICIO.plusMinutes(1)));
    }

    @Test
    void unaSesionCerradaNoSeRenueva() {
        SesionUsuario sesion = sesion();
        sesion.revocar(INICIO.plusMinutes(5));
        assertFalse(sesion.estaActiva(INICIO.plusMinutes(5)));
        assertEquals(Renovacion.VENCIDA, sesion.renovar("h1", 2, "h2", INICIO.plusMinutes(6)));
        assertEquals(INICIO.plusMinutes(5), sesion.getRevocada());
    }
}
