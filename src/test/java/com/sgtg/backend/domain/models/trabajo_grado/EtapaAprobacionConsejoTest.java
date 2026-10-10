package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.LUNES;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_ANTEPROYECTO;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_PROPUESTA;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.asesorOrganizacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.cartaAceptacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.decisionComite;
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
import org.junit.jupiter.params.provider.EnumSource;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Aprobación del anteproyecto por el Consejo de Facultad e inicio de la ejecución (Art. 14). */
class EtapaAprobacionConsejoTest {

    private static final String RUTA_RESOLUCION = "resoluciones/2026/res-100.pdf";
    /** Jueves 12 de marzo de 2026: conceptos de los evaluadores */
    private static final LocalDate CONCEPTOS = LocalDate.of(2026, 3, 12);
    /** Viernes 20 de marzo de 2026: decisión del Consejo */
    private static final LocalDate CONSEJO = LocalDate.of(2026, 3, 20);

    private final CalendarioHabil calendario = new CalendarioHabilFalso();
    private final Evaluador evaluador1 = evaluador(20L);
    private final Evaluador evaluador2 = evaluador(21L);
    private TrabajoGrado trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    private static Decision decisionConsejo(ResultadoConcepto resultado, String ruta, LocalDate fecha) {
        return new Decision(Organo.CONSEJO_FACULTAD, resultado, null, ruta, null, fecha);
    }

    private static LocalDateTime dia(LocalDate fecha) {
        return fecha.atTime(15, 0);
    }

    /** Ambos evaluadores aprueban la versión actual en la fecha dada. */
    private void evaluadoresAprueban(TrabajoGrado trabajo, LocalDate fecha) {
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador1, ResultadoConcepto.APROBADO, null, fecha), null,
                dia(fecha), calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador2, ResultadoConcepto.APROBADO, null, fecha), null,
                dia(fecha), calendario);
    }

    /** Lleva el trabajo hasta la aprobación del Consejo: propuesta y anteproyecto aprobados. */
    private TrabajoGrado enAprobacionConsejo(TrabajoGrado nuevo) {
        nuevo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        nuevo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(3)), "C001",
                dia(LUNES.plusDays(3)));
        nuevo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", dia(LUNES.plusDays(7)), calendario);
        nuevo.asignarEvaluadores(evaluador1, evaluador2, "J001", dia(LUNES.plusDays(8)), calendario);
        evaluadoresAprueban(nuevo, CONCEPTOS);
        return nuevo;
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = enAprobacionConsejo(DatosPrueba.investigacion());
        assertEquals(Proceso.APROBACION_CONSEJO, trabajo.getEtapa());
    }

    @Test
    void aprobadoPasaAEnCursoEIniciaLosNueveMesesDeInvestigacion() {
        trabajo.registrarDecisionConsejo(decisionConsejo(ResultadoConcepto.APROBADO, RUTA_RESOLUCION, CONSEJO),
                "CF01", dia(CONSEJO));

        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        Ejecucion ejecucion = trabajo.getEjecucion();
        // La fecha de la decisión es la de notificación: desde ahí corren los plazos
        assertEquals(CONSEJO, ejecucion.getFechaNotificacion());
        assertEquals(LocalDate.of(2026, 12, 20), ejecucion.getFechaFin());
        assertEquals(LocalDate.of(2026, 6, 20), ejecucion.getFechaMinimaSustentacion());
        Decision registrada = trabajo.getAnteproyecto().getDecisionesConsejo().get(0);
        assertEquals(RUTA_RESOLUCION, registrada.rutaArchivo());
        assertNull(registrada.acta());
        CambioEtapa cambio = trabajo.getHistorial().get(trabajo.getHistorial().size() - 1);
        assertEquals(Proceso.APROBACION_CONSEJO, cambio.origen());
        assertEquals(Proceso.EN_CURSO, cambio.destino());
        assertEquals("CF01", cambio.responsable());
    }

    @Test
    void laPracticaProfesionalTieneDeSeisMesesAUnAnio() {
        TrabajoGrado practica = enAprobacionConsejo(new PracticaProfesional(null, "Práctica", OBJETIVOS,
                estudiante(10L), directorPlanta(1L), null, asesorOrganizacion(), cartaAceptacion()));

        practica.registrarDecisionConsejo(decisionConsejo(ResultadoConcepto.APROBADO, RUTA_RESOLUCION, CONSEJO),
                "CF01", dia(CONSEJO));

        assertEquals(LocalDate.of(2027, 3, 20), practica.getEjecucion().getFechaFin());
        assertEquals(LocalDate.of(2026, 9, 20), practica.getEjecucion().getFechaMinimaSustentacion());
    }

    @Test
    void noAprobadoVuelveAAnteproyectoYLaNuevaVersionPasaPorLosEvaluadores() {
        trabajo.registrarDecisionConsejo(decisionConsejo(ResultadoConcepto.NO_APROBADO, RUTA_RESOLUCION, CONSEJO),
                "CF01", dia(CONSEJO));

        Anteproyecto anteproyecto = trabajo.getAnteproyecto();
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals(Estado.NO_APROBADO, anteproyecto.getEstado());
        assertNull(trabajo.getEjecucion());

        // El director radica una nueva versión: vuelve a los mismos evaluadores
        LocalDate reenvio = LocalDate.of(2026, 3, 30);
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", dia(reenvio), calendario);
        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertEquals(2, anteproyecto.versionActual().get().version());
        assertTrue(anteproyecto.conceptosVersionActual().isEmpty());

        evaluadoresAprueban(trabajo, reenvio);
        assertEquals(Proceso.APROBACION_CONSEJO, trabajo.getEtapa());
        LocalDate segunda = LocalDate.of(2026, 4, 10);
        trabajo.registrarDecisionConsejo(decisionConsejo(ResultadoConcepto.APROBADO, RUTA_RESOLUCION, segunda),
                "CF01", dia(segunda));

        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        assertEquals(segunda, trabajo.getEjecucion().getFechaNotificacion());
        assertEquals(2, anteproyecto.getDecisionesConsejo().size());
        assertEquals(List.of(Proceso.APROBACION_CONSEJO, Proceso.ANTEPROYECTO, Proceso.EVALUACION,
                Proceso.APROBACION_CONSEJO, Proceso.EN_CURSO),
                trabajo.getHistorial().stream().skip(2).map(CambioEtapa::destino).toList());
    }

    @Test
    void elComentarioEsOpcionalAunqueNoApruebe() {
        Decision sinComentario = decisionConsejo(ResultadoConcepto.NO_APROBADO, RUTA_RESOLUCION, CONSEJO);
        assertNull(sinComentario.observaciones());

        trabajo.registrarDecisionConsejo(sinComentario, "CF01", dia(CONSEJO));
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
    }

    @Test
    void laRutaDeLaResolucionEsObligatoria() {
        assertRegla("error.dominio.anteproyecto.resolucion_obligatoria", () -> trabajo.registrarDecisionConsejo(
                decisionConsejo(ResultadoConcepto.APROBADO, null, CONSEJO), "CF01", dia(CONSEJO)));
        assertRegla("error.dominio.anteproyecto.resolucion_obligatoria", () -> trabajo.registrarDecisionConsejo(
                decisionConsejo(ResultadoConcepto.NO_APROBADO, "  ", CONSEJO), "CF01", dia(CONSEJO)));
        assertEquals(Proceso.APROBACION_CONSEJO, trabajo.getEtapa());
        assertTrue(trabajo.getAnteproyecto().getDecisionesConsejo().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = ResultadoConcepto.class, names = { "APROBADO", "NO_APROBADO" },
            mode = EnumSource.Mode.EXCLUDE)
    void elConsejoSoloApruebaONoAprueba(ResultadoConcepto resultado) {
        assertRegla("error.dominio.anteproyecto.resultado_consejo_invalido", () -> trabajo.registrarDecisionConsejo(
                decisionConsejo(resultado, RUTA_RESOLUCION, CONSEJO), "CF01", dia(CONSEJO)));
        assertEquals(Proceso.APROBACION_CONSEJO, trabajo.getEtapa());
        assertTrue(trabajo.getAnteproyecto().getDecisionesConsejo().isEmpty());
    }

    @Test
    void laDecisionDebeEmitirlaElConsejoDeFacultad() {
        Decision delComite = new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, null, RUTA_RESOLUCION,
                null, CONSEJO);
        assertRegla("error.dominio.anteproyecto.organo_invalido",
                () -> trabajo.registrarDecisionConsejo(delComite, "CF01", dia(CONSEJO)));
    }

    @Test
    void laDecisionNoPuedeSerAnteriorALosConceptosDeLosEvaluadores() {
        LocalDate antes = CONCEPTOS.minusDays(1);
        assertRegla("error.dominio.anteproyecto.fecha_invalida", () -> trabajo.registrarDecisionConsejo(
                decisionConsejo(ResultadoConcepto.APROBADO, RUTA_RESOLUCION, antes), "CF01", dia(antes)));
    }

    @Test
    void soloSeDecideEnLaEtapaDeAprobacionDelConsejo() {
        trabajo.registrarDecisionConsejo(decisionConsejo(ResultadoConcepto.APROBADO, RUTA_RESOLUCION, CONSEJO),
                "CF01", dia(CONSEJO));

        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> trabajo.registrarDecisionConsejo(
                decisionConsejo(ResultadoConcepto.APROBADO, RUTA_RESOLUCION, CONSEJO), "CF01", dia(CONSEJO)));
        assertEquals(1, trabajo.getAnteproyecto().getDecisionesConsejo().size());
    }

    @Test
    void laEjecucionInformaSusPlazos() {
        trabajo.registrarDecisionConsejo(decisionConsejo(ResultadoConcepto.APROBADO, RUTA_RESOLUCION, CONSEJO),
                "CF01", dia(CONSEJO));
        Ejecucion ejecucion = trabajo.getEjecucion();

        assertFalse(ejecucion.permiteSolicitarSustentacion(LocalDate.of(2026, 6, 19)));
        assertTrue(ejecucion.permiteSolicitarSustentacion(LocalDate.of(2026, 6, 20)));
        assertFalse(ejecucion.vencida(LocalDate.of(2026, 12, 20)));
        assertTrue(ejecucion.vencida(LocalDate.of(2026, 12, 21)));
        assertEquals(10, ejecucion.diasRestantes(LocalDate.of(2026, 12, 10)));
    }

    @Test
    void laEjecucionSeReconstruyeConSusFechas() {
        DatosEjecucion datos = new DatosEjecucion(CONSEJO, LocalDate.of(2026, 12, 20), LocalDate.of(2026, 6, 20));
        TrabajoGrado reconstruido = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L,
                Modalidad.TRABAJO_DE_INVESTIGACION, "Título", OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L),
                null, null, null, Proceso.EN_CURSO, null, null, datos, List.of(), null));

        assertEquals(datos, reconstruido.getEjecucion().datos());
        assertThrows(ReglaNegocioException.class, () -> new DatosEjecucion(CONSEJO, null, CONSEJO));
    }
}
