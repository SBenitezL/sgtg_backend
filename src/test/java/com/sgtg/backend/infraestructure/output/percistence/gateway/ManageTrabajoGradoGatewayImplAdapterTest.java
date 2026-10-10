package com.sgtg.backend.infraestructure.output.percistence.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.Calificacion;
import com.sgtg.backend.domain.models.trabajo_grado.CambioObjetivos;
import com.sgtg.backend.domain.models.trabajo_grado.ConceptoEvaluacion;
import com.sgtg.backend.domain.models.trabajo_grado.InformeMensual;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Prorroga;
import com.sgtg.backend.domain.models.trabajo_grado.EstadoSustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.MotivoCierre;
import com.sgtg.backend.domain.models.trabajo_grado.Suspension;
import com.sgtg.backend.domain.models.trabajo_grado.Sustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoInvestigacion;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Guarda y recupera el agregado completo en H2 con el gateway y el mapper reales. */
@DataJpaTest
@Import(ManageTrabajoGradoGatewayImplAdapter.class)
class ManageTrabajoGradoGatewayImplAdapterTest {

    private static final Departamento SISTEMAS = new Departamento(1L, "Sistemas");
    private static final Programa ING_SISTEMAS = new Programa(1L, "Ingeniería de Sistemas");
    private static final LocalDate LUNES = LocalDate.of(2026, 3, 2);

    @Autowired
    private ManageTrabajoGradoGatewayImplAdapter gateway;

    private static Estudiante estudiante(long id) {
        return new Estudiante(id, "E" + id, new NombrePersona("Estudiante", null, "Prueba", null), "e" + id + "@unicauca.edu.co", ING_SISTEMAS);
    }

    private static Director director(long id, TipoVinculacion vinculacion) {
        return new Director(id, "D" + id, new NombrePersona("Docente", null, "Prueba", null), "d" + id + "@unicauca.edu.co", vinculacion, SISTEMAS);
    }

    @Test
    void guardaYRecuperaUnTrabajoConSuPropuestaEHistorial() {
        TrabajoGrado nuevo = new TrabajoInvestigacion(null, "Sistema de gestión", List.of("Objetivo 1", "Objetivo 2"),
                List.of(estudiante(10L), estudiante(11L)), director(1L, TipoVinculacion.CATEDRA),
                director(2L, TipoVinculacion.PLANTA), null);
        TrabajoGrado guardado = gateway.guardar(nuevo);
        assertNotNull(guardado.getId());

        // Radicar con correcciones, nueva versión y aprobación
        guardado.radicarPropuesta("1/v1.pdf", LUNES, new CalendarioHabilFalso());
        guardado.registrarDecisionComite(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.NO_APROBADO,
                "Acta 1", "actas/2026/acta-01.pdf", "Ajustar alcance", LUNES.plusDays(2)), "C001",
                LUNES.plusDays(2).atTime(9, 0));
        guardado = gateway.guardar(guardado);
        guardado.radicarPropuesta("1/v2.pdf", LUNES.plusDays(7),
                new CalendarioHabilFalso());
        guardado.registrarDecisionComite(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, "Acta 2",
                null, null, LUNES.plusDays(9)), "C001", LUNES.plusDays(9).atTime(9, 0));
        gateway.guardar(guardado);

        TrabajoGrado leido = gateway.buscarPorId(guardado.getId());

        assertInstanceOf(TrabajoInvestigacion.class, leido);
        assertEquals(Proceso.ANTEPROYECTO, leido.getEtapa());
        assertEquals(List.of("Objetivo 1", "Objetivo 2"), leido.getObjetivos());
        assertEquals(List.of(10L, 11L), leido.getEstudiantes().stream().map(Estudiante::getUsuarioId).toList());
        assertEquals(ING_SISTEMAS, leido.getEstudiantes().get(0).getPrograma());
        assertEquals(TipoVinculacion.CATEDRA, leido.getDirector().getTipoVinculacion());
        assertEquals(2L, leido.getCodirector().getUsuarioId());
        assertNull(leido.getAsesor());
        assertEquals(Estado.APROBADO, leido.getPropuesta().getEstado());
        assertEquals(List.of(1, 2), leido.getPropuesta().getVersiones().stream().map(Documento::version).toList());
        assertEquals(2, leido.getPropuesta().getDecisiones().size());
        assertEquals(ResultadoConcepto.NO_APROBADO, leido.getPropuesta().getDecisiones().get(0).resultado());
        assertEquals("actas/2026/acta-01.pdf", leido.getPropuesta().getDecisiones().get(0).rutaArchivo());
        assertNull(leido.getPropuesta().getDecisiones().get(1).rutaArchivo());
        assertEquals("Estudiante Prueba", leido.getEstudiantes().get(0).getNombreCompleto());
        assertEquals(1, leido.getHistorial().size());
        assertEquals(Proceso.ANTEPROYECTO, leido.getHistorial().get(0).destino());
    }

    private static Evaluador evaluador(long id) {
        return new Evaluador(id, "V" + id, new NombrePersona("Evaluador", null, "Prueba", null),
                "v" + id + "@unicauca.edu.co");
    }

    /** Trabajo guardado con la propuesta aprobada, en la etapa de anteproyecto. */
    private TrabajoGrado guardarEnAnteproyecto(String titulo, long directorId, long estudianteId) {
        TrabajoGrado trabajo = gateway.guardar(new TrabajoInvestigacion(null, titulo, List.of("O"),
                List.of(estudiante(estudianteId)), director(directorId, TipoVinculacion.PLANTA), null, null));
        trabajo.radicarPropuesta("1/p.pdf", LUNES, new CalendarioHabilFalso());
        trabajo.registrarDecisionComite(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, "Acta 1",
                null, null, LUNES), "C001", LUNES.atTime(9, 0));
        return gateway.guardar(trabajo);
    }

    @Test
    void guardaYRecuperaElAnteproyectoConEvaluadoresYConceptos() {
        CalendarioHabilFalso calendario = new CalendarioHabilFalso();
        TrabajoGrado trabajo = guardarEnAnteproyecto("Con anteproyecto", 1L, 10L);
        trabajo.radicarAnteproyecto("1/a1.pdf", "D1", LUNES.atTime(10, 0), calendario);
        trabajo.asignarEvaluadores(evaluador(20L), evaluador(21L), "J60", LUNES.atTime(11, 0), calendario);
        trabajo = gateway.guardar(trabajo);
        // Un evaluador aprueba con formato B y el otro pide correcciones: vuelve a anteproyecto
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(20L), ResultadoConcepto.APROBADO, null,
                LUNES.plusDays(1)), "1/b.pdf", LUNES.plusDays(1).atTime(9, 0),
                calendario);
        trabajo = gateway.guardar(trabajo);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(21L), ResultadoConcepto.CON_CORRECCIONES,
                "Ajustar cronograma", LUNES.plusDays(2)), null, LUNES.plusDays(2).atTime(9, 0), calendario);
        gateway.guardar(trabajo);

        TrabajoGrado leido = gateway.buscarPorId(trabajo.getId());

        assertEquals(Proceso.ANTEPROYECTO, leido.getEtapa());
        assertEquals(Estado.CON_CORRECCIONES, leido.getAnteproyecto().getEstado());
        assertEquals(LocalDate.of(2026, 3, 18), leido.getAnteproyecto().getFechaLimiteCorrecciones());
        assertEquals(LocalDate.of(2026, 3, 16), leido.getAnteproyecto().getFechaLimiteEvaluacion());
        assertEquals(TipoDocumento.ANTEPROYECTO, leido.getAnteproyecto().versionActual().get().tipo());
        assertEquals(List.of(20L, 21L),
                leido.getAnteproyecto().getEvaluadores().stream().map(Evaluador::getUsuarioId).toList());
        ConceptoEvaluacion aprobado = leido.getAnteproyecto().getConceptos().get(0);
        assertEquals("V20", aprobado.concepto().emisor().getCodigo());
        assertEquals(new Documento(TipoDocumento.EVALUACION_ANTEPROYECTO, "1/b.pdf",
                1, LUNES.plusDays(1)), aprobado.formato());
        ConceptoEvaluacion conCorrecciones = leido.getAnteproyecto().getConceptos().get(1);
        assertEquals(ResultadoConcepto.CON_CORRECCIONES, conCorrecciones.concepto().resultado());
        assertEquals("Ajustar cronograma", conCorrecciones.concepto().observaciones());
        assertNull(conCorrecciones.formato());
        assertEquals(List.of(Proceso.ANTEPROYECTO, Proceso.EVALUACION, Proceso.ANTEPROYECTO),
                leido.getHistorial().stream().map(c -> c.destino()).toList());

        // El trabajo leído sigue operando: la versión corregida vuelve a evaluación
        leido.radicarAnteproyecto("1/a2.pdf", "D1", LUNES.plusDays(3).atTime(9, 0),
                calendario);
        assertEquals(Proceso.EVALUACION, gateway.guardar(leido).getEtapa());
    }

    @Test
    void guardaLasDecisionesDelConsejoYLaEjecucion() {
        CalendarioHabilFalso calendario = new CalendarioHabilFalso();
        TrabajoGrado trabajo = guardarEnAnteproyecto("En curso", 1L, 10L);
        trabajo.radicarAnteproyecto("1/a1.pdf", "D1", LUNES.atTime(10, 0), calendario);
        trabajo.asignarEvaluadores(evaluador(20L), evaluador(21L), "J60", LUNES.atTime(11, 0), calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(20L), ResultadoConcepto.APROBADO, null, LUNES),
                null, LUNES.atTime(12, 0), calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(21L), ResultadoConcepto.APROBADO, null, LUNES),
                null, LUNES.atTime(12, 0), calendario);
        trabajo.registrarDecisionConsejo(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null,
                "resoluciones/res-100.pdf", "Aprobado en sesión ordinaria", LUNES.plusDays(4)), "CF01",
                LUNES.plusDays(4).atTime(9, 0));
        gateway.guardar(trabajo);

        TrabajoGrado leido = gateway.buscarPorId(trabajo.getId());

        assertEquals(Proceso.EN_CURSO, leido.getEtapa());
        Decision decision = leido.getAnteproyecto().getDecisionesConsejo().get(0);
        assertEquals(Organo.CONSEJO_FACULTAD, decision.organo());
        assertEquals("resoluciones/res-100.pdf", decision.rutaArchivo());
        assertEquals("Aprobado en sesión ordinaria", decision.observaciones());
        assertEquals(LocalDate.of(2026, 3, 6), leido.getEjecucion().getFechaNotificacion());
        assertEquals(LocalDate.of(2026, 12, 6), leido.getEjecucion().getFechaFin());
        assertEquals(LocalDate.of(2026, 6, 6), leido.getEjecucion().getFechaMinimaSustentacion());
        assertNull(gateway.buscarPorId(guardarEnAnteproyecto("Sin ejecución", 2L, 11L).getId()).getEjecucion());
    }

    @Test
    void guardaLosCambiosDeObjetivosYLosInformesMensuales() {
        CalendarioHabilFalso calendario = new CalendarioHabilFalso();
        Asesor externo = new Asesor(null, "1061000000", new NombrePersona("Asesor", null, "Externo", null),
                "a@empresa.com", "Empresa S.A.S.", true);
        Documento carta = Documento.inicial(TipoDocumento.CARTA_ACEPTACION, "1/c.pdf",
                LUNES);
        TrabajoGrado trabajo = gateway.guardar(new PracticaProfesional(null, "Práctica", List.of("Objetivo"),
                estudiante(10L), director(1L, TipoVinculacion.PLANTA), null, externo, carta));
        trabajo.radicarPropuesta("1/p.pdf", LUNES, calendario);
        trabajo.registrarDecisionComite(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, null, null,
                null, LUNES), "C001", LUNES.atTime(9, 0));
        trabajo.radicarAnteproyecto("1/a.pdf", "D1", LUNES.atTime(10, 0), calendario);
        trabajo.asignarEvaluadores(evaluador(20L), evaluador(21L), "J60", LUNES.atTime(11, 0), calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(20L), ResultadoConcepto.APROBADO, null, LUNES),
                null, LUNES.atTime(12, 0), calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(21L), ResultadoConcepto.APROBADO, null, LUNES),
                null, LUNES.atTime(12, 0), calendario);
        trabajo.registrarDecisionConsejo(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null,
                "resoluciones/res-100.pdf", null, LUNES), "CF01", LUNES.atTime(13, 0));
        // Un cambio no aprobado y otro pendiente; dos informes
        trabajo.solicitarCambioObjetivos("Práctica ampliada", List.of("Objetivo 1", "Objetivo 2"), "Ampliación",
                LUNES.plusDays(10));
        trabajo.resolverCambioObjetivos(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.NO_APROBADO, null,
                "resoluciones/res-150.pdf", null, LUNES.plusDays(20)));
        trabajo.solicitarCambioObjetivos("Práctica ajustada", List.of("Objetivo ajustado"), "Ajuste",
                LUNES.plusDays(30));
        trabajo.registrarInformeMensual("informes/marzo.pdf", LUNES.plusDays(28));
        trabajo.registrarInformeMensual("informes/abril.pdf", LUNES.plusDays(58));
        // Prórroga negada: la práctica sigue en curso con el plazo original
        trabajo.solicitarProrroga(3, "solicitudes/prorroga.pdf", LUNES.plusDays(60));
        trabajo.resolverProrroga(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.NO_APROBADO, null,
                "resoluciones/res-300.pdf", null, LUNES.plusDays(70)), "CF01", LUNES.plusDays(70).atTime(9, 0));
        // Una suspensión negada y otra aprobada y reanudada
        trabajo.solicitarSuspension("solicitudes/suspension-1.pdf", LUNES.plusDays(75));
        trabajo.resolverSuspension(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.NO_APROBADO, null,
                "resoluciones/res-399.pdf", null, LUNES.plusDays(78)), "CF01", LUNES.plusDays(78).atTime(9, 0));
        trabajo.solicitarSuspension("solicitudes/suspension-2.pdf", LUNES.plusDays(80));
        trabajo.resolverSuspension(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null,
                "resoluciones/res-400.pdf", "Calamidad doméstica", LUNES.plusDays(90)), "CF01",
                LUNES.plusDays(90).atTime(9, 0));
        trabajo.reanudar("solicitudes/reanudacion.pdf", "D1", LUNES.plusDays(150).atTime(9, 0));
        List<Suspension> suspensiones = trabajo.getEjecucion().getSuspensiones();
        LocalDate fechaFin = trabajo.getEjecucion().getFechaFin();
        gateway.guardar(trabajo);

        TrabajoGrado leido = gateway.buscarPorId(trabajo.getId());

        assertEquals(Proceso.EN_CURSO, leido.getEtapa());
        // Plazo de 12 meses (2-mar-2027) más los 60 días que estuvo suspendida (31-may a 30-jul-2026)
        assertEquals(LocalDate.of(2027, 5, 1), leido.getEjecucion().getFechaFin());
        CambioObjetivos noAprobado = leido.getEjecucion().getCambiosObjetivos().get(0);
        assertEquals(List.of("Objetivo"), noAprobado.objetivosAnteriores());
        assertEquals(List.of("Objetivo 1", "Objetivo 2"), noAprobado.objetivosPropuestos());
        assertEquals(ResultadoConcepto.NO_APROBADO, noAprobado.decision().resultado());
        assertEquals("resoluciones/res-150.pdf", noAprobado.decision().rutaArchivo());
        assertTrue(leido.getEjecucion().cambioPendiente().isPresent());
        assertNull(leido.getEjecucion().getCambiosObjetivos().get(1).decision());
        assertEquals(List.of("informes/marzo.pdf", "informes/abril.pdf"), leido.getEjecucion()
                .getInformesMensuales().stream().map(InformeMensual::rutaArchivo).toList());
        assertEquals(2, leido.getEjecucion().getInformesMensuales().get(1).numero());
        Prorroga prorroga = leido.getEjecucion().getProrroga();
        assertEquals(3, prorroga.meses());
        assertEquals("solicitudes/prorroga.pdf", prorroga.rutaSolicitud());
        assertEquals(ResultadoConcepto.NO_APROBADO, prorroga.decision().resultado());
        assertEquals("resoluciones/res-300.pdf", prorroga.decision().rutaArchivo());
        assertEquals(suspensiones, leido.getEjecucion().getSuspensiones());
        assertEquals(fechaFin, leido.getEjecucion().getFechaFin());
        assertEquals(Proceso.EN_CURSO, leido.getEtapa());
        assertTrue(leido.getEjecucion().suspensionActiva().isEmpty());
    }

    @Test
    void guardaLaSustentacionConSusSesionesCalificacionesEInasistencias() {
        CalendarioHabilFalso calendario = new CalendarioHabilFalso();
        TrabajoGrado trabajo = guardarEnAnteproyecto("Con sustentación", 1L, 10L);
        trabajo.radicarAnteproyecto("1/a1.pdf", "D1", LUNES.atTime(10, 0), calendario);
        trabajo.asignarEvaluadores(evaluador(20L), evaluador(21L), "J60", LUNES.atTime(11, 0), calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(20L), ResultadoConcepto.APROBADO, null, LUNES),
                null, LUNES.atTime(12, 0), calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(21L), ResultadoConcepto.APROBADO, null, LUNES),
                null, LUNES.atTime(12, 0), calendario);
        trabajo.registrarDecisionConsejo(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null,
                "resoluciones/res-100.pdf", null, LUNES), "CF01", LUNES.atTime(13, 0));
        // Sustentación: primera sesión aplazada; en la segunda, inasistencia
        LocalDate julio = LocalDate.of(2026, 7, 1);
        trabajo.solicitarSustentacion("finales/tg.pdf", "paz-y-salvo/tg.pdf", null, "D1", julio.atTime(9, 0));
        trabajo.programarSustentacion(evaluador(20L), evaluador(40L), julio.plusDays(9).atTime(14, 0), "Salón 301",
                "CF01", julio.plusDays(1).atTime(9, 0));
        trabajo.calificarSustentacion(evaluador(20L), Calificacion.APROBADO_CON_OBSERVACIONES, "Ajustar conclusiones",
                "actas/sesion-1.pdf", julio.plusDays(9).atTime(16, 0), calendario);
        trabajo.calificarSustentacion(evaluador(40L), Calificacion.APLAZADO, "Repetir la presentación", null,
                julio.plusDays(9).atTime(16, 0), calendario);
        trabajo.programarSustentacion(evaluador(40L), evaluador(41L), julio.plusDays(30).atTime(14, 0), "Salón 205",
                "CF01", julio.plusDays(15).atTime(9, 0));
        trabajo.registrarInasistencia(evaluador(41L), "El estudiante no se presentó",
                julio.plusDays(30).atTime(15, 0));
        gateway.guardar(trabajo);

        TrabajoGrado leido = gateway.buscarPorId(trabajo.getId());

        assertEquals(Proceso.NO_APROBADO, leido.getEtapa());
        assertEquals(MotivoCierre.INASISTENCIA_SUSTENTACION, leido.getMotivoCierre());
        Sustentacion sustentacion = leido.getSustentacion();
        assertEquals(EstadoSustentacion.NO_APROBADA, sustentacion.getEstado());
        assertEquals("paz-y-salvo/tg.pdf", sustentacion.getRutaPazYSalvo());
        assertEquals(2, sustentacion.getSesiones().size());
        assertEquals("Salón 205", sustentacion.sesionActual().salon());
        assertEquals(List.of(40L, 41L), sustentacion.sesionActual().jurados().stream().map(Evaluador::getUsuarioId)
                .toList());
        assertEquals(List.of(Calificacion.APROBADO_CON_OBSERVACIONES, Calificacion.APLAZADO),
                sustentacion.getCalificaciones().stream().map(c -> c.calificacion()).toList());
        assertEquals("actas/sesion-1.pdf", sustentacion.getCalificaciones().get(0).rutaActa());
        assertEquals(41L, sustentacion.getInasistencias().get(0).jurado().getUsuarioId());
        assertEquals(julio.plusDays(9).plusDays(60), sustentacion.getFechaLimiteSegundaSesion());
        assertEquals(MotivoCierre.INASISTENCIA_SUSTENTACION, sustentacion.getMotivoNoAprobacion());
        // Los jurados de ambas sesiones ven el trabajo entre los suyos
        assertEquals(List.of(trabajo.getId()),
                gateway.listarPorParticipante(41L).stream().map(TrabajoGrado::getId).toList());
    }

    @Test
    void guardaLasSolicitudesDeCancelacion() {
        TrabajoGrado trabajo = guardarEnAnteproyecto("Con cancelación", 1L, 10L);
        trabajo.solicitarCancelacion("solicitudes/cancelacion-1.pdf", LUNES);
        trabajo.resolverCancelacion(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.NO_APROBADO, null,
                "resoluciones/res-499.pdf", null, LUNES.plusDays(2)), "CF01", LUNES.plusDays(2).atTime(9, 0));
        trabajo.solicitarCancelacion("solicitudes/cancelacion-2.pdf", LUNES.plusDays(4));
        gateway.guardar(trabajo);

        TrabajoGrado leido = gateway.buscarPorId(trabajo.getId());

        assertEquals(trabajo.getCancelaciones(), leido.getCancelaciones());
        assertEquals(Proceso.ANTEPROYECTO, leido.getCancelaciones().get(0).etapaAlSolicitar());
        assertTrue(leido.cancelacionPendiente().isPresent());

        leido.resolverCancelacion(new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null,
                "resoluciones/res-500.pdf", null, LUNES.plusDays(6)), "CF01", LUNES.plusDays(6).atTime(9, 0));
        TrabajoGrado cancelado = gateway.guardar(leido);
        assertEquals(Proceso.CANCELADO, cancelado.getEtapa());
        assertEquals(MotivoCierre.CANCELACION_DIRECTOR, cancelado.getMotivoCierre());
    }

    @Test
    void listaPorEtapaEIncluyeALosEvaluadoresComoParticipantes() {
        CalendarioHabilFalso calendario = new CalendarioHabilFalso();
        TrabajoGrado enPropuesta = gateway.guardar(new TrabajoInvestigacion(null, "En propuesta", List.of("O"),
                List.of(estudiante(10L)), director(1L, TipoVinculacion.PLANTA), null, null));
        TrabajoGrado enAnteproyecto = guardarEnAnteproyecto("En anteproyecto", 2L, 11L);
        TrabajoGrado enEvaluacion = guardarEnAnteproyecto("En evaluación", 3L, 12L);
        enEvaluacion.radicarAnteproyecto("1/a.pdf", "D3", LUNES.atTime(10, 0),
                calendario);
        enEvaluacion.asignarEvaluadores(evaluador(20L), evaluador(21L), "J60", LUNES.atTime(11, 0), calendario);
        gateway.guardar(enEvaluacion);

        assertEquals(List.of(enAnteproyecto.getId()),
                gateway.listarPorEtapa(Proceso.ANTEPROYECTO).stream().map(TrabajoGrado::getId).toList());
        assertEquals(List.of(enPropuesta.getId()),
                gateway.listarPorEtapa(Proceso.PROPUESTA).stream().map(TrabajoGrado::getId).toList());
        assertEquals(List.of(enEvaluacion.getId()),
                gateway.listarPorParticipante(21L).stream().map(TrabajoGrado::getId).toList());
    }

    @Test
    void guardaLaPracticaProfesionalConCartaYAsesorExterno() {
        Asesor externo = new Asesor(null, "1061000000", new NombrePersona("Asesor", null, "Externo", null), "a@empresa.com", "Empresa S.A.S.", true);
        Documento carta = Documento.inicial(TipoDocumento.CARTA_ACEPTACION, "1/c.pdf",
                LUNES);
        TrabajoGrado guardado = gateway.guardar(new PracticaProfesional(null, "Práctica", List.of("Objetivo"),
                estudiante(10L), director(1L, TipoVinculacion.PLANTA), null, externo, carta));

        PracticaProfesional leida = assertInstanceOf(PracticaProfesional.class,
                gateway.buscarPorId(guardado.getId()));
        assertEquals(carta, leida.getCartaAceptacion());
        assertNull(leida.getAsesor().getUsuarioId());
        assertEquals("Empresa S.A.S.", leida.getAsesor().getOrganizacion());
        assertTrue(leida.getAsesor().isExterno());
        assertNull(leida.getCodirector());
        assertNull(leida.getPropuesta().getEstado());
    }

    @Test
    void listaLosTrabajosDondeParticipaElUsuario() {
        TrabajoGrado t1 = gateway.guardar(new TrabajoInvestigacion(null, "T1", List.of("O"), List.of(estudiante(10L)),
                director(1L, TipoVinculacion.PLANTA), null, null));
        TrabajoGrado t2 = gateway.guardar(new TrabajoInvestigacion(null, "T2", List.of("O"),
                List.of(estudiante(20L), estudiante(21L)), director(2L, TipoVinculacion.PLANTA), null, null));

        assertEquals(List.of(t1.getId()), gateway.listarPorParticipante(10L).stream().map(TrabajoGrado::getId).toList());
        assertEquals(List.of(t2.getId()), gateway.listarPorParticipante(21L).stream().map(TrabajoGrado::getId).toList());
        assertEquals(List.of(t2.getId()), gateway.listarPorParticipante(2L).stream().map(TrabajoGrado::getId).toList());
        assertTrue(gateway.listarPorParticipante(99L).isEmpty());
        assertNull(gateway.buscarPorId(404L));
    }
}
