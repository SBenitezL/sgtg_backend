package com.sgtg.backend.domain.models.auditoria;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class SelloAuditoriaTest {

    private static final byte[] CLAVE = "clave-de-prueba-de-al-menos-32-caracteres".getBytes(StandardCharsets.UTF_8);

    private static RegistroAuditoria registro(String ruta, String detalle) {
        return new RegistroAuditoria(LocalDateTime.of(2026, 10, 30, 15, 0, 0, 123_456_000), 5L, "a@unicauca.edu.co",
                "DOCENTE", "sesion", "solicitud", "10.0.0.1", "Navegador", "PUT", ruta, "Controlador.operacion", 7L,
                "trabajoId=7", 200, ResultadoAuditoria.EXITOSA, detalle);
    }

    @Test
    void elMismoRegistroEnElMismoLugarDaElMismoSello() {
        String sello = SelloAuditoria.sellar(CLAVE, 3, "anterior", registro("/ruta", null));
        assertEquals(64, sello.length());
        assertEquals(sello, SelloAuditoria.sellar(CLAVE, 3, "anterior", registro("/ruta", null)));
    }

    @Test
    void cambiarCualquierDatoElLugarOElAnteriorCambiaElSello() {
        String sello = SelloAuditoria.sellar(CLAVE, 3, "anterior", registro("/ruta", null));
        assertNotEquals(sello, SelloAuditoria.sellar(CLAVE, 3, "anterior", registro("/otra", null)));
        assertNotEquals(sello, SelloAuditoria.sellar(CLAVE, 3, "anterior", registro("/ruta", "")));
        assertNotEquals(sello, SelloAuditoria.sellar(CLAVE, 4, "anterior", registro("/ruta", null)));
        assertNotEquals(sello, SelloAuditoria.sellar(CLAVE, 3, "otro", registro("/ruta", null)));
        assertNotEquals(sello, SelloAuditoria.sellar(CLAVE, 3, null, registro("/ruta", null)));
        // Sin la clave no se puede calcular
        assertNotEquals(sello, SelloAuditoria.sellar("otra-clave-de-prueba-de-32-caracteres!!".getBytes(
                StandardCharsets.UTF_8), 3, "anterior", registro("/ruta", null)));
    }

    @Test
    void unSeparadorDentroDeUnDatoNoSeConfundeConElSiguiente() {
        // "a;" + "b" no puede dar lo mismo que "a" + ";b"
        assertNotEquals(SelloAuditoria.sellar(CLAVE, 1, null, registro("/a;", "b")),
                SelloAuditoria.sellar(CLAVE, 1, null, registro("/a", ";b")));
    }

    @Test
    void laClaveDerivadaSiempreEsLaMismaParaLaMismaBase() {
        byte[] base = "clave-de-los-tokens-de-32-caracteres!!".getBytes(StandardCharsets.UTF_8);
        assertEquals(32, SelloAuditoria.derivarClave(base).length);
        assertArrayEquals(SelloAuditoria.derivarClave(base), SelloAuditoria.derivarClave(base));
        assertFalse(Arrays.equals(base, SelloAuditoria.derivarClave(base)));
    }
}
