package com.sgtg.backend.infraestructure.output.archivo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;
import com.sgtg.backend.application.output.ManageAlertaGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.AlertaSeguridad;
import com.sgtg.backend.domain.models.auditoria.TipoAlerta;
import com.sgtg.backend.infraestructure.output.percistence.entities.HuellaArchivoEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.HuellaArchivoRepository;

/** Almacenamiento de archivos en disco, sobre un directorio temporal. */
class ManageArchivoGatewayImplAdapterTest {

    @TempDir
    Path temporal;

    /** Directorio del almacenamiento, dentro del temporal para poder probar rutas que intentan salir de él */
    private Path raiz;
    private ManageArchivoGatewayImplAdapter almacenamiento;
    /** Huellas guardadas, como las tendría la tabla */
    private final Map<String, HuellaArchivoEntity> huellas = new HashMap<>();
    private ManageAlertaGatewayIntPort alertas;

    @BeforeEach
    void preparar() {
        raiz = temporal.resolve("almacen");
        HuellaArchivoRepository repositorio = mock(HuellaArchivoRepository.class);
        when(repositorio.save(any())).thenAnswer(guardar -> {
            HuellaArchivoEntity huella = guardar.getArgument(0);
            huellas.put(huella.getRuta(), huella);
            return huella;
        });
        when(repositorio.findById(anyString())).thenAnswer(buscar -> Optional.ofNullable(huellas.get(buscar.getArgument(0))));
        when(repositorio.existsById(anyString())).thenAnswer(buscar -> huellas.containsKey(buscar.getArgument(0)));
        when(repositorio.findAll()).thenAnswer(todas -> new ArrayList<>(huellas.values()));
        doAnswer(borrar -> huellas.remove(borrar.getArgument(0))).when(repositorio)
                .deleteById(anyString());
        alertas = mock(ManageAlertaGatewayIntPort.class);
        almacenamiento = new ManageArchivoGatewayImplAdapter(raiz.toString(), repositorio, alertas);
    }

    private static ArchivoAdjunto pdf(String nombre, String contenido) {
        return new ArchivoAdjunto(nombre, "application/pdf", contenido.getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void guardaPorTrabajoTipoEIteracionConNombreDescriptivo() {
        assertEquals("15/PP-A/v1/Formato A - v1.pdf",
                almacenamiento.guardar(15L, "PP-A", "Formato A", pdf("propuesta.pdf", "%PDF-1")));
        assertEquals("15/PP-A/v2/Formato A - v2.pdf",
                almacenamiento.guardar(15L, "PP-A", "Formato A", pdf("otra.pdf", "%PDF-2")));
        assertEquals("15/ANTEPROYECTO/v1/Anteproyecto - v1.pdf",
                almacenamiento.guardar(15L, "ANTEPROYECTO", "Anteproyecto", pdf("a.pdf", "%PDF-3")));
        assertEquals("16/PP-A/v1/Formato A - v1.pdf",
                almacenamiento.guardar(16L, "PP-A", "Formato A", pdf("propuesta.pdf", "%PDF-4")));
        // La primera versión no se sobrescribe
        assertArrayEquals("%PDF-1".getBytes(StandardCharsets.US_ASCII),
                almacenamiento.obtener("15/PP-A/v1/Formato A - v1.pdf").contenido());
    }

    @Test
    void obtieneElArchivoConSuNombreYTipo() {
        String ruta = almacenamiento.guardar(15L, "TI-A", "Formato A", pdf("propuesta.pdf", "%PDF-1.4"));

        ArchivoAdjunto archivo = almacenamiento.obtener(ruta);
        assertEquals("Formato A - v1.pdf", archivo.nombre());
        assertEquals("application/pdf", archivo.tipoContenido());
        assertArrayEquals("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), archivo.contenido());
        assertNull(almacenamiento.obtener("15/TI-A/v9/Formato A - v9.pdf"));
    }

    @Test
    void noEntregaArchivosFueraDelDirectorio() throws Exception {
        Files.writeString(temporal.resolve("secreto.pdf"), "%PDF-secreto");
        assertNull(almacenamiento.obtener("../secreto.pdf"));
        assertNull(almacenamiento.obtener("15/../../secreto.pdf"));
        assertNull(almacenamiento.obtener(""));
    }

    @Test
    void elNombreSeGuardaSinTildesNiCaracteresDeRuta() {
        assertEquals("15/RESOLUCION/v1/Resolucion - v1.pdf",
                almacenamiento.guardar(15L, "RESOLUCION", "Resolución", pdf("r.pdf", "%PDF-1")));
        assertEquals("15/TI-A/v1/Formato A - v1.pdf",
                almacenamiento.guardar(15L, "TI-A", "../Formato A", pdf("x.pdf", "%PDF-1")));
        assertEquals("15/OTRO/v1/Documento - v1.pdf",
                almacenamiento.guardar(15L, "OTRO", " ", pdf("x.pdf", "%PDF-1")));
    }

    @Test
    void alEliminarLaIteracionQuedaLibre() {
        String ruta = almacenamiento.guardar(15L, "RESOLUCION", "Resolución", pdf("r.pdf", "%PDF-1"));
        almacenamiento.eliminar(ruta);

        assertFalse(Files.exists(raiz.resolve("15/RESOLUCION/v1")));
        assertEquals("15/RESOLUCION/v1/Resolucion - v1.pdf",
                almacenamiento.guardar(15L, "RESOLUCION", "Resolución", pdf("r2.pdf", "%PDF-2")));
        // Eliminar una ruta inexistente no falla
        almacenamiento.eliminar("15/RESOLUCION/v7/nada.pdf");
    }

    @Test
    void reubicaUnArchivoTemporalEnLaCarpetaDelTrabajo() {
        String temporal = almacenamiento.guardarTemporal(pdf("carta.pdf", "%PDF-carta"));
        assertTrue(temporal.startsWith("temporal/"));

        String definitiva = almacenamiento.reubicar(temporal, 20L, "CARTA_ACEPTACION", "Carta de aceptación");

        assertEquals("20/CARTA_ACEPTACION/v1/Carta de aceptacion - v1.pdf", definitiva);
        assertNull(almacenamiento.obtener(temporal));
        assertArrayEquals("%PDF-carta".getBytes(StandardCharsets.US_ASCII),
                almacenamiento.obtener(definitiva).contenido());
        // La carpeta temporal se elimina
        assertFalse(Files.exists(raiz.resolve(temporal).getParent()));
    }

    // ----- Huellas -----

    @Test
    void guardaLaHuellaSha256DeCadaArchivoYLaSigueAlReubicarlo() {
        String ruta = almacenamiento.guardar(15L, "TI-A", "Formato A", pdf("a.pdf", "%PDF-1"));
        // SHA-256 de "%PDF-1"
        assertEquals("21af8e71c8703196df7fe1ff901869a88fe64c07bbaa83d838efb45a52b4f303",
                huellas.get(ruta).getSha256());
        assertEquals(6, huellas.get(ruta).getTamano());

        String temporal = almacenamiento.guardarTemporal(pdf("carta.pdf", "%PDF-carta"));
        String definitiva = almacenamiento.reubicar(temporal, 20L, "CARTA_ACEPTACION", "Carta");
        assertFalse(huellas.containsKey(temporal));
        assertTrue(huellas.containsKey(definitiva));

        almacenamiento.eliminar(ruta);
        assertFalse(huellas.containsKey(ruta));
    }

    @Test
    void unArchivoCambiadoEnElDiscoSeAvisaAlLeerloYAlVerificar() throws Exception {
        String ruta = almacenamiento.guardar(15L, "TI-A", "Formato A", pdf("a.pdf", "%PDF-original"));
        almacenamiento.obtener(ruta);
        verify(alertas, never()).alertar(any());

        Files.writeString(raiz.resolve(ruta), "%PDF-cambiado");

        // Se entrega igual, para no bloquear el trámite, pero se avisa
        assertArrayEquals("%PDF-cambiado".getBytes(StandardCharsets.US_ASCII), almacenamiento.obtener(ruta).contenido());
        ArgumentCaptor<AlertaSeguridad> alerta = ArgumentCaptor.forClass(AlertaSeguridad.class);
        verify(alertas).alertar(alerta.capture());
        assertEquals(TipoAlerta.ARCHIVO_ALTERADO, alerta.getValue().tipo());
        assertEquals(List.of(ruta + ": no coincide con su huella"), almacenamiento.verificarHuellas());
    }

    @Test
    void laVerificacionRegistraLosArchivosSinHuellaYDetectaLosQueFaltan() throws Exception {
        // Un archivo guardado antes de calcular huellas
        Path antiguo = raiz.resolve("9/TI-A/v1/Antiguo - v1.pdf");
        Files.createDirectories(antiguo.getParent());
        Files.writeString(antiguo, "%PDF-antiguo");
        // Archivo propio del sistema: no se registra
        Files.writeString(raiz.resolve(".reloj-desplazamiento-dias"), "0");
        String borrado = almacenamiento.guardar(15L, "TI-A", "Formato A", pdf("a.pdf", "%PDF-1"));
        Files.delete(raiz.resolve(borrado));

        assertEquals(List.of(borrado + ": ya no existe"), almacenamiento.verificarHuellas());
        assertTrue(huellas.containsKey("9/TI-A/v1/Antiguo - v1.pdf"));
        assertFalse(huellas.containsKey(".reloj-desplazamiento-dias"));
    }
}
