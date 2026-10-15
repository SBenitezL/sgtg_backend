package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.LUNES;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_PROPUESTA;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.decisionComite;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.directorPlanta;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.estudiante;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Cancelación del trabajo de grado (Arts. 26, 56). */
class EtapaCancelacionTest {

    private static final String RUTA_SOLICITUD = "solicitudes/cancelacion.pdf";
    private static final String RUTA_RESOLUCION = "resoluciones/res-500.pdf";

    private final CalendarioHabil calendario = new CalendarioHabilFalso();
    private TrabajoInvestigacion trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    private static Decision consejo(ResultadoConcepto resultado, LocalDate fecha) {
        return new Decision(Organo.CONSEJO_FACULTAD, resultado, null, RUTA_RESOLUCION, null, fecha);
    }

    private static LocalDateTime dia(LocalDate fecha) {
        return fecha.atTime(10, 0);
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = DatosPrueba.investigacion();
    }

    @Test
    void aprobadaCancelaElTrabajoDesdeCualquierEtapaNoFinal() {
        trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES);
        assertTrue(trabajo.cancelacionPendiente().isPresent());
        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());

        trabajo.resolverCancelacion(consejo(ResultadoConcepto.APROBADO, LUNES.plusDays(5)), "CF01",
                dia(LUNES.plusDays(5)));

        assertEquals(Proceso.CANCELADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.CANCELACION_DIRECTOR, trabajo.getMotivoCierre());
        assertTrue(trabajo.estaCerrado());
        CambioEtapa cambio = trabajo.getHistorial().get(0);
        assertEquals(Proceso.PROPUESTA, cambio.origen());
        assertEquals(Proceso.CANCELADO, cambio.destino());
        assertEquals("CF01", cambio.responsable());
        Cancelacion cancelacion = trabajo.getCancelaciones().get(0);
        assertTrue(cancelacion.fueAprobada());
        assertEquals(RUTA_SOLICITUD, cancelacion.rutaSolicitud());
        assertEquals(RUTA_RESOLUCION, cancelacion.decision().rutaArchivo());
    }

    @Test
    void negadaElTrabajoSigueYSePuedeVolverASolicitar() {
        trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES);
        // El comentario del Consejo es opcional
        trabajo.resolverCancelacion(consejo(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(5)), "CF01",
                dia(LUNES.plusDays(5)));

        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
        assertNull(trabajo.getMotivoCierre());
        assertTrue(trabajo.cancelacionPendiente().isEmpty());

        trabajo.solicitarCancelacion("solicitudes/cancelacion-2.pdf", LUNES.plusDays(10));
        assertEquals(2, trabajo.getCancelaciones().size());
    }

    @Test
    void mientrasEstaPendienteElTrabajoSigueSuCurso() {
        trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES);
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(3)), "C001",
                dia(LUNES.plusDays(3)));
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());

        trabajo.resolverCancelacion(consejo(ResultadoConcepto.APROBADO, LUNES.plusDays(8)), "CF01",
                dia(LUNES.plusDays(8)));

        assertEquals(Proceso.CANCELADO, trabajo.getEtapa());
        assertEquals(Proceso.PROPUESTA, trabajo.getCancelaciones().get(0).etapaAlSolicitar());
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getHistorial().get(1).origen());
    }

    @Test
    void soloHayUnaSolicitudPendienteALaVez() {
        trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES);
        assertRegla("error.dominio.cancelacion.solicitud_pendiente",
                () -> trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES.plusDays(1)));
    }

    @Test
    void laSolicitudExigeElFormato() {
        assertRegla("error.dominio.cancelacion.documento_obligatorio", () -> trabajo.solicitarCancelacion(" ", LUNES));
        assertTrue(trabajo.getCancelaciones().isEmpty());
    }

    @Test
    void laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido() {
        LocalDate fecha = LUNES.plusDays(5);
        assertRegla("error.dominio.cancelacion.sin_solicitud_pendiente", () -> trabajo
                .resolverCancelacion(consejo(ResultadoConcepto.APROBADO, fecha), "CF01", dia(fecha)));

        trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES);
        assertRegla("error.dominio.cancelacion.resolucion_obligatoria", () -> trabajo.resolverCancelacion(
                new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, null, null, fecha), "CF01",
                dia(fecha)));
        assertRegla("error.dominio.cancelacion.organo_invalido", () -> trabajo.resolverCancelacion(
                new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, null, RUTA_RESOLUCION, null, fecha),
                "CF01", dia(fecha)));
        assertRegla("error.dominio.cancelacion.resultado_invalido", () -> trabajo
                .resolverCancelacion(consejo(ResultadoConcepto.EVALUADO, fecha), "CF01", dia(fecha)));
        assertRegla("error.dominio.cancelacion.fecha_invalida", () -> trabajo.resolverCancelacion(
                consejo(ResultadoConcepto.APROBADO, LUNES.minusDays(1)), "CF01", dia(fecha)));
        assertTrue(trabajo.cancelacionPendiente().isPresent());
        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
    }

    @Test
    void unTrabajoCerradoNoSeCancela() {
        trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES);
        trabajo.resolverCancelacion(consejo(ResultadoConcepto.APROBADO, LUNES), "CF01", dia(LUNES));

        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.solicitarCancelacion(RUTA_SOLICITUD, LUNES.plusDays(1)));
    }

    @Test
    void unaCancelacionPendienteSinDecidirCaducaSiElTrabajoSeCierra() {
        TrabajoGrado enCurso = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, Modalidad.TRABAJO_DE_INVESTIGACION,
                "Sistema de gestión", OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L), null, null, null,
                Proceso.EN_CURSO, null, null,
                Ejecucion.iniciar(LocalDate.of(2026, 3, 20), Modalidad.TRABAJO_DE_INVESTIGACION).datos(), List.of(),
                null));
        enCurso.solicitarCancelacion(RUTA_SOLICITUD, LocalDate.of(2026, 12, 1));

        // El trabajo sigue su curso: el plazo vence y queda NO APROBADO
        assertTrue(enCurso.verificarVencimientos(dia(LocalDate.of(2026, 12, 21))));
        assertEquals(Proceso.NO_APROBADO, enCurso.getEtapa());
        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> enCurso.resolverCancelacion(
                consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 12, 22)), "CF01", dia(LocalDate.of(2026, 12, 22))));
        assertFalse(enCurso.getCancelaciones().get(0).fueAprobada());
    }

    @Test
    void lasCancelacionesSeReconstruyen() {
        Cancelacion negada = Cancelacion.solicitar(RUTA_SOLICITUD, LUNES, Proceso.PROPUESTA)
                .resolver(consejo(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(2)));
        Cancelacion pendiente = Cancelacion.solicitar("solicitudes/c2.pdf", LUNES.plusDays(4), Proceso.PROPUESTA);
        TrabajoGrado reconstruido = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L,
                Modalidad.TRABAJO_DE_INVESTIGACION, "Título", OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L),
                null, null, null, Proceso.PROPUESTA, null, null, null, null, List.of(negada, pendiente), List.of(),
                null));

        assertEquals(List.of(negada, pendiente), reconstruido.getCancelaciones());
        assertEquals(pendiente, reconstruido.cancelacionPendiente().get());
    }

    // ----- Anulación del director (borrado lógico) -----

    @Test
    void elDirectorAnulaEnPropuestaYElTrabajoQuedaArchivado() {
        trabajo.anular("  Los estudiantes cambiaron de modalidad ", "D001", dia(LUNES));

        assertEquals(Proceso.CANCELADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.ANULACION_DIRECTOR, trabajo.getMotivoCierre());
        assertTrue(trabajo.estaCerrado());
        CambioEtapa cambio = trabajo.getHistorial().get(0);
        assertEquals(Proceso.PROPUESTA, cambio.origen());
        assertEquals("D001", cambio.responsable());
        assertEquals("Los estudiantes cambiaron de modalidad", cambio.observacion());
        assertTrue(trabajo.getCancelaciones().isEmpty());
    }

    @Test
    void tambienSeAnulaEnAnteproyecto() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(3)), "C001",
                dia(LUNES.plusDays(3)));

        trabajo.anular("Sin financiación", "D001", dia(LUNES.plusDays(4)));

        assertEquals(Proceso.CANCELADO, trabajo.getEtapa());
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getHistorial().get(1).origen());
    }

    @Test
    void laAnulacionExigeJustificacion() {
        assertRegla("error.dominio.anulacion.justificacion_obligatoria", () -> trabajo.anular(" ", "D001", dia(LUNES)));
        assertRegla("error.dominio.anulacion.justificacion_obligatoria", () -> trabajo.anular(null, "D001", dia(LUNES)));
        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
    }

    @Test
    void enCursoOCerradoNoSeAnula() {
        TrabajoGrado enCurso = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, Modalidad.TRABAJO_DE_INVESTIGACION,
                "Sistema de gestión", OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L), null, null, null,
                Proceso.EN_CURSO, null, null,
                Ejecucion.iniciar(LocalDate.of(2026, 3, 20), Modalidad.TRABAJO_DE_INVESTIGACION).datos(), List.of(),
                null));
        assertRegla("error.dominio.anulacion.etapa_invalida", () -> enCurso.anular("x", "D001", dia(LUNES)));

        trabajo.anular("Primera", "D001", dia(LUNES));
        assertRegla("error.dominio.anulacion.etapa_invalida", () -> trabajo.anular("Otra", "D001", dia(LUNES)));
    }
}
