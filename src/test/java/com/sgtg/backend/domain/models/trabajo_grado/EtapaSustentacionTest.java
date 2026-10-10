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
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Solicitud, programación, calificación y cierre de la sustentación (Arts. 19-25, 46-55). */
class EtapaSustentacionTest {

    /** Notificación 20-mar-2026: en investigación se solicita desde el 20-jun y hasta el 20-dic-2026 */
    private static final LocalDate NOTIFICACION = LocalDate.of(2026, 3, 20);
    private static final LocalDate SOLICITUD = LocalDate.of(2026, 8, 3);
    private static final LocalDate PROGRAMACION = LocalDate.of(2026, 8, 10);
    /** Jueves 20 de agosto de 2026: primera sesión */
    private static final LocalDate SESION = LocalDate.of(2026, 8, 20);
    private static final String DEFINITIVA = "definitivas/2026/tg-7.pdf";

    private final CalendarioHabil calendario = new CalendarioHabilFalso();
    private final Evaluador jurado1 = evaluador(30L);
    private final Evaluador jurado2 = evaluador(31L);
    private TrabajoGrado trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    private static LocalDateTime dia(LocalDate fecha) {
        return fecha.atTime(10, 0);
    }

    private static TrabajoGrado enCurso(Modalidad modalidad) {
        boolean practica = modalidad == Modalidad.PRACTICA_PROFESIONAL;
        return TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, modalidad, "Sistema de gestión", OBJETIVOS,
                List.of(estudiante(10L)), directorPlanta(1L), null, practica ? asesorOrganizacion() : null,
                practica ? cartaAceptacion() : null, Proceso.EN_CURSO, null, null,
                Ejecucion.iniciar(NOTIFICACION, modalidad).datos(), List.of(), null));
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = enCurso(Modalidad.TRABAJO_DE_INVESTIGACION);
    }

    private void solicitar() {
        trabajo.solicitarSustentacion("finales/tg-7.pdf", "paz-y-salvo/tg-7.pdf", null, "D1", dia(SOLICITUD));
    }

    private void programar(LocalDate fecha, LocalDate programacion) {
        trabajo.programarSustentacion(jurado1, jurado2, fecha.atTime(14, 0), "Salón 301", "CF01",
                dia(programacion));
    }

    private void calificar(Evaluador jurado, Calificacion calificacion, LocalDate fecha) {
        String observaciones = calificacion == Calificacion.APROBADO ? null : "Observaciones del jurado";
        trabajo.calificarSustentacion(jurado, calificacion, observaciones, null, dia(fecha), calendario);
    }

    /** Solicitada, programada y calificada en la primera sesión con los resultados dados. */
    private void primeraSesion(Calificacion primera, Calificacion segunda) {
        solicitar();
        programar(SESION, PROGRAMACION);
        calificar(jurado1, primera, SESION);
        calificar(jurado2, segunda, SESION);
    }

    // ----- Solicitud -----

    @Test
    void laSolicitudPasaASustentacionYDetieneElVencimiento() {
        solicitar();

        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
        Sustentacion sustentacion = trabajo.getSustentacion();
        assertEquals(EstadoSustentacion.SOLICITADA, sustentacion.getEstado());
        assertEquals("finales/tg-7.pdf", sustentacion.getRutaDocumentoFinal());
        assertEquals("paz-y-salvo/tg-7.pdf", sustentacion.getRutaPazYSalvo());
        assertEquals("D1", trabajo.getHistorial().get(0).responsable());
        // Con la sustentación iniciada, el fin del plazo ya no cierra el trabajo
        assertFalse(trabajo.verificarVencimientos(dia(LocalDate.of(2027, 1, 15))));
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
    }

    @Test
    void seSolicitaDespuesDeLosMesesMinimosYAntesDelFin() {
        assertRegla("error.dominio.sustentacion.antes_de_tiempo", () -> trabajo.solicitarSustentacion("f.pdf",
                "p.pdf", null, "D1", dia(LocalDate.of(2026, 6, 19))));
        assertRegla("error.dominio.sustentacion.plazo_vencido", () -> trabajo.solicitarSustentacion("f.pdf",
                "p.pdf", null, "D1", dia(LocalDate.of(2026, 12, 21))));
        trabajo.solicitarSustentacion("f.pdf", "p.pdf", null, "D1", dia(LocalDate.of(2026, 6, 20)));
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
    }

    @Test
    void exigeDocumentoFinalPazYSalvoYEnPracticaElConceptoDelAsesor() {
        assertRegla("error.dominio.sustentacion.documento_final_obligatorio",
                () -> trabajo.solicitarSustentacion(" ", "p.pdf", null, "D1", dia(SOLICITUD)));
        assertRegla("error.dominio.sustentacion.paz_y_salvo_obligatorio",
                () -> trabajo.solicitarSustentacion("f.pdf", null, null, "D1", dia(SOLICITUD)));

        TrabajoGrado practica = enCurso(Modalidad.PRACTICA_PROFESIONAL);
        LocalDateTime septiembre = dia(LocalDate.of(2026, 9, 21));
        assertRegla("error.dominio.sustentacion.concepto_asesor_obligatorio",
                () -> practica.solicitarSustentacion("f.pdf", "p.pdf", null, "D1", septiembre));
        practica.solicitarSustentacion("f.pdf", "p.pdf", "conceptos/asesor.pdf", "D1", septiembre);
        assertEquals("conceptos/asesor.pdf", practica.getSustentacion().getRutaConceptoAsesor());
    }

    @Test
    void noSeSolicitaConUnaProrrogaPendiente() {
        trabajo.solicitarProrroga(2, "solicitudes/prorroga.pdf", LocalDate.of(2026, 7, 1));
        assertRegla("error.dominio.sustentacion.decision_pendiente", this::solicitar);
    }

    // ----- Programación -----

    @Test
    void elConsejoProgramaJuradosFechaYSalon() {
        solicitar();
        programar(SESION, PROGRAMACION);

        Sustentacion sustentacion = trabajo.getSustentacion();
        assertEquals(EstadoSustentacion.PROGRAMADA, sustentacion.getEstado());
        SesionSustentacion sesion = sustentacion.sesionActual();
        assertEquals(1, sesion.numero());
        assertEquals(SESION.atTime(14, 0), sesion.fechaHora());
        assertEquals("Salón 301", sesion.salon());
        assertTrue(trabajo.esJurado(30L));
        assertTrue(trabajo.participa(31L));
        assertRegla("error.dominio.sustentacion.programacion_no_permitida", () -> programar(SESION, PROGRAMACION));
    }

    @Test
    void laProgramacionValidaJuradosYFecha() {
        solicitar();
        LocalDateTime fecha = SESION.atTime(14, 0);
        assertRegla("error.dominio.sustentacion.jurado_con_conflicto", () -> trabajo.programarSustentacion(jurado1,
                evaluador(1L), fecha, "Salón 301", "CF01", dia(PROGRAMACION)));
        assertRegla("error.dominio.sustentacion.jurados_repetidos", () -> trabajo.programarSustentacion(jurado1,
                evaluador(30L), fecha, "Salón 301", "CF01", dia(PROGRAMACION)));
        assertRegla("error.dominio.sustentacion.jurados_obligatorios", () -> trabajo.programarSustentacion(jurado1,
                null, fecha, "Salón 301", "CF01", dia(PROGRAMACION)));
        assertRegla("error.dominio.sustentacion.programacion_incompleta", () -> trabajo.programarSustentacion(
                jurado1, jurado2, fecha, " ", "CF01", dia(PROGRAMACION)));
        assertRegla("error.dominio.sustentacion.fecha_sesion_invalida", () -> trabajo.programarSustentacion(
                jurado1, jurado2, PROGRAMACION.minusDays(1).atTime(14, 0), "Salón 301", "CF01", dia(PROGRAMACION)));
        assertEquals(EstadoSustentacion.SOLICITADA, trabajo.getSustentacion().getEstado());
    }

    // ----- Calificación -----

    @Test
    void ambosApruebanYUnJuradoCargaLaVersionDefinitiva() {
        solicitar();
        programar(SESION, PROGRAMACION);
        calificar(jurado1, Calificacion.APROBADO, SESION);
        assertEquals(EstadoSustentacion.PROGRAMADA, trabajo.getSustentacion().getEstado());
        calificar(jurado2, Calificacion.APROBADO, SESION);
        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());

        assertRegla("error.dominio.sustentacion.jurado_no_asignado",
                () -> trabajo.cargarVersionDefinitiva(evaluador(99L), DEFINITIVA, dia(SESION.plusDays(5))));
        trabajo.cargarVersionDefinitiva(jurado2, DEFINITIVA, dia(SESION.plusDays(5)));

        assertEquals(Proceso.FINALIZADO, trabajo.getEtapa());
        assertTrue(trabajo.estaCerrado());
        assertNull(trabajo.getMotivoCierre());
        assertEquals(DEFINITIVA, trabajo.getSustentacion().getRutaVersionDefinitiva());
        assertEquals(EstadoSustentacion.FINALIZADA, trabajo.getSustentacion().getEstado());
    }

    /** Si los jurados no coinciden, rige la calificación menos favorable. */
    @ParameterizedTest
    @CsvSource({ "APROBADO, APROBADO_CON_OBSERVACIONES, EN_AJUSTES, SUSTENTACION",
            "APROBADO_CON_OBSERVACIONES, APLAZADO, APLAZADA, APLAZADO",
            "APROBADO, NO_APROBADO, NO_APROBADA, NO_APROBADO" })
    void rigeLaCalificacionMenosFavorable(Calificacion primera, Calificacion segunda, EstadoSustentacion estado,
            Proceso etapa) {
        primeraSesion(primera, segunda);
        assertEquals(estado, trabajo.getSustentacion().getEstado());
        assertEquals(etapa, trabajo.getEtapa());
    }

    @Test
    void noAprobadoCierraElTrabajo() {
        primeraSesion(Calificacion.NO_APROBADO, Calificacion.NO_APROBADO);
        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.SUSTENTACION_NO_APROBADA, trabajo.getMotivoCierre());
        assertEquals("V31", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
    }

    @Test
    void laCalificacionValidaJuradoFechaYObservaciones() {
        solicitar();
        programar(SESION, PROGRAMACION);
        assertRegla("error.dominio.sustentacion.jurado_no_asignado",
                () -> calificar(evaluador(99L), Calificacion.APROBADO, SESION));
        assertRegla("error.dominio.sustentacion.fecha_registro_invalida",
                () -> calificar(jurado1, Calificacion.APROBADO, SESION.minusDays(1)));
        assertRegla("error.dominio.sustentacion.observaciones_obligatorias", () -> trabajo.calificarSustentacion(
                jurado1, Calificacion.APLAZADO, " ", null, dia(SESION), calendario));
        calificar(jurado1, Calificacion.APROBADO, SESION);
        assertRegla("error.dominio.sustentacion.calificacion_repetida",
                () -> calificar(jurado1, Calificacion.APROBADO, SESION));
        assertEquals(1, trabajo.getSustentacion().getCalificaciones().size());
    }

    // ----- Ajustes -----

    @Test
    void conObservacionesElDirectorRadicaAjustesYLosJuradosLosAprueban() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES, Calificacion.APROBADO);
        // 10 días hábiles desde el jueves 20-ago-2026
        assertEquals(LocalDate.of(2026, 9, 3), trabajo.getSustentacion().getFechaLimiteAjustes());

        trabajo.radicarAjustesSustentacion("ajustes/tg-7.pdf", dia(LocalDate.of(2026, 9, 1)));
        assertEquals(EstadoSustentacion.AJUSTES_RADICADOS, trabajo.getSustentacion().getEstado());
        trabajo.verificarAjustesSustentacion(jurado1, true, null, dia(LocalDate.of(2026, 9, 2)));
        trabajo.verificarAjustesSustentacion(jurado2, true, null, dia(LocalDate.of(2026, 9, 2)));

        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
        trabajo.cargarVersionDefinitiva(jurado1, DEFINITIVA, dia(LocalDate.of(2026, 9, 4)));
        assertEquals(Proceso.FINALIZADO, trabajo.getEtapa());
    }

    @Test
    void siLosAjustesNoCumplenSeAplaza() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES, Calificacion.APROBADO_CON_OBSERVACIONES);
        trabajo.radicarAjustesSustentacion("ajustes/tg-7.pdf", dia(LocalDate.of(2026, 9, 1)));
        trabajo.verificarAjustesSustentacion(jurado1, true, null, dia(LocalDate.of(2026, 9, 2)));
        assertRegla("error.dominio.sustentacion.observaciones_obligatorias", () -> trabajo
                .verificarAjustesSustentacion(jurado2, false, null, dia(LocalDate.of(2026, 9, 2))));
        trabajo.verificarAjustesSustentacion(jurado2, false, "No se corrigió el capítulo 3",
                dia(LocalDate.of(2026, 9, 2)));

        assertEquals(Proceso.APLAZADO, trabajo.getEtapa());
        assertEquals(LocalDate.of(2026, 11, 1), trabajo.getSustentacion().getFechaLimiteSegundaSesion());
    }

    @Test
    void siNoSeRadicanLosAjustesATiempoSeAplaza() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES, Calificacion.APROBADO);
        assertRegla("error.dominio.sustentacion.ajustes_fuera_de_plazo", () -> trabajo
                .radicarAjustesSustentacion("ajustes/tg-7.pdf", dia(LocalDate.of(2026, 9, 4))));

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
        primeraSesion(Calificacion.APLAZADO, Calificacion.APROBADO);
        assertEquals(Proceso.APLAZADO, trabajo.getEtapa());
        // 20-ago-2026 + 60 días
        assertEquals(LocalDate.of(2026, 10, 19), trabajo.getSustentacion().getFechaLimiteSegundaSesion());
        assertRegla("error.dominio.sustentacion.segunda_sesion_fuera_de_plazo",
                () -> programar(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 9, 1)));

        programar(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 9, 1));
        assertEquals(Proceso.SUSTENTACION, trabajo.getEtapa());
        assertEquals(2, trabajo.getSustentacion().sesionActual().numero());

        calificar(jurado1, Calificacion.APROBADO, LocalDate.of(2026, 10, 15));
        calificar(jurado2, Calificacion.APROBADO, LocalDate.of(2026, 10, 15));
        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
    }

    @Test
    void unSegundoAplazamientoCierraElTrabajo() {
        primeraSesion(Calificacion.APLAZADO, Calificacion.APLAZADO);
        programar(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 9, 1));
        calificar(jurado1, Calificacion.APLAZADO, LocalDate.of(2026, 10, 15));
        calificar(jurado2, Calificacion.APROBADO, LocalDate.of(2026, 10, 15));

        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.SUSTENTACION_NO_APROBADA, trabajo.getMotivoCierre());
    }

    @Test
    void sinSegundaSesionEnSesentaDiasQuedaNoAprobadoPorVencimiento() {
        primeraSesion(Calificacion.APLAZADO, Calificacion.APLAZADO);

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
                () -> trabajo.registrarInasistencia(jurado1, null, dia(SESION)));
        trabajo.registrarInasistencia(jurado1, "El estudiante no se presentó", dia(SESION));
        assertEquals(Proceso.APLAZADO, trabajo.getEtapa());

        programar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1));
        trabajo.registrarInasistencia(jurado2, "El estudiante no se presentó de nuevo",
                dia(LocalDate.of(2026, 10, 1)));

        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.INASISTENCIA_SUSTENTACION, trabajo.getMotivoCierre());
        assertEquals(2, trabajo.getSustentacion().getInasistencias().size());
    }

    @Test
    void noSeRegistraInasistenciaEnUnaSesionYaCalificada() {
        solicitar();
        programar(SESION, PROGRAMACION);
        calificar(jurado1, Calificacion.APROBADO, SESION);
        assertRegla("error.dominio.sustentacion.sesion_ya_calificada",
                () -> trabajo.registrarInasistencia(jurado2, "No se presentó", dia(SESION)));
    }

    // ----- Reconstrucción -----

    @Test
    void laSustentacionSeReconstruyeConSuEstado() {
        primeraSesion(Calificacion.APROBADO_CON_OBSERVACIONES, Calificacion.APROBADO);
        trabajo.radicarAjustesSustentacion("ajustes/tg-7.pdf", dia(LocalDate.of(2026, 9, 1)));

        DatosSustentacion datos = trabajo.getSustentacion().datos();
        Sustentacion reconstruida = new Sustentacion(datos);

        assertEquals(datos, reconstruida.datos());
        assertEquals(EstadoSustentacion.AJUSTES_RADICADOS, reconstruida.getEstado());
        assertTrue(reconstruida.jurado(31L).isPresent());
    }
}
