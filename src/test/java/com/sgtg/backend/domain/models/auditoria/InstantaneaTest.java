package com.sgtg.backend.domain.models.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InstantaneaTest {

    private static Instantanea de(String... claveValor) {
        Map<String, String> datos = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2)
            datos.put(claveValor[i], claveValor[i + 1]);
        return new Instantanea(datos);
    }

    @Test
    void soloMuestraLosDatosQueCambiaronEnSuOrden() {
        Instantanea antes = de("etapa", "PROPUESTA", "título", "Uno", "roles", "DOCENTE");
        Instantanea despues = de("etapa", "ANTEPROYECTO", "título", "Uno", "roles", "DIRECTOR, DOCENTE");

        assertEquals(List.of("etapa: PROPUESTA → ANTEPROYECTO", "roles: DOCENTE → DIRECTOR, DOCENTE"),
                antes.cambiosHasta(despues));
    }

    @Test
    void losDatosVaciosSeMuestranConUnGuion() {
        Instantanea antes = de("motivo de cierre", null, "asesor", "");
        Instantanea despues = de("motivo de cierre", "CANCELADO", "asesor", "usuario 4");

        assertEquals(List.of("motivo de cierre: — → CANCELADO", "asesor: — → usuario 4"),
                antes.cambiosHasta(despues));
        assertEquals(List.of(), antes.cambiosHasta(antes));
    }
}
