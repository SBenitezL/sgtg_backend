package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.asesorOrganizacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.cartaAceptacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.directorPlanta;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.estudiante;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.evaluador;
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
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/**
 * Solicitud, programación (con jurado coordinador), nueva fecha, calificación
 * y cierre de la sustentación (Arts. 19-25, 46-55).
 */
class EtapaSustentacionTest {

    /** Notificación 20-mar-2026: en investigación se solicita desde el 20-jun y hasta el 20-dic-2026 */
    private static final LocalDate NOTIFICACION = LocalDate.of(2026, 3, 20);
    private static final LocalDate SOLICITUD = LocalDate.of(2026, 8, 3);
    private static final LocalDate PROGRAMACION = LocalDate.of(2026, 8, 10);
    /** Jueves 20 de agosto de 2026: primera sesión */
    private static final LocalDate SESION = LocalDate.of(2026, 8, 20);
    private static final String DEFINITIVA = "definitivas/2026/tg-7.pdf";
    private static final String ACTA = "actas/tg-7.pdf";
    private static final String FORMATO_E = "formatos/e-tg-7.pdf";
    private static final String PAZ_Y_SALVO = "paz-y-salvo/tg-7.pdf";

    private final CalendarioHabil calendario = new CalendarioHabilFalso();
    /** Jurado coordinador */
    private final Evaluador coordinador = evaluador(30L);
    private final Evaluador jurado = evaluador(31L);
    private TrabajoGrado trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    private static LocalDateTime dia(LocalDate fecha) {
        return fecha.atTime(10, 0);
    }

    /** Trabajo en curso; los evaluadores de su anteproyecto fueron los usuarios 20 y 21. */
    private static TrabajoGrado enCurso(Modalidad modalidad) {
        boolean practica = modalidad == Modalidad.PRACTICA_PROFESIONAL;
        DatosAnteproyecto anteproyecto = new DatosAnteproyecto(List.of(), List.of(evaluador(20L), evaluador(21L)),
                List.of(), List.of(), null, null);
        return TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, modalidad, "Sistema de gestión", OBJETIVOS,
                List.of(estudiante(10L)), directorPlanta(1L), null, practica ? asesorOrganizacion() : null,
                practica ? cartaAceptacion() : null, Proceso.EN_CURSO, null, anteproyecto,
                Ejecucion.iniciar(NOTIFICACION, modalidad).datos(), List.of(), null));
    }

    private static Decision consejo(ResultadoConcepto resultado, LocalDate fecha) {
        return new Decision(Organo.CONSEJO_FACULTAD, resultado, null, "resoluciones/nueva-fecha.pdf", null, fecha);
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = enCurso(Modalidad.TRABAJO_DE_INVESTIGACION);
    }

    /** Solicitada por el director, con el paz y salvo aprobado por Decanatura. */
    private void solicitar() {
        trabajo.solicitarSustentacion("monografias/tg-7.pdf", "finales/tg-7.pdf", null,
                null, "D1", dia(SOLICITUD));
        trabajo.revisarPazYSalvo(PAZ_Y_SALVO, true, null, dia(SOLICITUD));
    }

    private void programar(LocalDate fecha, LocalDate programacion) {
        trabajo.programarSustentacion(coordinador, jurado, fecha.atTime(14, 0), "Salón 301", FORMATO_E, "CF01",
                dia(programacion));
    }

    private void calificar(Calificacion calificacion, LocalDate fecha) {
        String observaciones = calificacion == Calificacion.APROBADO ? null : "Observaciones del jurado";
        trabajo.calificarSustentacion(coordinador, calificacion, observaciones, ACTA, dia(fecha), calendario);
    }

    /** Solicitada, programada y calificada por el coordinador en la primera sesión. */
    private void primeraSesion(Calificacion calificacion) {
        solicitar();
        programar(SESION, PROGRAMACION);
        calificar(calificacion, SESION);
    }

    // ----- Solicitud -----

    @Test
    void laSolicitudPasaASustentacionYDetieneElVencimiento() {
        trabajo.solicitarSustentacion("monografias/tg-7.pdf", "finales/tg-7.pdf", null,
                "anexos/tg-7.pdf", "D1", dia(SOLICITUD));

        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
        Sustentacion sustentacion = trabajo.getSustentacion();
        assertEquals(EstadoSustentacion.SOLICITADA, sustentacion.getEstado());
        assertEquals("monografias/tg-7.pdf", sustentacion.getRutaMonografia());
        assertEquals("finales/tg-7.pdf", sustentacion.getRutaDocumentoFinal());
        // El paz y salvo lo revisa Decanatura después
        assertNull(sustentacion.getRutaPazYSalvo());
        assertEquals("anexos/tg-7.pdf", sustentacion.getRutaAnexos());
        assertEquals("D1", trabajo.getHistorial().get(0).responsable());
        // Con la sustentación iniciada, el fin del plazo ya no cierra el trabajo
        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2027, 1, 15))));
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
    }

    @Test
    void seSolicitaDespuesDeLosMesesMinimosYAntesDelFin() {
        assertRegla("error.dominio.sustentacion.antes_de_tiempo", () -> trabajo.solicitarSustentacion("m.pdf",
                "f.pdf", null, null, "D1", dia(LocalDate.of(2026, 6, 19))));
        assertRegla("error.dominio.sustentacion.plazo_vencido", () -> trabajo.solicitarSustentacion("m.pdf",
                "f.pdf", null, null, "D1", dia(LocalDate.of(2026, 12, 21))));
        trabajo.solicitarSustentacion("m.pdf", "f.pdf", null, null, "D1", dia(LocalDate.of(2026, 6, 20)));
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
        assertNull(trabajo.getSustentacion().getRutaAnexos());
    }

    @Test
    void exigeMonografiaDocumentoFinalYEnPracticaElConceptoDelAsesor() {
        assertRegla("error.dominio.sustentacion.monografia_obligatoria",
                () -> trabajo.solicitarSustentacion(null, "f.pdf", null, null, "D1", dia(SOLICITUD)));
        assertRegla("error.dominio.sustentacion.documento_final_obligatorio",
                () -> trabajo.solicitarSustentacion("m.pdf", " ", null, null, "D1", dia(SOLICITUD)));

        TrabajoGrado practica = enCurso(Modalidad.PRACTICA_PROFESIONAL);
        LocalDateTime septiembre = dia(LocalDate.of(2026, 9, 21));
        assertRegla("error.dominio.sustentacion.concepto_asesor_obligatorio",
                () -> practica.solicitarSustentacion("m.pdf", "f.pdf", null, null, "D1", septiembre));
        practica.solicitarSustentacion("m.pdf", "f.pdf", "conceptos/asesor.pdf", null, "D1", septiembre);
        assertEquals("conceptos/asesor.pdf", practica.getSustentacion().getRutaConceptoAsesor());
    }

    @Test
    void noSeSolicitaConUnaProrrogaPendiente() {
        trabajo.solicitarProrroga(2, "solicitudes/prorroga.pdf", LocalDate.of(2026, 7, 1));
        assertRegla("error.dominio.sustentacion.decision_pendiente", this::solicitar);
    }

    // ----- Paz y salvo (Decanatura) -----

    @Test
    void sinPazYSalvoAprobadoNoSeProgramaYDecanaturaPuedeNoAprobarlo() {
        trabajo.solicitarSustentacion("m.pdf", "f.pdf", null, null, "D1", dia(SOLICITUD));
        assertRegla("error.dominio.sustentacion.paz_y_salvo_pendiente", () -> programar(SESION, PROGRAMACION));

        trabajo.revisarPazYSalvo("paz/f-1.pdf", false, " Deuda en biblioteca ", dia(SOLICITUD.plusDays(1)));
        assertNull(trabajo.getSustentacion().getRutaPazYSalvo());
        assertRegla("error.dominio.sustentacion.paz_y_salvo_pendiente", () -> programar(SESION, PROGRAMACION));

        trabajo.revisarPazYSalvo("paz/f-2.pdf", true, null, dia(SOLICITUD.plusDays(2)));
        assertEquals("paz/f-2.pdf", trabajo.getSustentacion().getRutaPazYSalvo());
        assertEquals("Deuda en biblioteca", trabajo.getSustentacion().getRevisionesPazYSalvo().get(0).observaciones());
        programar(SESION, PROGRAMACION);
        assertEquals(EstadoSustentacion.PROGRAMADA, trabajo.getSustentacion().getEstado());
    }

    @Test
    void laRevisionDelPazYSalvoExigeDocumentoYSiNoSeApruebaObservaciones() {
        trabajo.solicitarSustentacion("m.pdf", "f.pdf", null, null, "D1", dia(SOLICITUD));
        assertRegla("error.dominio.sustentacion.paz_y_salvo_obligatorio",
                () -> trabajo.revisarPazYSalvo(" ", true, null, dia(SOLICITUD)));
        assertRegla("error.dominio.sustentacion.paz_y_salvo_obligatorio",
                () -> trabajo.revisarPazYSalvo(null, false, "Deuda", dia(SOLICITUD)));
        assertRegla("error.dominio.sustentacion.paz_y_salvo_observaciones_obligatorias",
                () -> trabajo.revisarPazYSalvo("paz/f.pdf", false, " ", dia(SOLICITUD)));
        assertRegla("error.dominio.sustentacion.paz_y_salvo_fecha_invalida",
                () -> trabajo.revisarPazYSalvo("paz/f.pdf", true, null, dia(SOLICITUD.minusDays(1))));
        assertTrue(trabajo.getSustentacion().getRevisionesPazYSalvo().isEmpty());
    }

    @Test
    void unPazYSalvoAprobadoNoSeVuelveARevisarYSoloSeRevisaEnSustentacion() {
        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.revisarPazYSalvo("paz/f.pdf", true, null, dia(SOLICITUD)));
        solicitar();
        assertRegla("error.dominio.sustentacion.paz_y_salvo_no_permitido",
                () -> trabajo.revisarPazYSalvo("paz/otro.pdf", true, null, dia(SOLICITUD)));
    }

    // ----- Programación -----

    @Test
    void elConsejoProgramaCoordinadorJuradoFechaSalonYFormatoE() {
        solicitar();
        programar(SESION, PROGRAMACION);

        Sustentacion sustentacion = trabajo.getSustentacion();
        assertEquals(EstadoSustentacion.PROGRAMADA, sustentacion.getEstado());
        SesionSustentacion sesion = sustentacion.sesionActual();
        assertEquals(1, sesion.numero());
        assertEquals(SESION.atTime(14, 0), sesion.fechaHora());
        assertEquals("Salón 301", sesion.salon());
        assertEquals(FORMATO_E, sesion.rutaFormatoE());
        assertEquals(30L, sesion.coordinador().getUsuarioId());
        assertTrue(sesion.esCoordinador(30L));
        assertFalse(sesion.esCoordinador(31L));
        assertTrue(trabajo.esJurado(30L));
        assertTrue(trabajo.participa(31L));
        assertRegla("error.dominio.sustentacion.programacion_no_permitida", () -> programar(SESION, PROGRAMACION));
    }

    @Test
    void laProgramacionValidaJuradosFechaYFormatoE() {
        solicitar();
        LocalDateTime fecha = SESION.atTime(14, 0);
        LocalDateTime ahora = dia(PROGRAMACION);
        assertRegla("error.dominio.sustentacion.jurado_con_conflicto", () -> trabajo.programarSustentacion(
                coordinador, evaluador(1L), fecha, "Salón 301", FORMATO_E, "CF01", ahora));
        assertRegla("error.dominio.sustentacion.jurados_repetidos", () -> trabajo.programarSustentacion(
                coordinador, evaluador(30L), fecha, "Salón 301", FORMATO_E, "CF01", ahora));
        assertRegla("error.dominio.sustentacion.jurados_obligatorios", () -> trabajo.programarSustentacion(
                coordinador, null, fecha, "Salón 301", FORMATO_E, "CF01", ahora));
        assertRegla("error.dominio.sustentacion.programacion_incompleta", () -> trabajo.programarSustentacion(
                coordinador, jurado, fecha, " ", FORMATO_E, "CF01", ahora));
        assertRegla("error.dominio.sustentacion.fecha_sesion_invalida", () -> trabajo.programarSustentacion(
                coordinador, jurado, PROGRAMACION.minusDays(1).atTime(14, 0), "Salón 301", FORMATO_E, "CF01", ahora));
        assertRegla("error.dominio.sustentacion.formato_e_obligatorio", () -> trabajo.programarSustentacion(
                coordinador, jurado, fecha, "Salón 301", null, "CF01", ahora));
        assertEquals(EstadoSustentacion.SOLICITADA, trabajo.getSustentacion().getEstado());
    }

    /** Los evaluadores del anteproyecto no pueden ser jurados del mismo trabajo (sí de otros). */
    @Test
    void losEvaluadoresDelAnteproyectoNoSonJuradosDelMismoTrabajo() {
        solicitar();
        LocalDateTime fecha = SESION.atTime(14, 0);
        assertRegla("error.dominio.sustentacion.jurado_con_conflicto", () -> trabajo.programarSustentacion(
                evaluador(20L), jurado, fecha, "Salón 301", FORMATO_E, "CF01", dia(PROGRAMACION)));
        assertRegla("error.dominio.sustentacion.jurado_con_conflicto", () -> trabajo.programarSustentacion(
                coordinador, evaluador(21L), fecha, "Salón 301", FORMATO_E, "CF01", dia(PROGRAMACION)));
    }

    // ----- Calificación (solo el coordinador) -----

    @Test
    void elCoordinadorApruebaYCargaLaVersionDefinitiva() {
        primeraSesion(Calificacion.APROBADO);
        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
        assertEquals(ACTA, trabajo.getSustentacion().getCalificaciones().get(0).rutaActa());

        assertRegla("error.dominio.sustentacion.jurado_no_asignado",
                () -> trabajo.cargarVersionDefinitiva(evaluador(99L), DEFINITIVA, dia(SESION.plusDays(5))));
        assertRegla("error.dominio.sustentacion.solo_coordinador",
                () -> trabajo.cargarVersionDefinitiva(jurado, DEFINITIVA, dia(SESION.plusDays(5))));
        trabajo.cargarVersionDefinitiva(coordinador, DEFINITIVA, dia(SESION.plusDays(5)));

        assertEquals(Proceso.FINALIZADO, trabajo.getEtapa());
        assertTrue(trabajo.estaCerrado());
        assertNull(trabajo.getMotivoCierre());
        assertEquals(DEFINITIVA, trabajo.getSustentacion().getRutaVersionDefinitiva());
        assertEquals(EstadoSustentacion.FINALIZADA, trabajo.getSustentacion().getEstado());
    }

    /** La calificación del coordinador define el resultado de la sesión. */
    @ParameterizedTest
    @CsvSource({ "APROBADO_CON_OBSERVACIONES, EN_AJUSTES, SUSTENTACION", "APLAZADO, APLAZADA, APLAZADO",
            "NO_APROBADO, NO_APROBADA, NO_APROBADO" })
    void laCalificacionDelCoordinadorDefineElResultado(Calificacion calificacion, EstadoSustentacion estado,
            Proceso etapa) {
        primeraSesion(calificacion);
        assertEquals(estado, trabajo.getSustentacion().getEstado());
        assertEquals(etapa, trabajo.getEtapa());
    }

    @Test
    void noAprobadoCierraElTrabajo() {
        primeraSesion(Calificacion.NO_APROBADO);
        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.SUSTENTACION_NO_APROBADA, trabajo.getMotivoCierre());
        assertEquals("V30", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
    }

    @Test
    void laCalificacionValidaCoordinadorActaFechaYObservaciones() {
        solicitar();
        programar(SESION, PROGRAMACION);
        assertRegla("error.dominio.sustentacion.jurado_no_asignado", () -> trabajo.calificarSustentacion(
                evaluador(99L), Calificacion.APROBADO, null, ACTA, dia(SESION), calendario));
        assertRegla("error.dominio.sustentacion.solo_coordinador", () -> trabajo.calificarSustentacion(jurado,
                Calificacion.APROBADO, null, ACTA, dia(SESION), calendario));
        assertRegla("error.dominio.sustentacion.acta_obligatoria", () -> trabajo.calificarSustentacion(
                coordinador, Calificacion.APROBADO, null, null, dia(SESION), calendario));
        assertRegla("error.dominio.sustentacion.fecha_registro_invalida",
                () -> calificar(Calificacion.APROBADO, SESION.minusDays(1)));
        assertRegla("error.dominio.sustentacion.observaciones_obligatorias", () -> trabajo.calificarSustentacion(
                coordinador, Calificacion.APLAZADO, " ", ACTA, dia(SESION), calendario));
        calificar(Calificacion.APROBADO, SESION);
        assertEquals(1, trabajo.getSustentacion().getCalificaciones().size());
    }

    // ----- Nueva fecha de la sesión -----

    @Test
    void elDirectorSolicitaNuevaFechaYElConsejoLaAprueba() {
        solicitar();
        programar(SESION, PROGRAMACION);
        trabajo.solicitarNuevaFechaSustentacion("solicitudes/nueva-fecha.pdf", dia(LocalDate.of(2026, 8, 15)));

        SolicitudNuevaFecha pendiente = trabajo.getSustentacion().nuevaFechaPendiente().orElseThrow();
        assertEquals(SESION.atTime(14, 0), pendiente.fechaHoraAnterior());
        assertRegla("error.dominio.nueva_fecha.solicitud_pendiente", () -> trabajo
                .solicitarNuevaFechaSustentacion("otra.pdf", dia(LocalDate.of(2026, 8, 16))));
        // Mientras el Consejo no decide, el coordinador no califica
        assertRegla("error.dominio.nueva_fecha.solicitud_pendiente", () -> calificar(Calificacion.APROBADO, SESION));
        assertRegla("error.dominio.sustentacion.nueva_fecha_obligatoria", () -> trabajo
                .resolverNuevaFechaSustentacion(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 8, 17)), null,
                        null));

        LocalDateTime nueva = LocalDate.of(2026, 9, 10).atTime(9, 0);
        trabajo.resolverNuevaFechaSustentacion(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 8, 17)), nueva,
                "Auditorio");

        SesionSustentacion sesion = trabajo.getSustentacion().sesionActual();
        assertEquals(nueva, sesion.fechaHora());
        assertEquals("Auditorio", sesion.salon());
        assertEquals(1, sesion.numero());
        assertEquals(30L, sesion.coordinador().getUsuarioId());
        assertEquals(EstadoSustentacion.PROGRAMADA, trabajo.getSustentacion().getEstado());
        assertTrue(trabajo.getSustentacion().nuevaFechaPendiente().isEmpty());
        // La calificación ahora es desde la nueva fecha
        assertRegla("error.dominio.sustentacion.fecha_registro_invalida",
                () -> calificar(Calificacion.APROBADO, SESION));
        calificar(Calificacion.APROBADO, LocalDate.of(2026, 9, 10));
        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
    }

    @Test
    void siElConsejoNoApruebaLaNuevaFechaLaSesionSeMantiene() {
        solicitar();
        programar(SESION, PROGRAMACION);
        trabajo.solicitarNuevaFechaSustentacion("solicitudes/nueva-fecha.pdf", dia(LocalDate.of(2026, 8, 15)));
        trabajo.resolverNuevaFechaSustentacion(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.NO_APROBADO,
                null, "resoluciones/r.pdf", "No procede", LocalDate.of(2026, 8, 17)), null, null);

        assertEquals(SESION.atTime(14, 0), trabajo.getSustentacion().sesionActual().fechaHora());
        SolicitudNuevaFecha resuelta = trabajo.getSustentacion().getSolicitudesNuevaFecha().get(0);
        assertFalse(resuelta.fueAprobada());
        assertNull(resuelta.nuevaFechaHora());
        calificar(Calificacion.APROBADO, SESION);
    }

    @Test
    void laNuevaFechaSeSolicitaAntesDeLaSesionYConSuDocumento() {
        solicitar();
        assertRegla("error.dominio.nueva_fecha.no_permitida", () -> trabajo
                .solicitarNuevaFechaSustentacion("s.pdf", dia(LocalDate.of(2026, 8, 5))));
        programar(SESION, PROGRAMACION);
        assertRegla("error.dominio.nueva_fecha.fuera_de_plazo",
                () -> trabajo.solicitarNuevaFechaSustentacion("s.pdf", dia(SESION)));
        assertRegla("error.dominio.nueva_fecha.documento_obligatorio",
                () -> trabajo.solicitarNuevaFechaSustentacion(" ", dia(LocalDate.of(2026, 8, 15))));
        assertRegla("error.dominio.nueva_fecha.sin_solicitud_pendiente", () -> trabajo.resolverNuevaFechaSustentacion(
                consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 8, 17)), SESION.atTime(9, 0), null));
    }

    // ----- Ajustes -----

    @Test
    void conObservacionesElDirectorRadicaAjustesYAnexosYElCoordinadorLosAprueba() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES);
        // 10 días hábiles desde el jueves 20-ago-2026
        assertEquals(LocalDate.of(2026, 9, 3), trabajo.getSustentacion().getFechaLimiteAjustes());

        trabajo.radicarAjustesSustentacion("ajustes/tg-7.pdf", "anexos/ajustes-tg-7.pdf",
                dia(LocalDate.of(2026, 9, 1)));
        assertEquals(EstadoSustentacion.AJUSTES_RADICADOS, trabajo.getSustentacion().getEstado());
        assertEquals("anexos/ajustes-tg-7.pdf", trabajo.getSustentacion().getRutaAnexosAjustes());
        assertRegla("error.dominio.sustentacion.solo_coordinador", () -> trabajo
                .verificarAjustesSustentacion(jurado, true, null, dia(LocalDate.of(2026, 9, 2))));
        trabajo.verificarAjustesSustentacion(coordinador, true, null, dia(LocalDate.of(2026, 9, 2)));

        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
        trabajo.cargarVersionDefinitiva(coordinador, DEFINITIVA, dia(LocalDate.of(2026, 9, 4)));
        assertEquals(Proceso.FINALIZADO, trabajo.getEtapa());
    }

    @Test
    void siLosAjustesNoCumplenSeAplaza() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES);
        trabajo.radicarAjustesSustentacion("ajustes/tg-7.pdf", null, dia(LocalDate.of(2026, 9, 1)));
        assertNull(trabajo.getSustentacion().getRutaAnexosAjustes());
        assertRegla("error.dominio.sustentacion.observaciones_obligatorias", () -> trabajo
                .verificarAjustesSustentacion(coordinador, false, null, dia(LocalDate.of(2026, 9, 2))));
        trabajo.verificarAjustesSustentacion(coordinador, false, "No se corrigió el capítulo 3",
                dia(LocalDate.of(2026, 9, 2)));

        assertEquals(Proceso.APLAZADO, trabajo.getEtapa());
        assertEquals(LocalDate.of(2026, 11, 1), trabajo.getSustentacion().getFechaLimiteSegundaSesion());
    }

    @Test
    void siNoSeRadicanLosAjustesATiempoSeAplaza() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES);
        assertRegla("error.dominio.sustentacion.ajustes_fuera_de_plazo", () -> trabajo
                .radicarAjustesSustentacion("ajustes/tg-7.pdf", null, dia(LocalDate.of(2026, 9, 4))));

        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2026, 9, 3))));
        assertTrue(trabajo.verificarVencimientos(dia(LocalDate.of(2026, 9, 4))));

        assertEquals(Proceso.APLAZADO, trabajo.getEtapa());
        assertEquals(TrabajoGrado.RESPONSABLE_SISTEMA,
                trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
        // El plazo de la segunda sesión cuenta desde el día siguiente al vencimiento de los ajustes
        assertEquals(LocalDate.of(2026, 9, 4).plusDays(60), trabajo.getSustentacion().getFechaLimiteSegundaSesion());
    }

    // ----- Aplazamiento y segunda sesión -----

    @Test
    void aplazadoSeProgramaUnaSegundaSesionDentroDeSesentaDias() {
        primeraSesion(Calificacion.APLAZADO);
        assertEquals(Proceso.APLAZADO, trabajo.getEtapa());
        // 20-ago-2026 + 60 días
        assertEquals(LocalDate.of(2026, 10, 19), trabajo.getSustentacion().getFechaLimiteSegundaSesion());
        assertRegla("error.dominio.sustentacion.segunda_sesion_fuera_de_plazo",
                () -> programar(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 9, 1)));

        programar(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 9, 1));
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
        assertEquals(2, trabajo.getSustentacion().sesionActual().numero());

        calificar(Calificacion.APROBADO, LocalDate.of(2026, 10, 15));
        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
    }

    @Test
    void unSegundoAplazamientoCierraElTrabajo() {
        primeraSesion(Calificacion.APLAZADO);
        programar(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 9, 1));
        calificar(Calificacion.APLAZADO, LocalDate.of(2026, 10, 15));

        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.SUSTENTACION_NO_APROBADA, trabajo.getMotivoCierre());
    }

    @Test
    void sinSegundaSesionEnSesentaDiasQuedaNoAprobadoPorVencimiento() {
        primeraSesion(Calificacion.APLAZADO);

        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2026, 10, 19))));
        assertTrue(trabajo.verificarVencimientos(dia(LocalDate.of(2026, 10, 20))));

        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.VENCIMIENTO_PLAZO, trabajo.getMotivoCierre());
        assertEquals(Proceso.APLAZADO, trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).origen());
    }

    // ----- Inasistencia -----

    @Test
    void laInasistenciaAplazaLaPrimeraSesionYCierraLaSegunda() {
        solicitar();
        programar(SESION, PROGRAMACION);
        assertRegla("error.dominio.sustentacion.observaciones_obligatorias",
                () -> trabajo.registrarInasistencia(coordinador, null, dia(SESION)));
        assertRegla("error.dominio.sustentacion.solo_coordinador",
                () -> trabajo.registrarInasistencia(jurado, "No se presentó", dia(SESION)));
        trabajo.registrarInasistencia(coordinador, "El estudiante no se presentó", dia(SESION));
        assertEquals(Proceso.APLAZADO, trabajo.getEtapa());

        programar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1));
        trabajo.registrarInasistencia(coordinador, "El estudiante no se presentó de nuevo",
                dia(LocalDate.of(2026, 10, 1)));

        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.INASISTENCIA_SUSTENTACION, trabajo.getMotivoCierre());
        assertEquals(2, trabajo.getSustentacion().getInasistencias().size());
    }

    @Test
    void noSeRegistraInasistenciaEnUnaSesionYaCalificada() {
        solicitar();
        programar(SESION, PROGRAMACION);
        calificar(Calificacion.APROBADO, SESION);
        assertRegla("error.dominio.sustentacion.registro_no_permitido",
                () -> trabajo.registrarInasistencia(coordinador, "No se presentó", dia(SESION)));
    }

    // ----- Reconstrucción -----

    @Test
    void laSustentacionSeReconstruyeConSuEstado() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES);
        trabajo.radicarAjustesSustentacion("ajustes/tg-7.pdf", "anexos/a.pdf", dia(LocalDate.of(2026, 9, 1)));

        DatosSustentacion datos = trabajo.getSustentacion().datos();
        Sustentacion reconstruida = new Sustentacion(datos);

        assertEquals(datos, reconstruida.datos());
        assertEquals(EstadoSustentacion.AJUSTES_RADICADOS, reconstruida.getEstado());
        assertTrue(reconstruida.jurado(31L).isPresent());
        assertEquals("monografias/tg-7.pdf", reconstruida.getRutaMonografia());
    }
}
