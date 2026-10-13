package com.sgtg.backend.domain.models.trabajo_grado;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

class DocumentoTest {

    private final LocalDate hoy = LocalDate.of(2026, 3, 1);

    @Test
    void nuevaVersionConservaTipoEIncrementaVersion() {
        Documento v1 = Documento.inicial(TipoDocumento.ANTEPROYECTO, "1/tg/1/v1.pdf",
                hoy);
        Documento v2 = v1.nuevaVersion("1/tg/1/v2.pdf", hoy.plusDays(10));
        assertEquals(1, v1.version());
        assertEquals(2, v2.version());
        assertEquals(TipoDocumento.ANTEPROYECTO, v2.tipo());
    }

    @ParameterizedTest
    @ValueSource(strings = { "../archivo.pdf", "1/../../archivo.pdf", "1\\..\\archivo.pdf" })
    void rechazaRutasQueSalenDelAlmacenamiento(String ruta) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> Documento.inicial(TipoDocumento.PROPUESTA, ruta, hoy));
        assertEquals("error.dominio.documento.ruta_invalida", error.getMessage());
    }

    @Test
    void conRutaConservaTipoVersionYFecha() {
        Documento temporal = Documento.inicial(TipoDocumento.CARTA_ACEPTACION, "temporal/abc/carta.pdf", hoy);
        Documento definitiva = temporal.conRuta("15/CARTA_ACEPTACION/v1/carta.pdf");
        assertEquals(new Documento(TipoDocumento.CARTA_ACEPTACION, "15/CARTA_ACEPTACION/v1/carta.pdf", 1, hoy),
                definitiva);
    }

    @Test
    void laCarpetaEsElFormatoOElNombreDelTipo() {
        assertEquals("PP-A", TipoDocumento.PROPUESTA.carpeta(Modalidad.PRACTICA_PROFESIONAL));
        assertEquals("TI-B", TipoDocumento.EVALUACION_ANTEPROYECTO.carpeta(Modalidad.TRABAJO_DE_INVESTIGACION));
        assertEquals("ANTEPROYECTO", TipoDocumento.ANTEPROYECTO.carpeta(Modalidad.TRABAJO_DE_INVESTIGACION));
        assertEquals("RESOLUCION", TipoDocumento.RESOLUCION.carpeta(Modalidad.PRACTICA_PROFESIONAL));
    }

    @Test
    void rechazaDatosIncompletosYVersionInvalida() {
        assertThrows(ReglaNegocioException.class, () -> Documento.inicial(null, "1/a.pdf", hoy));
        assertThrows(ReglaNegocioException.class, () -> Documento.inicial(TipoDocumento.PROPUESTA, " ", hoy));
        assertThrows(ReglaNegocioException.class,
                () -> new Documento(TipoDocumento.PROPUESTA, "1/a.pdf", 0, hoy));
    }

    @Test
    void formatoDependeDeLaModalidad() {
        assertEquals(Optional.of("TI-A"), TipoDocumento.PROPUESTA.formato(Modalidad.TRABAJO_DE_INVESTIGACION));
        assertEquals(Optional.of("PP-H"), TipoDocumento.VERSION_DEFINITIVA.formato(Modalidad.PRACTICA_PROFESIONAL));
        assertEquals(Optional.empty(), TipoDocumento.ANTEPROYECTO.formato(Modalidad.PRACTICA_PROFESIONAL));
    }

    @Test
    void documentosDePracticaProfesionalNoAplicanAInvestigacion() {
        assertTrue(TipoDocumento.CARTA_ACEPTACION.aplicaA(Modalidad.PRACTICA_PROFESIONAL));
        assertFalse(TipoDocumento.CARTA_ACEPTACION.aplicaA(Modalidad.TRABAJO_DE_INVESTIGACION));
        assertFalse(TipoDocumento.CONCEPTO_ASESOR.aplicaA(Modalidad.TRABAJO_DE_INVESTIGACION));
        // Los informes mensuales (formato D) aplican a ambas modalidades
        assertEquals(Optional.of("TI-D"), TipoDocumento.INFORME_MENSUAL.formato(Modalidad.TRABAJO_DE_INVESTIGACION));
    }

    @Test
    void cambioEtapaSoloRegistraTransicionesValidas() {
        LocalDateTime ahora = hoy.atStartOfDay();
        assertDoesNotThrow(() -> new CambioEtapa(Proceso.EVALUACION, Proceso.ANTEPROYECTO, ahora, "C001", null));
        assertThrows(ReglaNegocioException.class,
                () -> new CambioEtapa(Proceso.PROPUESTA, Proceso.EN_CURSO, ahora, "C001", null));
        assertThrows(ReglaNegocioException.class,
                () -> new CambioEtapa(Proceso.PROPUESTA, Proceso.ANTEPROYECTO, ahora, " ", null));
    }
}
