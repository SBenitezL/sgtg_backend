package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.notificacion.EventoTrabajoGrado;
import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.domain.models.notificacion.TipoNotificacion;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.DatosEjecucion;
import com.sgtg.backend.domain.models.trabajo_grado.DatosTrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.InformeMensual;
import com.sgtg.backend.domain.models.trabajo_grado.ReunionSeguimiento;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoInvestigacion;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Destinatarios, textos y avisos de plazos de las notificaciones del trabajo de grado. */
class NotificadorTrabajoGradoTest {

    private static final Departamento SISTEMAS = new Departamento(1L, "Sistemas");
    private static final Departamento TELEMATICA = new Departamento(2L, "Telemática");
    private static final Programa ING_SISTEMAS = new Programa(1L, "Ingeniería de Sistemas");
    private static final LocalDate LUNES = LocalDate.of(2026, 3, 2);
    private static final Clock RELOJ = Clock.fixed(LUNES.atTime(10, 0).atZone(ZoneId.of("America/Bogota"))
            .toInstant(), ZoneId.of("America/Bogota"));

    private final CalendarioHabil calendario = new CalendarioHabilFalso();
    private ManageNotificacionGatewayIntPort notificacionGateway;
    private ManagePerfilGatewayIntPort perfilGateway;
    private ManageCatalogoGatewayIntPort catalogoGateway;
    private ManageUserGatewayIntPort userGateway;
    private NotificadorTrabajoGrado notificador;

    private static Usuario usuario(long id, boolean activo, RoleAdmited... roles) {
        return new Usuario(id, "U" + id, TipoDocumentoIdentidad.CC, "10" + id, "u" + id + "@unicauca.edu.co", "Nombre", null, "Apellido", null,
                List.of(roles).stream().map(rol -> new Role(null, rol)).toList(), activo);
    }

    private static Evaluador evaluador(long id) {
        return new Evaluador(id, "EV" + id, new NombrePersona("Evaluador", null, "Prueba", null),
                "ev" + id + "@unicauca.edu.co");
    }

    /** Trabajo nuevo: director 1 (Sistemas), codirector 2 y estudiante 10. */
    private static TrabajoInvestigacion trabajo() {
        Director director = new Director(1L, "D1", new NombrePersona("Docente", null, "Prueba", null),
                "d1@unicauca.edu.co", TipoVinculacion.PLANTA, SISTEMAS);
        Director codirector = new Director(2L, "D2", new NombrePersona("Codirector", null, "Prueba", null),
                "d2@unicauca.edu.co", TipoVinculacion.PLANTA, SISTEMAS);
        Estudiante estudiante = new Estudiante(10L, "E10", new NombrePersona("Estudiante", null, "Prueba", null),
                "e10@unicauca.edu.co", ING_SISTEMAS);
        return new TrabajoInvestigacion(null, "Sistema de gestión", List.of("Objetivo"), List.of(estudiante),
                director, codirector, null);
    }

    @SuppressWarnings("unchecked")
    private List<Notificacion> guardadas() {
        ArgumentCaptor<List<Notificacion>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificacionGateway).guardarTodas(captor.capture());
        return captor.getValue();
    }

    private static Set<Long> usuarios(List<Notificacion> notificaciones) {
        return notificaciones.stream().map(Notificacion::usuarioId).collect(Collectors.toSet());
    }

    @BeforeEach
    void preparar() {
        notificacionGateway = mock(ManageNotificacionGatewayIntPort.class);
        userGateway = mock(ManageUserGatewayIntPort.class);
        perfilGateway = mock(ManagePerfilGatewayIntPort.class);
        catalogoGateway = mock(ManageCatalogoGatewayIntPort.class);
        // Comité de Sistemas (50, y 51 inactivo), comité de Telemática (52), Consejo (70),
        // jefes de Sistemas (60) y de Telemática (61)
        when(userGateway.listar()).thenReturn(List.of(usuario(50L, true, RoleAdmited.COMITE),
                usuario(51L, false, RoleAdmited.COMITE), usuario(52L, true, RoleAdmited.COMITE),
                usuario(70L, true, RoleAdmited.CONSEJO),
                usuario(60L, true, RoleAdmited.JEFE_DEP, RoleAdmited.DOCENTE),
                usuario(61L, true, RoleAdmited.JEFE_DEP, RoleAdmited.DOCENTE)));
        when(perfilGateway.buscarPerfilDocente(50L)).thenReturn(new PerfilDocente(50L, TipoVinculacion.PLANTA, SISTEMAS));
        when(perfilGateway.buscarPerfilDocente(51L)).thenReturn(new PerfilDocente(51L, TipoVinculacion.PLANTA, SISTEMAS));
        when(perfilGateway.buscarPerfilDocente(52L))
                .thenReturn(new PerfilDocente(52L, TipoVinculacion.PLANTA, TELEMATICA));
        when(perfilGateway.buscarPerfilDocente(60L)).thenReturn(new PerfilDocente(60L, TipoVinculacion.PLANTA, SISTEMAS));
        when(perfilGateway.buscarPerfilDocente(61L))
                .thenReturn(new PerfilDocente(61L, TipoVinculacion.PLANTA, TELEMATICA));
        // Ingeniería de Sistemas está adscrito al departamento de Sistemas
        when(catalogoGateway.buscarPrograma(1L)).thenReturn(new Programa(1L, "Ingeniería de Sistemas", SISTEMAS));
        notificador = new NotificadorTrabajoGrado(notificacionGateway, userGateway, perfilGateway, catalogoGateway,
                RELOJ);
    }

    @Test
    void notificaALosDestinatariosDelHechoMenosAlActor() {
        TrabajoInvestigacion trabajo = trabajo();
        trabajo.radicarPropuesta("1/PROPUESTA/v1/p.pdf", LUNES, calendario);

        notificador.notificar(trabajo, EventoTrabajoGrado.PROPUESTA_RADICADA, 1L, null);

        List<Notificacion> notificaciones = guardadas();
        // Comité activo del programa, estudiante y codirector; no el director que radicó, el comité inactivo
        // ni el Comité de otro programa
        assertEquals(Set.of(50L, 10L, 2L), usuarios(notificaciones));
        Notificacion una = notificaciones.get(0);
        assertEquals(TipoNotificacion.INFO, una.tipo());
        assertEquals("Propuesta radicada", una.titulo());
        assertEquals("Se radicó la propuesta \"Sistema de gestión\" para el concepto del Comité de Programa.",
                una.mensaje());
        assertEquals(Proceso.PROPUESTA, una.etapa());
        assertEquals(EventoTrabajoGrado.PROPUESTA_RADICADA, una.evento());
        assertEquals(LocalDateTime.of(2026, 3, 2, 10, 0), una.fecha());
        assertEquals(false, una.leida());
    }

    @Test
    void elComiteQueSeNotificaEsElDelProgramaDeLosEstudiantes() {
        TrabajoInvestigacion trabajo = trabajo();
        trabajo.radicarPropuesta("1/PROPUESTA/v1/p.pdf", LUNES, calendario);
        // Si el programa pasa al departamento de Telemática, se notifica a su Comité
        when(catalogoGateway.buscarPrograma(1L)).thenReturn(new Programa(1L, "Ingeniería de Sistemas", TELEMATICA));

        notificador.notificar(trabajo, EventoTrabajoGrado.PROPUESTA_RADICADA, 1L, null);

        assertEquals(Set.of(52L, 10L, 2L), usuarios(guardadas()));
    }

    @Test
    void elJefeQueSeNotificaEsElDelDepartamentoDelDirector() {
        notificador.notificar(trabajo(), EventoTrabajoGrado.ANTEPROYECTO_RADICADO, 1L, null);
        assertEquals(Set.of(60L, 10L, 2L), usuarios(guardadas()));
    }

    @Test
    void laSolicitudDeSustentacionLlegaADecanatura() {
        when(userGateway.listar()).thenReturn(List.of(usuario(80L, true, RoleAdmited.DECANATURA),
                usuario(70L, true, RoleAdmited.CONSEJO)));
        notificador.notificar(trabajo(), EventoTrabajoGrado.SUSTENTACION_SOLICITADA, 1L, null);
        assertEquals(Set.of(80L, 70L, 10L, 2L), usuarios(guardadas()));
    }

    @Test
    void elDetalleCompletaElTexto() {
        notificador.notificar(trabajo(), EventoTrabajoGrado.DECISION_COMITE, 50L, "aprobado");
        assertEquals("El Comité de Programa emitió su concepto sobre la propuesta \"Sistema de gestión\": aprobado.",
                guardadas().get(0).mensaje());
    }

    @Test
    void unaFallaAlGuardarNoInterrumpeLaOperacion() {
        when(notificacionGateway.guardarTodas(anyList())).thenThrow(new IllegalStateException("sin conexión"));
        notificador.notificar(trabajo(), EventoTrabajoGrado.PROPUESTA_RADICADA, 1L, null);
        assertEquals(0, notificador.avisarPlazos(trabajo(), LUNES));
    }

    @Test
    void elCierreDelSistemaSeNotificaConSuMotivo() {
        Director director = trabajo().getDirector();
        TrabajoGrado enCurso = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, Modalidad.TRABAJO_DE_INVESTIGACION,
                "Vencido", List.of("Objetivo"), trabajo().getEstudiantes(), director, null, null, null,
                Proceso.EN_CURSO, null, null,
                new DatosEjecucion(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 10, 1), LocalDate.of(2025, 4, 1)),
                List.of(), null));
        enCurso.verificarVencimientos(LUNES.atTime(0, 5));

        notificador.notificarCambioDelSistema(enCurso);

        Notificacion cierre = guardadas().get(0);
        assertEquals("Trabajo cerrado por vencimiento", cierre.titulo());
        assertEquals(TipoNotificacion.ERROR, cierre.tipo());
        assertTrue(cierre.mensaje().startsWith("El trabajo \"Vencido\" quedó no aprobado: Plazo de ejecución vencido"));
        assertEquals(Set.of(10L, 1L, 70L), usuarios(guardadas()));
    }

    // ----- Avisos de plazos -----

    @Test
    void elComiteRecibeElAvisoTresDiasAntesYUnaSolaVez() {
        TrabajoInvestigacion trabajo = trabajo();
        trabajo.radicarPropuesta("1/PROPUESTA/v1/p.pdf", LUNES, calendario);
        LocalDate limite = trabajo.getPropuesta().getFechaLimiteConcepto();

        assertEquals(0, notificador.avisarPlazos(trabajo, limite.minusDays(4)));
        verify(notificacionGateway, never()).guardarTodas(anyList());

        assertEquals(1, notificador.avisarPlazos(trabajo, limite.minusDays(3)));
        Notificacion aviso = guardadas().get(0);
        assertEquals(50L, aviso.usuarioId());
        assertEquals("PLAZO_PROPUESTA_POR_VENCER:null:" + limite, aviso.clave());
        assertTrue(aviso.mensaje().startsWith("Faltan 3 días"));

        // Ya avisado: no se repite
        when(notificacionGateway.existeClave(eq(50L), any())).thenReturn(true);
        assertEquals(0, notificador.avisarPlazos(trabajo, limite.minusDays(2)));
    }

    @Test
    void vencidoElConceptoSeAvisaAlComite() {
        TrabajoInvestigacion trabajo = trabajo();
        trabajo.radicarPropuesta("1/PROPUESTA/v1/p.pdf", LUNES, calendario);
        LocalDate limite = trabajo.getPropuesta().getFechaLimiteConcepto();

        assertEquals(1, notificador.avisarPlazos(trabajo, limite.plusDays(1)));
        assertEquals("Plazo de concepto vencido", guardadas().get(0).titulo());
    }

    @Test
    void enEvaluacionSoloSeAvisaAlEvaluadorQueNoHaEmitidoConcepto() {
        TrabajoInvestigacion trabajo = trabajo();
        trabajo.radicarPropuesta("1/PROPUESTA/v1/p.pdf", LUNES, calendario);
        trabajo.registrarDecisionComite(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, "Acta 1",
                "actas/acta-1.pdf", null, LUNES.plusDays(3)), "C001", LUNES.plusDays(3).atTime(10, 0));
        trabajo.radicarAnteproyecto("1/ANTEPROYECTO/v1/a.pdf", "D1", LUNES.plusDays(4).atTime(10, 0), calendario);
        trabajo.asignarEvaluadores(evaluador(20L), evaluador(21L), "J001", LUNES.plusDays(5).atTime(10, 0),
                calendario);
        trabajo.registrarConceptoEvaluador(new Concepto(evaluador(20L), ResultadoConcepto.APROBADO, null,
                LUNES.plusDays(6)), "1/EVALUACION/v1/f.pdf", LUNES.plusDays(6).atTime(10, 0), calendario);
        LocalDate limite = trabajo.getAnteproyecto().getFechaLimiteEvaluacion();

        assertEquals(1, notificador.avisarPlazos(trabajo, limite.minusDays(1)));
        assertEquals(Set.of(21L), usuarios(guardadas()));
    }

    /** En curso del 1-mar al 1-dic-2026 (9 reuniones), con los informes y las reuniones dados. */
    private TrabajoGrado enCurso(int informes, List<ReunionSeguimiento> reuniones) {
        List<InformeMensual> radicados = java.util.stream.IntStream.rangeClosed(1, informes)
                .mapToObj(numero -> new InformeMensual(numero, "informes/" + numero + ".pdf",
                        LocalDate.of(2026, 3, 1).plusMonths(numero), null, null, List.of()))
                .toList();
        return TrabajoGrado.reconstruir(new DatosTrabajoGrado(8L, Modalidad.TRABAJO_DE_INVESTIGACION,
                "Vigente", List.of("Objetivo"), trabajo().getEstudiantes(), trabajo().getDirector(), null, null,
                null, Proceso.EN_CURSO, null, null,
                new DatosEjecucion(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 1),
                        List.of(), radicados, null, List.of(), reuniones),
                List.of(), null));
    }

    @Test
    void laReunionAgendadaSeRecuerdaElDiaAnterior() {
        LocalDateTime reunion = LocalDate.of(2026, 4, 1).atTime(10, 0);
        TrabajoGrado agendada = enCurso(9, List.of(new ReunionSeguimiento(1, reunion, "Sala 2", "D1", false, null)));

        assertEquals(0, notificador.avisarPlazos(agendada, LocalDate.of(2026, 3, 30)));
        assertEquals(2, notificador.avisarPlazos(agendada, LocalDate.of(2026, 3, 31)));
        assertEquals(Set.of(10L, 1L), usuarios(guardadas()));
        assertEquals(EventoTrabajoGrado.REUNION_PROXIMA, guardadas().get(0).evento());
        assertEquals("Recuerde la reunión de seguimiento 1, el 01/04/2026 a las 10:00 (Sala 2) del trabajo \"Vigente\".",
                guardadas().get(0).mensaje());
    }

    @Test
    void siPasoLaUltimaReunionSinSuInformeSeAvisaElInformePendiente() {
        // Pasó la reunión del 1-may (la segunda): solo se avisa esa, no la de abril
        assertEquals(2, notificador.avisarPlazos(enCurso(0, List.of()), LocalDate.of(2026, 5, 2)));
        assertEquals("REUNION_SIN_INFORME:8:2", guardadas().get(0).clave());
        assertEquals(EventoTrabajoGrado.REUNION_SIN_INFORME, guardadas().get(0).evento());
    }

    @Test
    void conElInformeDeLaUltimaReunionNoSeAvisa() {
        assertEquals(0, notificador.avisarPlazos(enCurso(2, List.of()), LocalDate.of(2026, 5, 2)));
    }

    @Test
    void elTrabajoEnCursoSeAvisaTreintaDiasAntesDelFin() {
        // Con todos sus informes: solo el aviso del fin del plazo
        TrabajoGrado enCurso = enCurso(9, List.of());

        assertEquals(0, notificador.avisarPlazos(enCurso, LocalDate.of(2026, 10, 31)));
        assertEquals(2, notificador.avisarPlazos(enCurso, LocalDate.of(2026, 11, 1)));
        assertEquals(Set.of(10L, 1L), usuarios(guardadas()));
        assertEquals("PLAZO_TRABAJO_POR_VENCER:8:2026-12-01", guardadas().get(0).clave());
    }
}
