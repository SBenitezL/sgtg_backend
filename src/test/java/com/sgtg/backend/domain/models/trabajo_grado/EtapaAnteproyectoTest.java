package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.LUNES;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_ANTEPROYECTO;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_FORMATO_B;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_FORMATO_C;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_PROPUESTA;
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
import org.junit.jupiter.params.provider.ValueSource;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Flujo de la etapa de anteproyecto y evaluación (Arts. 12-13, 37-38). */
class EtapaAnteproyectoTest {

    private final CalendarioHabil calendario = new CalendarioHabilFalso();
    /** Lunes 9 de marzo de 2026: radicación del anteproyecto */
    private final LocalDateTime radicacion = LUNES.plusDays(7).atTime(9, 0);
    /** Martes 10 de marzo de 2026: asignación de evaluadores */
    private final LocalDateTime asignacion = LUNES.plusDays(8).atTime(9, 0);
    private final Evaluador evaluador1 = evaluador(20L);
    private final Evaluador evaluador2 = evaluador(21L);
    private TrabajoInvestigacion trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    private static Concepto concepto(Evaluador emisor, ResultadoConcepto resultado, LocalDate fecha) {
        String observaciones = resultado == ResultadoConcepto.APROBADO ? null : "Ajustar la metodología";
        return new Concepto(emisor, resultado, observaciones, fecha);
    }

    /** Lleva el trabajo hasta la etapa de anteproyecto con la propuesta aprobada. */
    private static TrabajoInvestigacion enAnteproyecto(TrabajoInvestigacion trabajo, CalendarioHabil calendario) {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(3)), "C001",
                LUNES.plusDays(3).atTime(10, 0));
        return trabajo;
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = enAnteproyecto(DatosPrueba.investigacion(), calendario);
    }

    private void radicarYAsignar() {
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);
        trabajo.asignarEvaluadores(evaluador1, evaluador2, "J001", asignacion, calendario);
    }

    private LocalDateTime dia(LocalDate fecha) {
        return fecha.atTime(15, 0);
    }

    // ----- Radicación -----

    @Test
    void laPrimeraVersionQuedaEnRevisionEsperandoLosEvaluadores() {
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);

        Anteproyecto anteproyecto = trabajo.getAnteproyecto();
        assertEquals(Estado.EN_REVISION, anteproyecto.getEstado());
        assertEquals(1, anteproyecto.versionActual().get().version());
        assertEquals(TipoDocumento.ANTEPROYECTO, anteproyecto.versionActual().get().tipo());
        assertNull(anteproyecto.getFechaLimiteEvaluacion());
        // Sin evaluadores el plazo aún no corre, así que no puede vencer
        assertFalse(anteproyecto.evaluacionVencida(radicacion.toLocalDate().plusYears(1)));
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals(1, trabajo.getHistorial().size());
    }

    @Test
    void noSeEmiteConceptoMientrasNoHayEvaluadoresAsignados() {
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);

        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador1, ResultadoConcepto.APROBADO, radicacion.toLocalDate()), RUTA_FORMATO_B, radicacion,
                calendario));
        assertEquals(Estado.EN_REVISION, trabajo.getAnteproyecto().getEstado());
        assertTrue(trabajo.getAnteproyecto().getConceptos().isEmpty());
    }

    @Test
    void soloSeRadicaEnLaEtapaDeAnteproyecto() {
        TrabajoInvestigacion enPropuesta = DatosPrueba.investigacion();
        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> enPropuesta.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario));
    }

    @Test
    void noSeRadicaOtraVersionMientrasLaActualEstePendiente() {
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);

        assertRegla("error.dominio.anteproyecto.radicacion_no_permitida",
                () -> trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario));
        assertEquals(1, trabajo.getAnteproyecto().getVersiones().size());
    }

    // ----- Asignación de evaluadores -----

    @Test
    void asignarEvaluadoresPasaAEvaluacionYCalculaElPlazo() {
        radicarYAsignar();

        Anteproyecto anteproyecto = trabajo.getAnteproyecto();
        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertEquals(Estado.EN_REVISION, anteproyecto.getEstado());
        assertEquals(List.of(evaluador1, evaluador2), anteproyecto.getEvaluadores());
        // 10 días hábiles desde el martes 10-mar-2026
        assertEquals(LocalDate.of(2026, 3, 24), anteproyecto.getFechaLimiteEvaluacion());
        CambioEtapa cambio = trabajo.getHistorial().get(1);
        assertEquals(Proceso.ANTEPROYECTO, cambio.origen());
        assertEquals(Proceso.EVALUACION, cambio.destino());
        assertEquals("J001", cambio.responsable());
        assertTrue(trabajo.esEvaluador(20L));
        assertTrue(trabajo.participa(21L));
    }

    @Test
    void noSeAsignanEvaluadoresSinAnteproyectoRadicado() {
        assertRegla("error.dominio.anteproyecto.asignacion_no_permitida",
                () -> trabajo.asignarEvaluadores(evaluador1, evaluador2, "J001", asignacion, calendario));
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
    }

    @Test
    void seExigenDosEvaluadoresDistintos() {
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);

        assertRegla("error.dominio.anteproyecto.evaluadores_obligatorios",
                () -> trabajo.asignarEvaluadores(evaluador1, null, "J001", asignacion, calendario));
        assertRegla("error.dominio.anteproyecto.evaluadores_repetidos",
                () -> trabajo.asignarEvaluadores(evaluador1, evaluador(20L), "J001", asignacion, calendario));
        assertFalse(trabajo.getAnteproyecto().tieneEvaluadores());
        assertEquals(Estado.EN_REVISION, trabajo.getAnteproyecto().getEstado());
    }

    @Test
    void laAsignacionNoPuedeSerAnteriorALaRadicacion() {
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);

        assertRegla("error.dominio.anteproyecto.fecha_invalida", () -> trabajo.asignarEvaluadores(evaluador1,
                evaluador2, "J001", radicacion.minusDays(1), calendario));
    }

    /** Usuarios 1 (director), 2 (codirector), 3 (asesor) y 10 (estudiante). */
    @ParameterizedTest
    @ValueSource(longs = { 1L, 2L, 3L, 10L })
    void unEvaluadorNoPuedeTenerOtroPapelEnElTrabajo(long usuarioId) {
        Asesor asesor = new Asesor(3L, "A3", new NombrePersona("Asesor", null, "Interno", null),
                "a3@unicauca.edu.co", null, false);
        TrabajoInvestigacion conTodos = enAnteproyecto(new TrabajoInvestigacion(null, "Sistema de gestión",
                OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L), directorPlanta(2L), asesor), calendario);
        conTodos.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);

        assertRegla("error.dominio.anteproyecto.evaluador_con_conflicto", () -> conTodos
                .asignarEvaluadores(evaluador1, evaluador(usuarioId), "J001", asignacion, calendario));
        assertEquals(Proceso.ANTEPROYECTO, conTodos.getEtapa());
        assertFalse(conTodos.getAnteproyecto().tieneEvaluadores());
    }

    // ----- Conceptos de los evaluadores -----

    @Test
    void elPrimerConceptoNoCambiaLaEtapa() {
        radicarYAsignar();
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, asignacion.toLocalDate()),
                RUTA_FORMATO_B, dia(asignacion.toLocalDate()), calendario);

        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertEquals(Estado.EN_REVISION, trabajo.getAnteproyecto().getEstado());
        ConceptoEvaluacion registrado = trabajo.getAnteproyecto().getConceptos().get(0);
        assertEquals(1, registrado.version());
        assertEquals(TipoDocumento.EVALUACION_ANTEPROYECTO, registrado.formato().tipo());
        assertEquals(RUTA_FORMATO_B, registrado.formato().ruta());
        assertTrue(registrado.emitidoPor(20L));
    }

    @Test
    void siAmbosApruebanQuedaEnElDepartamentoHastaQueElJefeLoRemitaConElFormatoC() {
        radicarYAsignar();
        LocalDate fecha = asignacion.toLocalDate().plusDays(2);
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);
        trabajo.registrarConceptoEvaluador(concepto(evaluador2, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                "1/tg/anotaciones.pdf", dia(fecha), calendario);

        // Aprobado por los evaluadores: sigue en evaluación, pendiente de remisión
        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertTrue(trabajo.getAnteproyecto().estaAprobado());
        assertFalse(trabajo.getAnteproyecto().remitidoAlConsejo());
        assertEquals(TipoDocumento.EVALUACION_ANTEPROYECTO, trabajo.getAnteproyecto().getConceptos().get(0).formato().tipo());
        assertNull(trabajo.getAnteproyecto().getConceptos().get(0).anotaciones());
        assertEquals(TipoDocumento.ANOTACIONES_EVALUACION,
                trabajo.getAnteproyecto().getConceptos().get(1).anotaciones().tipo());

        trabajo.remitirAnteproyectoAlConsejo(RUTA_FORMATO_C, "J001", dia(fecha.plusDays(1)));

        assertEquals(Proceso.APROBACION_CONSEJO, trabajo.getEtapa());
        assertTrue(trabajo.getAnteproyecto().remitidoAlConsejo());
        Documento remision = trabajo.getAnteproyecto().getRemisiones().get(0);
        assertEquals(TipoDocumento.REMISION_CONSEJO, remision.tipo());
        assertEquals(1, remision.version());
        CambioEtapa cambio = trabajo.getHistorial().get(trabajo.getHistorial().size() - 1);
        assertEquals(Proceso.EVALUACION, cambio.origen());
        assertEquals(Proceso.APROBACION_CONSEJO, cambio.destino());
        assertEquals("J001", cambio.responsable());
    }

    @Test
    void elConceptoExigeElFormatoB() {
        radicarYAsignar();
        LocalDate fecha = asignacion.toLocalDate();
        assertRegla("error.dominio.anteproyecto.formato_b_obligatorio", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), " ", dia(fecha), calendario));
        assertTrue(trabajo.getAnteproyecto().getConceptos().isEmpty());
    }

    @Test
    void soloSeRemiteUnaVersionAprobadaYConFormatoC() {
        radicarYAsignar();
        LocalDate fecha = asignacion.toLocalDate();
        // Sin la aprobación de ambos evaluadores no se remite
        assertRegla("error.dominio.anteproyecto.remision_no_permitida",
                () -> trabajo.remitirAnteproyectoAlConsejo(RUTA_FORMATO_C, "J001", dia(fecha)));
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);
        trabajo.registrarConceptoEvaluador(concepto(evaluador2, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);
        // El formato C es obligatorio
        assertRegla("error.dominio.anteproyecto.formato_c_obligatorio",
                () -> trabajo.remitirAnteproyectoAlConsejo(null, "J001", dia(fecha)));
        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertTrue(trabajo.getAnteproyecto().getRemisiones().isEmpty());

        trabajo.remitirAnteproyectoAlConsejo(RUTA_FORMATO_C, "J001", dia(fecha));
        // Ya remitida: una segunda remisión no procede (la etapa ya es la del Consejo)
        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.remitirAnteproyectoAlConsejo(RUTA_FORMATO_C, "J001", dia(fecha)));
        assertEquals(1, trabajo.getAnteproyecto().getRemisiones().size());
    }

    @Test
    void conCorreccionesVuelveAAnteproyectoYLaNuevaVersionLaRevisanLosMismosEvaluadores() {
        radicarYAsignar();
        // Jueves 12-mar-2026: un evaluador aprueba y el otro pide correcciones
        LocalDate fecha = LocalDate.of(2026, 3, 12);
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);
        trabajo.registrarConceptoEvaluador(concepto(evaluador2, ResultadoConcepto.CON_CORRECCIONES, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);

        Anteproyecto anteproyecto = trabajo.getAnteproyecto();
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals(Estado.CON_CORRECCIONES, anteproyecto.getEstado());
        // 10 días hábiles para el director desde el concepto
        assertEquals(LocalDate.of(2026, 3, 26), anteproyecto.getFechaLimiteCorrecciones());

        // Lunes 23-mar-2026: el director radica la versión corregida
        LocalDate reenvio = LocalDate.of(2026, 3, 23);
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", dia(reenvio), calendario);

        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertEquals(Estado.EN_REVISION, anteproyecto.getEstado());
        assertEquals(2, anteproyecto.versionActual().get().version());
        assertTrue(anteproyecto.conceptosVersionActual().isEmpty());
        assertNull(anteproyecto.getFechaLimiteCorrecciones());
        assertEquals(LocalDate.of(2026, 4, 6), anteproyecto.getFechaLimiteEvaluacion());

        // Ambos evaluadores deben emitir de nuevo su concepto sobre la versión 2
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, reenvio), RUTA_FORMATO_B,
                dia(reenvio), calendario);
        trabajo.registrarConceptoEvaluador(concepto(evaluador2, ResultadoConcepto.APROBADO, reenvio), RUTA_FORMATO_B,
                dia(reenvio), calendario);
        trabajo.remitirAnteproyectoAlConsejo(RUTA_FORMATO_C, "J001", dia(reenvio));

        assertEquals(Proceso.APROBACION_CONSEJO, trabajo.getEtapa());
        assertEquals(4, anteproyecto.getConceptos().size());
        // Cada versión conserva el resultado de su revisión
        assertEquals(List.of(Estado.CON_CORRECCIONES, Estado.APROBADO),
                anteproyecto.getVersiones().stream().map(VersionDocumento::estado).toList());
        assertEquals(List.of(Proceso.ANTEPROYECTO, Proceso.EVALUACION, Proceso.ANTEPROYECTO, Proceso.EVALUACION,
                Proceso.APROBACION_CONSEJO),
                trabajo.getHistorial().stream().map(CambioEtapa::destino).toList());
    }

    @Test
    void soloLosEvaluadoresAsignadosEmitenConceptoUnaVezPorVersion() {
        radicarYAsignar();
        LocalDate fecha = asignacion.toLocalDate();

        assertRegla("error.dominio.anteproyecto.evaluador_no_asignado", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador(30L), ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B, dia(fecha), calendario));

        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);
        assertRegla("error.dominio.anteproyecto.concepto_repetido", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador1, ResultadoConcepto.CON_CORRECCIONES, fecha), RUTA_FORMATO_B, dia(fecha), calendario));
        assertEquals(1, trabajo.getAnteproyecto().getConceptos().size());
    }

    /** El anteproyecto no se rechaza (Art. 13): solo se aprueba o se devuelve con correcciones. */
    @ParameterizedTest
    @EnumSource(value = ResultadoConcepto.class, names = { "APROBADO", "CON_CORRECCIONES" },
            mode = EnumSource.Mode.EXCLUDE)
    void elEvaluadorSoloApruebaOPideCorrecciones(ResultadoConcepto resultado) {
        radicarYAsignar();
        LocalDate fecha = asignacion.toLocalDate();

        assertRegla("error.dominio.anteproyecto.resultado_invalido", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador1, resultado, fecha), RUTA_FORMATO_B, dia(fecha), calendario));
        assertTrue(trabajo.getAnteproyecto().getConceptos().isEmpty());
    }

    @Test
    void elConceptoNoPuedeSerAnteriorALaRadicacion() {
        radicarYAsignar();
        LocalDate antes = radicacion.toLocalDate().minusDays(1);

        assertRegla("error.dominio.anteproyecto.fecha_invalida", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador1, ResultadoConcepto.APROBADO, antes), RUTA_FORMATO_B, dia(antes), calendario));
    }

    @Test
    void unFormatoConRutaInvalidaNoModificaElAnteproyecto() {
        radicarYAsignar();
        LocalDate fecha = asignacion.toLocalDate();
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);

        assertRegla("error.dominio.documento.ruta_invalida", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador2, ResultadoConcepto.APROBADO, fecha), "../fuera.pdf", dia(fecha), calendario));
        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertEquals(1, trabajo.getAnteproyecto().getConceptos().size());
    }

    @Test
    void noSeEmiteConceptoFueraDeLaEtapaDeEvaluacion() {
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", radicacion, calendario);
        LocalDate fecha = asignacion.toLocalDate();

        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> trabajo.registrarConceptoEvaluador(
                concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B, dia(fecha), calendario));
    }

    // ----- Plazos informativos -----

    @Test
    void losPlazosVencidosSonInformativos() {
        radicarYAsignar();
        LocalDate limite = trabajo.getAnteproyecto().getFechaLimiteEvaluacion();

        assertFalse(trabajo.getAnteproyecto().evaluacionVencida(limite));
        assertTrue(trabajo.getAnteproyecto().evaluacionVencida(limite.plusDays(1)));
        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());

        // Un evaluador concede y el otro pide correcciones fuera de plazo
        LocalDate tarde = limite.plusDays(1);
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, tarde), RUTA_FORMATO_B,
                dia(tarde), calendario);
        trabajo.registrarConceptoEvaluador(concepto(evaluador2, ResultadoConcepto.CON_CORRECCIONES, tarde), RUTA_FORMATO_B,
                dia(tarde), calendario);
        LocalDate limiteCorrecciones = trabajo.getAnteproyecto().getFechaLimiteCorrecciones();
        assertFalse(trabajo.getAnteproyecto().correccionesVencidas(limiteCorrecciones));
        assertTrue(trabajo.getAnteproyecto().correccionesVencidas(limiteCorrecciones.plusDays(1)));
        assertFalse(trabajo.getAnteproyecto().evaluacionVencida(limiteCorrecciones.plusDays(1)));
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
    }

    // ----- Reconstrucción -----

    @Test
    void elAnteproyectoReconstruidoSigueOperando() {
        Documento version = Documento.inicial(TipoDocumento.ANTEPROYECTO, RUTA_ANTEPROYECTO, radicacion.toLocalDate());
        LocalDate fecha = asignacion.toLocalDate();
        DatosAnteproyecto anteproyecto = new DatosAnteproyecto(
                List.of(new VersionDocumento(version, Estado.EN_REVISION)), List.of(evaluador1, evaluador2),
                List.of(new ConceptoEvaluacion(1, concepto(evaluador1, ResultadoConcepto.APROBADO, fecha), null)),
                List.of(), LocalDate.of(2026, 3, 24), null);
        TrabajoGrado reconstruido = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L,
                Modalidad.TRABAJO_DE_INVESTIGACION, "Título", OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L),
                null, null, null, Proceso.EVALUACION, null, anteproyecto, null, List.of(), null));

        assertTrue(reconstruido.esEvaluador(21L));
        reconstruido.registrarConceptoEvaluador(concepto(evaluador2, ResultadoConcepto.APROBADO, fecha), RUTA_FORMATO_B,
                dia(fecha), calendario);
        assertEquals(Proceso.EVALUACION, reconstruido.getEtapa());
        assertEquals(Estado.APROBADO, reconstruido.getAnteproyecto().getVersiones().get(0).estado());
    }

    @Test
    void laFechaLimiteDeCadaConceptoSeCuentaDesdeLaAsignacionODesdeLaNuevaVersion() {
        // Versión 1: radicada el lunes 9 y asignada el martes 10 → 10 días hábiles desde la asignación: martes 24
        radicarYAsignar();
        LocalDate dia24 = LocalDate.of(2026, 3, 24);
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, dia24), RUTA_FORMATO_B,
                dia(dia24), calendario);
        trabajo.registrarConceptoEvaluador(concepto(evaluador2, ResultadoConcepto.CON_CORRECCIONES, dia24),
                RUTA_FORMATO_B, dia(dia24), calendario);
        // Versión 2: radicada el miércoles 1 de abril, con los evaluadores ya asignados → miércoles 15
        LocalDate dia1Abril = LocalDate.of(2026, 4, 1);
        trabajo.radicarAnteproyecto(RUTA_ANTEPROYECTO, "D1", dia(dia1Abril), calendario);
        trabajo.registrarConceptoEvaluador(concepto(evaluador1, ResultadoConcepto.APROBADO, dia1Abril.plusDays(1)),
                RUTA_FORMATO_B, dia(dia1Abril.plusDays(1)), calendario);

        List<ConceptoEvaluacion> conceptos = trabajo.getAnteproyecto().getConceptos();
        assertEquals(java.util.Optional.of(dia24), trabajo.fechaLimiteConcepto(conceptos.get(0), calendario));
        assertEquals(java.util.Optional.of(LocalDate.of(2026, 4, 15)),
                trabajo.fechaLimiteConcepto(conceptos.get(2), calendario));
    }
}
