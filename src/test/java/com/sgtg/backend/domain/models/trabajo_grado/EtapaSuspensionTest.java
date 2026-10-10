package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

/** Suspensión y reanudación del trabajo de grado (Arts. 27, 57). */
class EtapaSuspensionTest {

    /** Notificación 20-mar-2026; en investigación el plazo termina el 20-dic-2026 */
    private static final LocalDate NOTIFICACION = LocalDate.of(2026, 3, 20);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 20);
    private static final String RUTA_SOLICITUD = "solicitudes/2026/suspension.pdf";
    private static final String RUTA_RESOLUCION = "resoluciones/2026/res-400.pdf";
    private static final String RUTA_REANUDACION = "solicitudes/2027/reanudacion.pdf";
    private static final LocalDate SOLICITUD = LocalDate.of(2026, 10, 1);
    /** Decisión del Consejo: 10-oct-2026 (período 2026-2); faltan 71 días para el fin */
    private static final LocalDate DECISION = LocalDate.of(2026, 10, 10);

    private TrabajoGrado trabajo;

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
        trabajo = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, Modalidad.TRABAJO_DE_INVESTIGACION,
                "Sistema de gestión", OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L), null, null, null,
                Proceso.EN_CURSO, null, null, Ejecucion.iniciar(NOTIFICACION, Modalidad.TRABAJO_DE_INVESTIGACION)
                        .datos(),
                List.of(), null));
    }

    private void suspender() {
        trabajo.solicitarSuspension(RUTA_SOLICITUD, SOLICITUD);
        trabajo.resolverSuspension(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION));
    }

    // ----- Solicitud y decisión -----

    @Test
    void aprobadaRigeDesdeLaDecisionYCongelaLosDiasRestantes() {
        trabajo.solicitarSuspension(RUTA_SOLICITUD, SOLICITUD);
        assertTrue(trabajo.getEjecucion().tieneSuspensionPendiente());
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());

        trabajo.resolverSuspension(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION));

        assertEquals(Proceso.SUSPENDIDO, trabajo.getEtapa());
        Suspension suspension = trabajo.getEjecucion().suspensionActiva().get();
        assertEquals(DECISION, suspension.fechaInicio());
        assertEquals(71, suspension.diasRestantes());
        assertEquals(Proceso.EN_CURSO, suspension.etapaPrevia());
        // Período 2026-2: se suspende también el 2027-1 y se reanuda a más tardar el 1-jul-2027
        assertEquals(LocalDate.of(2027, 7, 1), suspension.fechaMaximaReanudacion());
        CambioEtapa cambio = trabajo.getHistorial().get(trabajo.getHistorial().size() - 1);
        assertEquals(Proceso.SUSPENDIDO, cambio.destino());
        assertEquals("CF01", cambio.responsable());
    }

    @Test
    void negadaSigueEnCursoYNoConsumeLaOportunidad() {
        trabajo.solicitarSuspension(RUTA_SOLICITUD, SOLICITUD);
        trabajo.resolverSuspension(consejo(ResultadoConcepto.NO_APROBADO, DECISION), "CF01", dia(DECISION));

        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        assertEquals(FIN, trabajo.getEjecucion().getFechaFin());
        assertTrue(trabajo.getEjecucion().suspensionActiva().isEmpty());

        trabajo.solicitarSuspension(RUTA_SOLICITUD, DECISION.plusDays(5));
        trabajo.resolverSuspension(consejo(ResultadoConcepto.APROBADO, DECISION.plusDays(10)), "CF01",
                dia(DECISION.plusDays(10)));
        assertEquals(Proceso.SUSPENDIDO, trabajo.getEtapa());
        assertEquals(2, trabajo.getEjecucion().getSuspensiones().size());
    }

    @Test
    void soloSeSuspendeUnaVez() {
        suspender();
        trabajo.reanudar(RUTA_REANUDACION, "D1", dia(LocalDate.of(2027, 2, 1)));

        assertRegla("error.dominio.suspension.ya_realizada",
                () -> trabajo.solicitarSuspension(RUTA_SOLICITUD, LocalDate.of(2027, 2, 15)));
    }

    @Test
    void noSeSolicitaOtraMientrasHayaUnaPendiente() {
        trabajo.solicitarSuspension(RUTA_SOLICITUD, SOLICITUD);
        assertRegla("error.dominio.suspension.solicitud_pendiente",
                () -> trabajo.solicitarSuspension(RUTA_SOLICITUD, SOLICITUD.plusDays(1)));
    }

    @Test
    void laSolicitudExigeElDocumentoYEstarDentroDelPlazo() {
        assertRegla("error.dominio.suspension.documento_obligatorio",
                () -> trabajo.solicitarSuspension(" ", SOLICITUD));
        assertRegla("error.dominio.suspension.fuera_de_plazo",
                () -> trabajo.solicitarSuspension(RUTA_SOLICITUD, NOTIFICACION.minusDays(1)));
        assertRegla("error.dominio.suspension.fuera_de_plazo",
                () -> trabajo.solicitarSuspension(RUTA_SOLICITUD, FIN.plusDays(1)));
        assertTrue(trabajo.getEjecucion().getSuspensiones().isEmpty());
    }

    @Test
    void laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido() {
        assertRegla("error.dominio.suspension.sin_solicitud_pendiente", () -> trabajo
                .resolverSuspension(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION)));

        trabajo.solicitarSuspension(RUTA_SOLICITUD, SOLICITUD);
        assertRegla("error.dominio.suspension.resolucion_obligatoria", () -> trabajo.resolverSuspension(
                new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, null, null, DECISION),
                "CF01", dia(DECISION)));
        assertRegla("error.dominio.suspension.organo_invalido", () -> trabajo.resolverSuspension(
                new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, null, RUTA_RESOLUCION, null,
                        DECISION),
                "CF01", dia(DECISION)));
        assertRegla("error.dominio.suspension.resultado_invalido", () -> trabajo
                .resolverSuspension(consejo(ResultadoConcepto.RECHAZADO, DECISION), "CF01", dia(DECISION)));
        assertRegla("error.dominio.suspension.fecha_invalida", () -> trabajo.resolverSuspension(
                consejo(ResultadoConcepto.APROBADO, SOLICITUD.minusDays(1)), "CF01", dia(DECISION)));
        assertTrue(trabajo.getEjecucion().tieneSuspensionPendiente());
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
    }

    @Test
    void noSeApruebaSiElPlazoYaTermino() {
        trabajo.solicitarSuspension(RUTA_SOLICITUD, LocalDate.of(2026, 12, 15));
        LocalDate tarde = LocalDate.of(2026, 12, 21);

        assertRegla("error.dominio.suspension.plazo_vencido", () -> trabajo
                .resolverSuspension(consejo(ResultadoConcepto.APROBADO, tarde), "CF01", dia(tarde)));
        assertTrue(trabajo.getEjecucion().tieneSuspensionPendiente());
    }

    // ----- Reanudación -----

    @Test
    void elDirectorReanudaYElFinEsLaReanudacionMasLosDiasCongelados() {
        suspender();
        LocalDate reanudacion = LocalDate.of(2027, 2, 1);

        trabajo.reanudar(RUTA_REANUDACION, "D1", dia(reanudacion));

        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        // 1-feb-2027 + 71 días
        assertEquals(LocalDate.of(2027, 4, 13), trabajo.getEjecucion().getFechaFin());
        Suspension reanudada = trabajo.getEjecucion().getSuspensiones().get(0);
        assertEquals(reanudacion, reanudada.fechaReanudacion());
        assertEquals(RUTA_REANUDACION, reanudada.rutaReanudacion());
        assertFalse(reanudada.fueReanudadaAutomaticamente());
        assertEquals("D1", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
    }

    @Test
    void laReanudacionExigeDocumentoYUnaSuspensionVigente() {
        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.reanudar(RUTA_REANUDACION, "D1", dia(DECISION)));
        suspender();
        assertRegla("error.dominio.suspension.documento_reanudacion_obligatorio",
                () -> trabajo.reanudar(" ", "D1", dia(LocalDate.of(2027, 2, 1))));
        assertEquals(Proceso.SUSPENDIDO, trabajo.getEtapa());
    }

    @Test
    void desdeProrrogaVuelveAProrroga() {
        trabajo.solicitarProrroga(2, "solicitudes/prorroga.pdf", LocalDate.of(2026, 9, 1));
        trabajo.resolverProrroga(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 9, 10)), "CF01",
                dia(LocalDate.of(2026, 9, 10)));
        assertEquals(LocalDate.of(2027, 2, 20), trabajo.getEjecucion().getFechaFin());

        suspender();
        assertEquals(Proceso.PRORROGA, trabajo.getEjecucion().suspensionActiva().get().etapaPrevia());
        trabajo.reanudar(RUTA_REANUDACION, "D1", dia(LocalDate.of(2027, 2, 1)));

        assertEquals(Proceso.PRORROGA, trabajo.getEtapa());
        // Faltaban 133 días (10-oct-2026 a 20-feb-2027)
        assertEquals(LocalDate.of(2027, 2, 1).plusDays(133), trabajo.getEjecucion().getFechaFin());
    }

    @Test
    void suspendidoNoAdmiteProrrogaNiInformes() {
        suspender();
        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.solicitarProrroga(1, "solicitudes/p.pdf", DECISION.plusDays(1)));
        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.solicitarSuspension(RUTA_SOLICITUD, DECISION.plusDays(1)));
    }

    // ----- Verificación diaria -----

    @Test
    void alLlegarLaFechaMaximaSeReanudaAutomaticamente() {
        suspender();

        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2027, 6, 30))));
        assertEquals(Proceso.SUSPENDIDO, trabajo.getEtapa());

        assertTrue(trabajo.verificarVencimientos(dia(LocalDate.of(2027, 7, 1))));

        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        Suspension reanudada = trabajo.getEjecucion().getSuspensiones().get(0);
        assertTrue(reanudada.fueReanudadaAutomaticamente());
        assertEquals(LocalDate.of(2027, 7, 1), reanudada.fechaReanudacion());
        assertEquals(LocalDate.of(2027, 9, 10), trabajo.getEjecucion().getFechaFin());
        assertEquals(TrabajoGrado.RESPONSABLE_SISTEMA,
                trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
    }

    @Test
    void siLaVerificacionLlegaTardeReanudaEnLaFechaMaximaYAplicaElVencimiento() {
        suspender();

        // Reanuda el 1-jul-2027 (fin 10-sep-2027) y, como ya pasó, queda NO APROBADO
        assertTrue(trabajo.verificarVencimientos(dia(LocalDate.of(2027, 12, 1))));

        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.VENCIMIENTO_PLAZO, trabajo.getMotivoCierre());
        assertEquals(LocalDate.of(2027, 7, 1), trabajo.getEjecucion().getSuspensiones().get(0).fechaReanudacion());
    }

    @Test
    void unaSuspensionPendienteDetieneElVencimiento() {
        trabajo.solicitarSuspension(RUTA_SOLICITUD, LocalDate.of(2026, 12, 15));
        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2026, 12, 21))));
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
    }

    // ----- Período académico y reconstrucción -----

    /** La suspensión cubre el resto del período en que se aprueba y el período siguiente completo. */
    @ParameterizedTest
    @CsvSource({ "2026-01-15, 2027-01-01", "2026-06-30, 2027-01-01", "2026-07-01, 2027-07-01",
            "2026-12-31, 2027-07-01" })
    void laFechaMaximaEsElInicioDelPeriodoPosteriorAlSiguiente(LocalDate inicio, LocalDate maxima) {
        assertEquals(maxima, Suspension.fechaMaximaReanudacion(inicio));
    }

    @Test
    void laSuspensionSeReconstruyeConSuEstado() {
        suspender();

        DatosEjecucion datos = trabajo.getEjecucion().datos();
        Ejecucion reconstruida = new Ejecucion(datos);

        assertEquals(datos, reconstruida.datos());
        assertTrue(reconstruida.suspensionActiva().isPresent());
        assertNull(reconstruida.suspensionActiva().get().fechaReanudacion());
    }
}
