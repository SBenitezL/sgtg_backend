package com.sgtg.backend.infraestructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Reloj ajustable de desarrollo: su desplazamiento sobrevive a los reinicios. */
class RelojAjustableTest {

    /** Lunes 26 de octubre de 2026, 10:00 en Bogotá */
    private final Clock sistema = Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("America/Bogota"));

    @TempDir
    Path carpeta;

    @Test
    void elDesplazamientoSobreviveAUnReinicio() {
        Path archivo = carpeta.resolve(".reloj-desplazamiento-dias");
        new RelojAjustable(sistema, archivo).avanzarDias(195);

        // Al reiniciar la aplicación el tiempo no retrocede
        RelojAjustable reiniciado = new RelojAjustable(sistema, archivo);
        assertEquals(195, reiniciado.getDesplazamientoDias());
        assertEquals(LocalDate.of(2027, 5, 9), LocalDate.now(reiniciado));

        reiniciado.restablecer();
        assertEquals(0, new RelojAjustable(sistema, archivo).getDesplazamientoDias());
    }

    @Test
    void sinArchivoOConUnValorInvalidoUsaLaFechaReal() throws IOException {
        assertEquals(0, new RelojAjustable(sistema).getDesplazamientoDias());

        Path archivo = carpeta.resolve("reloj");
        Files.writeString(archivo, "no es un número");
        assertEquals(LocalDate.of(2026, 10, 26), LocalDate.now(new RelojAjustable(sistema, archivo)));
    }
}
