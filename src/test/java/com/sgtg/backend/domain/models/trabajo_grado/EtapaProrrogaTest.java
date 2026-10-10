package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.asesorOrganizacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.cartaAceptacion;
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
import org.junit.jupiter.params.provider.ValueSource;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

/** Prórroga del plazo de ejecución (Arts. 16, 41). */
class EtapaProrrogaTest {

    /** Notificación de la aprobación: 20-mar-2026; en investigación el plazo termina el 20-dic-2026 */
    private static final LocalDate NOTIFICACION = LocalDate.of(2026, 3, 20);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 20);
    private static final String RUTA_SOLICITUD = "solicitudes/2026/prorroga.pdf";
    private static final String RUTA_RESOLUCION = "resoluciones/2026/res-300.pdf";
    private static final LocalDate SOLICITUD = LocalDate.of(2026, 11, 2);
    private static final LocalDate DECISION = LocalDate.of(2026, 11, 13);

    private TrabajoGrado trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    /** Trabajo reconstruido en curso, con la ejecución iniciada en la notificación. */
    private static TrabajoGrado enCurso(Modalidad modalidad) {
        boolean practica = modalidad == Modalidad.PRACTICA_PROFESIONAL;
        return TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, modalidad, "Sistema de gestión", OBJETIVOS,
                List.of(estudiante(10L)), directorPlanta(1L), null, practica ? asesorOrganizacion() : null,
                practica ? cartaAceptacion() : null, Proceso.EN_CURSO, null, null,
                Ejecucion.iniciar(NOTIFICACION, modalidad).datos(), List.of(), null));
    }

    private static Decision consejo(ResultadoConcepto resultado, LocalDate fecha) {
        return new Decision(Organo.CONSEJO_FACULTAD, resultado, null, RUTA_RESOLUCION, null, fecha);
    }

    private static LocalDateTime dia(LocalDate fecha) {
        return fecha.atTime(10, 0);
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = enCurso(Modalidad.TRABAJO_DE_INVESTIGACION);
    }

    @Test
    void aprobadaExtiendeElPlazoYPasaAProrroga() {
        trabajo.solicitarProrroga(2, RUTA_SOLICITUD, SOLICITUD);
        assertTrue(trabajo.getEjecucion().tieneProrrogaPendiente());
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());

        trabajo.resolverProrroga(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION));

        assertEquals(Proceso.PRORROGA, trabajo.getEtapa());
        assertEquals(LocalDate.of(2027, 2, 20), trabajo.getEjecucion().getFechaFin());
        Prorroga prorroga = trabajo.getEjecucion().getProrroga();
        assertTrue(prorroga.fueAprobada());
        assertEquals(RUTA_SOLICITUD, prorroga.rutaSolicitud());
        assertEquals(RUTA_RESOLUCION, prorroga.decision().rutaArchivo());
        CambioEtapa cambio = trabajo.getHistorial().get(trabajo.getHistorial().size() - 1);
        assertEquals(Proceso.EN_CURSO, cambio.origen());
        assertEquals(Proceso.PRORROGA, cambio.destino());
        assertEquals("CF01", cambio.responsable());
    }

    @Test
    void negadaSigueEnCursoYConsumeLaUnicaOportunidad() {
        trabajo.solicitarProrroga(3, RUTA_SOLICITUD, SOLICITUD);
        trabajo.resolverProrroga(consejo(ResultadoConcepto.NO_APROBADO, DECISION), "CF01", dia(DECISION));

        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        assertEquals(FIN, trabajo.getEjecucion().getFechaFin());
        assertFalse(trabajo.getEjecucion().tieneProrrogaPendiente());
        assertTrue(trabajo.getHistorial().isEmpty());
        assertRegla("error.dominio.prorroga.ya_solicitada",
                () -> trabajo.solicitarProrroga(1, RUTA_SOLICITUD, DECISION.plusDays(1)));
    }

    @Test
    void soloSeSolicitaUnaVez() {
        trabajo.solicitarProrroga(1, RUTA_SOLICITUD, SOLICITUD);
        assertRegla("error.dominio.prorroga.ya_solicitada",
                () -> trabajo.solicitarProrroga(2, RUTA_SOLICITUD, SOLICITUD.plusDays(1)));
        assertEquals(1, trabajo.getEjecucion().getProrroga().meses());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 4, -1 })
    void losMesesVanDeUnoATres(int meses) {
        assertRegla("error.dominio.prorroga.meses_invalidos",
                () -> trabajo.solicitarProrroga(meses, RUTA_SOLICITUD, SOLICITUD));
        assertNull(trabajo.getEjecucion().getProrroga());
    }

    @Test
    void laSolicitudExigeElDocumento() {
        assertRegla("error.dominio.prorroga.documento_obligatorio",
                () -> trabajo.solicitarProrroga(2, " ", SOLICITUD));
    }

    @Test
    void seSolicitaConAlMenosQuinceDiasDeAnticipacion() {
        assertEquals(LocalDate.of(2026, 12, 5), trabajo.getEjecucion().fechaLimiteSolicitudProrroga());
        assertRegla("error.dominio.prorroga.fuera_de_plazo",
                () -> trabajo.solicitarProrroga(2, RUTA_SOLICITUD, LocalDate.of(2026, 12, 6)));
        assertRegla("error.dominio.prorroga.fuera_de_plazo",
                () -> trabajo.solicitarProrroga(2, RUTA_SOLICITUD, NOTIFICACION.minusDays(1)));

        trabajo.solicitarProrroga(2, RUTA_SOLICITUD, LocalDate.of(2026, 12, 5));
        assertTrue(trabajo.getEjecucion().tieneProrrogaPendiente());
    }

    @Test
    void laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido() {
        assertRegla("error.dominio.prorroga.sin_solicitud_pendiente", () -> trabajo
                .resolverProrroga(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION)));

        trabajo.solicitarProrroga(2, RUTA_SOLICITUD, SOLICITUD);
        assertRegla("error.dominio.prorroga.resolucion_obligatoria", () -> trabajo.resolverProrroga(
                new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, null, null, DECISION),
                "CF01", dia(DECISION)));
        assertRegla("error.dominio.prorroga.organo_invalido", () -> trabajo.resolverProrroga(
                new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, null, RUTA_RESOLUCION, null,
                        DECISION),
                "CF01", dia(DECISION)));
        assertRegla("error.dominio.prorroga.resultado_invalido", () -> trabajo
                .resolverProrroga(consejo(ResultadoConcepto.CON_CORRECCIONES, DECISION), "CF01", dia(DECISION)));
        assertRegla("error.dominio.prorroga.fecha_invalida", () -> trabajo.resolverProrroga(
                consejo(ResultadoConcepto.APROBADO, SOLICITUD.minusDays(1)), "CF01", dia(DECISION)));
        // Ninguna decisión inválida modificó la prórroga ni el plazo
        assertTrue(trabajo.getEjecucion().tieneProrrogaPendiente());
        assertEquals(FIN, trabajo.getEjecucion().getFechaFin());
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
    }

    @Test
    void enProrrogaNoSeVuelveASolicitarNiADecidir() {
        trabajo.solicitarProrroga(2, RUTA_SOLICITUD, SOLICITUD);
        trabajo.resolverProrroga(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION));

        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.solicitarProrroga(1, RUTA_SOLICITUD, DECISION));
        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> trabajo
                .resolverProrroga(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION)));
    }

    @Test
    void elVencimientoEsperaLaDecisionDelConsejo() {
        trabajo.solicitarProrroga(2, RUTA_SOLICITUD, LocalDate.of(2026, 12, 5));

        // El plazo venció, pero la prórroga sigue pendiente
        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2026, 12, 21))));
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());

        // El Consejo la niega después del fin: en la siguiente verificación se cierra
        LocalDate negada = LocalDate.of(2026, 12, 22);
        trabajo.resolverProrroga(consejo(ResultadoConcepto.NO_APROBADO, negada), "CF01", dia(negada));
        assertTrue(trabajo.verificarVencimientos(dia(LocalDate.of(2026, 12, 23))));
        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.VENCIMIENTO_PLAZO, trabajo.getMotivoCierre());
    }

    @Test
    void enProrrogaVenceAlTerminarElPlazoExtendido() {
        trabajo.solicitarProrroga(1, RUTA_SOLICITUD, SOLICITUD);
        trabajo.resolverProrroga(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION));

        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2027, 1, 20))));
        assertTrue(trabajo.verificarVencimientos(dia(LocalDate.of(2027, 1, 21))));
        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        CambioEtapa cierre = trabajo.getHistorial().get(trabajo.getHistorial().size() - 1);
        assertEquals(Proceso.PRORROGA, cierre.origen());
    }

    @Test
    void laPracticaProfesionalTambienTieneProrrogaYSigueRadicandoInformes() {
        TrabajoGrado practica = enCurso(Modalidad.PRACTICA_PROFESIONAL);
        practica.solicitarProrroga(3, RUTA_SOLICITUD, LocalDate.of(2027, 2, 1));
        practica.resolverProrroga(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2027, 2, 10)), "CF01",
                dia(LocalDate.of(2027, 2, 10)));

        assertEquals(Proceso.PRORROGA, practica.getEtapa());
        assertEquals(LocalDate.of(2027, 6, 20), practica.getEjecucion().getFechaFin());
        assertEquals(1, practica.registrarInformeMensual("informes/2027/abril.pdf", LocalDate.of(2027, 4, 20))
                .numero());
    }

    @Test
    void laProrrogaSeReconstruyeConSuDecision() {
        trabajo.solicitarProrroga(2, RUTA_SOLICITUD, SOLICITUD);
        trabajo.resolverProrroga(consejo(ResultadoConcepto.APROBADO, DECISION), "CF01", dia(DECISION));

        DatosEjecucion datos = trabajo.getEjecucion().datos();
        Ejecucion reconstruida = new Ejecucion(datos);

        assertEquals(datos, reconstruida.datos());
        assertEquals(LocalDate.of(2027, 2, 20), reconstruida.getFechaFin());
        assertTrue(reconstruida.getProrroga().fueAprobada());
    }
}
