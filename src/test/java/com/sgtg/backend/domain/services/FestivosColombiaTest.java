package com.sgtg.backend.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FestivosColombiaTest {

    @Test
    void calculaElDomingoDePascua() {
        assertEquals(LocalDate.of(2024, 3, 31), FestivosColombia.domingoDePascua(2024));
        assertEquals(LocalDate.of(2025, 4, 20), FestivosColombia.domingoDePascua(2025));
        assertEquals(LocalDate.of(2026, 4, 5), FestivosColombia.domingoDePascua(2026));
    }

    @Test
    void festivosDe2026() {
        Set<LocalDate> esperados = Set.of(
                LocalDate.of(2026, 1, 1), // Año Nuevo
                LocalDate.of(2026, 1, 12), // Reyes Magos (6 ene, trasladado)
                LocalDate.of(2026, 3, 23), // San José (19 mar, trasladado)
                LocalDate.of(2026, 4, 2), // Jueves Santo
                LocalDate.of(2026, 4, 3), // Viernes Santo
                LocalDate.of(2026, 5, 1), // Día del Trabajo
                LocalDate.of(2026, 5, 18), // Ascensión
                LocalDate.of(2026, 6, 8), // Corpus Christi
                LocalDate.of(2026, 6, 15), // Sagrado Corazón
                LocalDate.of(2026, 6, 29), // San Pedro y San Pablo (ya es lunes)
                LocalDate.of(2026, 7, 20), // Independencia
                LocalDate.of(2026, 8, 7), // Batalla de Boyacá
                LocalDate.of(2026, 8, 17), // Asunción (15 ago, trasladado)
                LocalDate.of(2026, 10, 12), // Día de la Raza (ya es lunes)
                LocalDate.of(2026, 11, 2), // Todos los Santos (1 nov, trasladado)
                LocalDate.of(2026, 11, 16), // Independencia de Cartagena (11 nov, trasladado)
                LocalDate.of(2026, 12, 8), // Inmaculada Concepción
                LocalDate.of(2026, 12, 25)); // Navidad

        assertEquals(esperados, FestivosColombia.delAnio(2026));
    }

    @Test
    void identificaFestivosYDiasHabiles() {
        assertTrue(FestivosColombia.esFestivo(LocalDate.of(2026, 1, 12)));
        assertFalse(FestivosColombia.esFestivo(LocalDate.of(2026, 1, 6)));
    }
}
