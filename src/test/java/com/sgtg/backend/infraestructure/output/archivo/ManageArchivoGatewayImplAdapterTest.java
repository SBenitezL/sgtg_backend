package com.sgtg.backend.infraestructure.output.archivo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;

/** Almacenamiento de archivos en disco, sobre un directorio temporal. */
class ManageArchivoGatewayImplAdapterTest {

    @TempDir
    Path temporal;

    /** Directorio del almacenamiento, dentro del temporal para poder probar rutas que intentan salir de él */
    private Path raiz;
    private ManageArchivoGatewayImplAdapter almacenamiento;

    @BeforeEach
    void preparar() {
        raiz = temporal.resolve("almacen");
        almacenamiento = new ManageArchivoGatewayImplAdapter(raiz.toString());
    }

    private static ArchivoAdjunto pdf(String nombre, String contenido) {
        return new ArchivoAdjunto(nombre, "application/pdf", contenido.getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void guardaPorTrabajoTipoEIteracion() {
        assertEquals("15/PP-A/v1/propuesta.pdf", almacenamiento.guardar(15L, "PP-A", pdf("propuesta.pdf", "%PDF-1")));
        assertEquals("15/PP-A/v2/propuesta.pdf", almacenamiento.guardar(15L, "PP-A", pdf("propuesta.pdf", "%PDF-2")));
        assertEquals("15/ANTEPROYECTO/v1/a.pdf", almacenamiento.guardar(15L, "ANTEPROYECTO", pdf("a.pdf", "%PDF-3")));
        assertEquals("16/PP-A/v1/propuesta.pdf", almacenamiento.guardar(16L, "PP-A", pdf("propuesta.pdf", "%PDF-4")));
        // La primera versión no se sobrescribe
        assertArrayEquals("%PDF-1".getBytes(StandardCharsets.US_ASCII),
                almacenamiento.obtener("15/PP-A/v1/propuesta.pdf").contenido());
    }

    @Test
    void obtieneElArchivoConSuNombreYTipo() {
        String ruta = almacenamiento.guardar(15L, "TI-A", pdf("propuesta.pdf", "%PDF-1.4"));

        ArchivoAdjunto archivo = almacenamiento.obtener(ruta);
        assertEquals("propuesta.pdf", archivo.nombre());
        assertEquals("application/pdf", archivo.tipoContenido());
        assertArrayEquals("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), archivo.contenido());
        assertNull(almacenamiento.obtener("15/TI-A/v9/propuesta.pdf"));
    }

    @Test
    void noEntregaArchivosFueraDelDirectorio() throws Exception {
        Files.writeString(temporal.resolve("secreto.pdf"), "%PDF-secreto");
        assertNull(almacenamiento.obtener("../secreto.pdf"));
        assertNull(almacenamiento.obtener("15/../../secreto.pdf"));
        assertNull(almacenamiento.obtener(""));
    }

    @Test
    void limpiaElNombreDelArchivo() {
        assertEquals("15/TI-A/v1/Propuesta_final_ano.pdf",
                almacenamiento.guardar(15L, "TI-A", pdf("C:\\Users\\x\\Propuesta final año.pdf", "%PDF-1")));
        assertEquals("15/TI-A/v2/x.pdf", almacenamiento.guardar(15L, "TI-A", pdf("../../x.pdf", "%PDF-1")));
    }

    @Test
    void alEliminarLaIteracionQuedaLibre() {
        String ruta = almacenamiento.guardar(15L, "RESOLUCION", pdf("r.pdf", "%PDF-1"));
        almacenamiento.eliminar(ruta);

        assertFalse(Files.exists(raiz.resolve("15/RESOLUCION/v1")));
        assertEquals("15/RESOLUCION/v1/r2.pdf", almacenamiento.guardar(15L, "RESOLUCION", pdf("r2.pdf", "%PDF-2")));
        // Eliminar una ruta inexistente no falla
        almacenamiento.eliminar("15/RESOLUCION/v7/nada.pdf");
    }

    @Test
    void reubicaUnArchivoTemporalEnLaCarpetaDelTrabajo() {
        String temporal = almacenamiento.guardarTemporal(pdf("carta.pdf", "%PDF-carta"));
        assertTrue(temporal.startsWith("temporal/"));

        String definitiva = almacenamiento.reubicar(temporal, 20L, "CARTA_ACEPTACION");

        assertEquals("20/CARTA_ACEPTACION/v1/carta.pdf", definitiva);
        assertNull(almacenamiento.obtener(temporal));
        assertArrayEquals("%PDF-carta".getBytes(StandardCharsets.US_ASCII),
                almacenamiento.obtener(definitiva).contenido());
        // La carpeta temporal se elimina
        assertFalse(Files.exists(raiz.resolve(temporal).getParent()));
    }
}
