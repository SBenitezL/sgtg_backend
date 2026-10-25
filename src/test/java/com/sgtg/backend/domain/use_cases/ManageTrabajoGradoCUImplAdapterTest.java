package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;
import com.sgtg.backend.application.input.trabajo_grado.DatosAsesor;
import com.sgtg.backend.application.input.trabajo_grado.NuevoTrabajoGrado;
import com.sgtg.backend.application.output.ManageArchivoGatewayIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageTrabajoGradoGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.notificacion.EventoTrabajoGrado;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.trabajo_grado.Calificacion;
import com.sgtg.backend.domain.models.trabajo_grado.DatosEjecucion;
import com.sgtg.backend.domain.models.trabajo_grado.EstadoSustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.DatosTrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.MotivoCierre;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.InformeMensual;
import com.sgtg.backend.domain.models.trabajo_grado.RevisionInforme;
import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoInvestigacion;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;
import com.sgtg.backend.infraestructure.output.exception.own.BusinessRuleException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityNotFoundException;
import com.sgtg.backend.infraestructure.output.exception.own.NoAccessException;
import com.sgtg.backend.infraestructure.output.formatter.ExceptionFormatterImplAdapter;

/** Casos de uso de las Fases 1 a 8 con los gateways simulados. */
class ManageTrabajoGradoCUImplAdapterTest {

    private static final Departamento SISTEMAS = new Departamento(1L, "Sistemas");
    private static final Departamento TELEMATICA = new Departamento(2L, "Telemática");
    private static final Programa ING_SISTEMAS = new Programa(1L, "Ingeniería de Sistemas");
    private static final Programa TEC_TELEMATICA = new Programa(2L, "Tecnología en Telemática");
    /** Lunes 2 de marzo de 2026 a las 10:00 en Bogotá */
    private static final Clock RELOJ = Clock.fixed(
            LocalDate.of(2026, 3, 2).atTime(10, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
            ZoneId.of("America/Bogota"));

    private ManageTrabajoGradoGatewayIntPort trabajoGateway;
    private ManageUserGatewayIntPort userGateway;
    private ManagePerfilGatewayIntPort perfilGateway;
    private ManageCatalogoGatewayIntPort catalogoGateway;
    private ManageArchivoGatewayIntPort archivoGateway;
    private NotificadorTrabajoGrado notificador;
    private ManageTrabajoGradoCUImplAdapter casoDeUso;

    private static Usuario usuario(long id, RoleAdmited... roles) {
        return new Usuario(id, "U" + id, TipoDocumentoIdentidad.CC, "10" + id, "u" + id + "@unicauca.edu.co",
                "clave", "Nombre", null, "Apellido", null,
                List.of(roles).stream().map(rol -> new Role(null, rol)).toList());
    }

    @BeforeEach
    void preparar() {
        trabajoGateway = mock(ManageTrabajoGradoGatewayIntPort.class);
        userGateway = mock(ManageUserGatewayIntPort.class);
        perfilGateway = mock(ManagePerfilGatewayIntPort.class);
        catalogoGateway = mock(ManageCatalogoGatewayIntPort.class);
        archivoGateway = mock(ManageArchivoGatewayIntPort.class);
        notificador = mock(NotificadorTrabajoGrado.class);
        casoDeUso = casoDeUsoCon(RELOJ);

        // Catálogo: Ingeniería de Sistemas está adscrito a Sistemas y Tecnología en Telemática a Telemática
        Programa sistemasEnCatalogo = new Programa(1L, "Ingeniería de Sistemas", SISTEMAS);
        Programa telematicaEnCatalogo = new Programa(2L, "Tecnología en Telemática", TELEMATICA);
        when(catalogoGateway.buscarPrograma(1L)).thenReturn(sistemasEnCatalogo);
        when(catalogoGateway.buscarPrograma(2L)).thenReturn(telematicaEnCatalogo);
        when(catalogoGateway.listarProgramas()).thenReturn(List.of(sistemasEnCatalogo, telematicaEnCatalogo));

        // Director (1), estudiantes de Sistemas (10) y de Telemática (11), miembro del comité de Sistemas (50)
        // y usuario sin relación (99)
        when(userGateway.findById(1L)).thenReturn(usuario(1L, RoleAdmited.DOCENTE));
        when(perfilGateway.buscarPerfilDocente(1L))
                .thenReturn(new PerfilDocente(1L, TipoVinculacion.PLANTA, SISTEMAS));
        when(userGateway.findById(10L)).thenReturn(usuario(10L, RoleAdmited.ESTUDIANTE));
        when(perfilGateway.buscarPerfilEstudiante(10L)).thenReturn(new PerfilEstudiante(10L, ING_SISTEMAS));
        when(userGateway.findById(11L)).thenReturn(usuario(11L, RoleAdmited.ESTUDIANTE));
        when(perfilGateway.buscarPerfilEstudiante(11L)).thenReturn(new PerfilEstudiante(11L, TEC_TELEMATICA));
        when(userGateway.findById(50L)).thenReturn(usuario(50L, RoleAdmited.COMITE));
        when(perfilGateway.buscarPerfilDocente(50L))
                .thenReturn(new PerfilDocente(50L, TipoVinculacion.PLANTA, SISTEMAS));
        when(userGateway.findById(99L)).thenReturn(usuario(99L, RoleAdmited.DOCENTE));
        // Miembro del comité de Telemática (51) y del comité sin perfil docente (52)
        when(userGateway.findById(51L)).thenReturn(usuario(51L, RoleAdmited.COMITE, RoleAdmited.DOCENTE));
        when(perfilGateway.buscarPerfilDocente(51L))
                .thenReturn(new PerfilDocente(51L, TipoVinculacion.PLANTA, TELEMATICA));
        when(userGateway.findById(52L)).thenReturn(usuario(52L, RoleAdmited.COMITE));
        // Jefes de los departamentos de Sistemas (60) y Telemática (61), y evaluadores (20, 21)
        when(userGateway.findById(60L)).thenReturn(usuario(60L, RoleAdmited.JEFE_DEP, RoleAdmited.DOCENTE));
        when(perfilGateway.buscarPerfilDocente(60L))
                .thenReturn(new PerfilDocente(60L, TipoVinculacion.PLANTA, SISTEMAS));
        when(userGateway.findById(61L)).thenReturn(usuario(61L, RoleAdmited.JEFE_DEP, RoleAdmited.DOCENTE));
        when(perfilGateway.buscarPerfilDocente(61L))
                .thenReturn(new PerfilDocente(61L, TipoVinculacion.PLANTA, TELEMATICA));
        when(userGateway.findById(20L)).thenReturn(usuario(20L, RoleAdmited.DOCENTE));
        when(userGateway.findById(21L)).thenReturn(usuario(21L, RoleAdmited.EVALUADOR));
        // Evaluadores y jurados son docentes del departamento del director (Sistemas)
        for (long docente : new long[] { 20L, 21L, 30L, 99L })
            when(perfilGateway.buscarPerfilDocente(docente))
                    .thenReturn(new PerfilDocente(docente, TipoVinculacion.PLANTA, SISTEMAS));
        // Docente de Telemática (23) y asesor con perfil en Sistemas, sin rol para evaluar (22)
        when(userGateway.findById(23L)).thenReturn(usuario(23L, RoleAdmited.DOCENTE));
        when(perfilGateway.buscarPerfilDocente(23L))
                .thenReturn(new PerfilDocente(23L, TipoVinculacion.PLANTA, TELEMATICA));
        when(userGateway.findById(22L)).thenReturn(usuario(22L, RoleAdmited.ASESOR));
        when(perfilGateway.buscarPerfilDocente(22L))
                .thenReturn(new PerfilDocente(22L, TipoVinculacion.PLANTA, SISTEMAS));
        // Miembro del Consejo de Facultad (70) y jurado de otros trabajos (30)
        when(userGateway.findById(70L)).thenReturn(usuario(70L, RoleAdmited.CONSEJO));
        when(userGateway.findById(30L)).thenReturn(usuario(30L, RoleAdmited.JURADO));
        // El gateway devuelve el mismo trabajo que recibe
        when(trabajoGateway.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        // El almacenamiento devuelve <trabajo>/<carpeta>/v1/<nombre>
        when(archivoGateway.guardar(any(), any(), any(), any())).thenAnswer(invocacion -> invocacion.getArgument(0)
                + "/" + invocacion.getArgument(1) + "/v1/" + ((ArchivoAdjunto) invocacion.getArgument(3)).nombre());
    }

    /** Caso de uso con el reloj dado. */
    private ManageTrabajoGradoCUImplAdapter casoDeUsoCon(Clock reloj) {
        return new ManageTrabajoGradoCUImplAdapter(trabajoGateway, userGateway, perfilGateway, catalogoGateway,
                archivoGateway, new CalendarioHabilFalso(), reloj, new ExceptionFormatterImplAdapter(), notificador);
    }

    /** Archivo PDF de prueba con el nombre dado. */
    private static ArchivoAdjunto pdf(String nombre) {
        return new ArchivoAdjunto(nombre, "application/pdf", "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII));
    }

    private NuevoTrabajoGrado investigacion() {
        return new NuevoTrabajoGrado(Modalidad.TRABAJO_DE_INVESTIGACION, "Título", List.of("Objetivo"),
                List.of(10L), null, null, null);
    }

    private TrabajoGrado trabajoGuardado() {
        TrabajoGrado trabajo = casoDeUso.crear(1L, investigacion());
        when(trabajoGateway.buscarPorId(7L)).thenReturn(trabajo);
        return trabajo;
    }

    @Test
    void creaElTrabajoConElDocenteAutenticadoComoDirector() {
        TrabajoGrado trabajo = casoDeUso.crear(1L, investigacion());

        assertInstanceOf(TrabajoInvestigacion.class, trabajo);
        assertEquals(1L, trabajo.getDirector().getUsuarioId());
        assertEquals(TipoVinculacion.PLANTA, trabajo.getDirector().getTipoVinculacion());
        assertEquals(ING_SISTEMAS, trabajo.getEstudiantes().get(0).getPrograma());
        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
        verify(trabajoGateway).guardar(trabajo);
        // El director gana el rol DIRECTOR (rol de contexto)
        verify(userGateway).agregarRol(1L, "DIRECTOR");
    }

    @Test
    void creaElTrabajoConDescripcionYLineaDeInvestigacion() {
        TrabajoGrado trabajo = casoDeUso.crear(1L, new NuevoTrabajoGrado(Modalidad.TRABAJO_DE_INVESTIGACION,
                "Título", List.of("Objetivo"), List.of(10L), null, null, null, "Descripción", "Ingeniería de software"));
        assertEquals("Descripción", trabajo.getDescripcion());
        assertEquals("Ingeniería de software", trabajo.getLineaInvestigacion());
    }

    @Test
    void soloElDirectorCorrigeLaPropuesta() {
        trabajoGuardado();
        assertThrows(NoAccessException.class,
                () -> casoDeUso.actualizarPropuesta(7L, 10L, "T", List.of("O"), null, null));
        TrabajoGrado trabajo = casoDeUso.actualizarPropuesta(7L, 1L, "Nuevo", List.of("O"), "D", null);
        assertEquals("Nuevo", trabajo.getTitulo());
    }

    @Test
    void creaPracticaProfesionalConAsesorExternoYCarta() {
        NuevoTrabajoGrado datos = new NuevoTrabajoGrado(Modalidad.PRACTICA_PROFESIONAL, "Título",
                List.of("Objetivo"), List.of(10L), null,
                new DatosAsesor(null, "1061000000", "Asesor", null, "Externo", null, "a@empresa.com", "Empresa",
                        true),
                pdf("carta.pdf"));
        // La carta se guarda en una ubicación temporal y se mueve a la carpeta del trabajo ya guardado
        when(archivoGateway.guardarTemporal(any())).thenReturn("temporal/abc/carta.pdf");
        when(archivoGateway.reubicar(eq("temporal/abc/carta.pdf"), any(), eq("CARTA_ACEPTACION"),
                eq("Carta de aceptación")))
                .thenReturn("9/CARTA_ACEPTACION/v1/carta.pdf");

        PracticaProfesional practica = assertInstanceOf(PracticaProfesional.class, casoDeUso.crear(1L, datos));
        assertEquals("Empresa", practica.getAsesor().getOrganizacion());
        assertEquals(LocalDate.of(2026, 3, 2), practica.getCartaAceptacion().fechaCarga());
        assertEquals("9/CARTA_ACEPTACION/v1/carta.pdf", practica.getCartaAceptacion().ruta());
    }

    @Test
    void elDirectorCargaUnaNuevaVersionDeLaCartaDeAceptacion() {
        NuevoTrabajoGrado datos = new NuevoTrabajoGrado(Modalidad.PRACTICA_PROFESIONAL, "Título",
                List.of("Objetivo"), List.of(10L), null,
                new DatosAsesor(null, "1061000000", "Asesor", null, "Externo", null, "a@empresa.com", "Empresa",
                        true),
                pdf("carta.pdf"));
        when(archivoGateway.guardarTemporal(any())).thenReturn("temporal/abc/carta.pdf");
        when(archivoGateway.reubicar(any(), any(), any(), any())).thenReturn("9/CARTA_ACEPTACION/v1/carta.pdf");
        TrabajoGrado practica = casoDeUso.crear(1L, datos);
        when(trabajoGateway.buscarPorId(9L)).thenReturn(practica);

        assertThrows(NoAccessException.class,
                () -> casoDeUso.actualizarCartaAceptacion(9L, 10L, pdf("carta-2.pdf")));
        PracticaProfesional corregida = assertInstanceOf(PracticaProfesional.class,
                casoDeUso.actualizarCartaAceptacion(9L, 1L, pdf("carta-2.pdf")));

        verify(archivoGateway).guardar(eq(9L), eq("CARTA_ACEPTACION"), eq("Carta de aceptación"), any());
        assertEquals("9/CARTA_ACEPTACION/v1/carta-2.pdf", corregida.getCartaAceptacion().ruta());
        assertEquals(2, corregida.getCartaAceptacion().version());
    }

    @Test
    void laCartaDeAceptacionSoloEsDeLaPracticaYSiNoSeAceptaSeEliminaElArchivo() {
        trabajoGuardado();
        BusinessRuleException investigacion = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.actualizarCartaAceptacion(7L, 1L, pdf("carta.pdf")));
        assertEquals("error.trabajo_grado.carta_solo_practica", investigacion.getBusinessRule());
        verify(archivoGateway, never()).guardar(any(), eq("CARTA_ACEPTACION"), any(), any());

        // Mientras el Comité revisa la propuesta no se corrige la carta: se elimina el archivo cargado
        NuevoTrabajoGrado datos = new NuevoTrabajoGrado(Modalidad.PRACTICA_PROFESIONAL, "Título",
                List.of("Objetivo"), List.of(10L), null,
                new DatosAsesor(null, "1061000000", "Asesor", null, "Externo", null, "a@empresa.com", "Empresa",
                        true),
                pdf("carta.pdf"));
        when(archivoGateway.guardarTemporal(any())).thenReturn("temporal/abc/carta.pdf");
        when(archivoGateway.reubicar(any(), any(), any(), any())).thenReturn("9/CARTA_ACEPTACION/v1/carta.pdf");
        TrabajoGrado practica = casoDeUso.crear(1L, datos);
        when(trabajoGateway.buscarPorId(9L)).thenReturn(practica);
        casoDeUso.radicarPropuesta(9L, 1L, pdf("p.pdf"));

        assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.actualizarCartaAceptacion(9L, 1L, pdf("carta-2.pdf")));
        verify(archivoGateway).eliminar("9/CARTA_ACEPTACION/v1/carta-2.pdf");
    }

    @Test
    void siLaPracticaNoSeCreaSeEliminaLaCartaTemporal() {
        // Sin asesor de la organización el dominio rechaza la práctica
        NuevoTrabajoGrado datos = new NuevoTrabajoGrado(Modalidad.PRACTICA_PROFESIONAL, "Título",
                List.of("Objetivo"), List.of(10L), null, null, pdf("carta.pdf"));
        when(archivoGateway.guardarTemporal(any())).thenReturn("temporal/abc/carta.pdf");

        assertThrows(ReglaNegocioException.class, () -> casoDeUso.crear(1L, datos));
        verify(archivoGateway).eliminar("temporal/abc/carta.pdf");
        verify(trabajoGateway, never()).guardar(any());
    }

    @Test
    void elDirectorDebeTenerPerfilDocente() {
        when(perfilGateway.buscarPerfilDocente(1L)).thenReturn(null);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.crear(1L, investigacion()));
        assertEquals("error.trabajo_grado.director_sin_perfil", error.getBusinessRule());
        verify(trabajoGateway, never()).guardar(any());
    }

    @Test
    void losEstudiantesDebenTenerPerfil() {
        when(perfilGateway.buscarPerfilEstudiante(10L)).thenReturn(null);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.crear(1L, investigacion()));
        assertEquals("error.trabajo_grado.estudiante_sin_perfil", error.getBusinessRule());
    }

    @Test
    void lasReglasDelDominioSePropagan() {
        // Aunque tenga perfil docente, un usuario con rol ESTUDIANTE no puede ser director:
        // el dominio rechaza el rol
        when(perfilGateway.buscarPerfilDocente(10L))
                .thenReturn(new PerfilDocente(10L, TipoVinculacion.PLANTA, SISTEMAS));
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.crear(10L, investigacion()));
        assertEquals("error.dominio.participante.rol_no_permitido", error.getMessage());
    }

    @Test
    void soloElDirectorRadicaLaPropuesta() {
        trabajoGuardado();
        assertThrows(NoAccessException.class,
                () -> casoDeUso.radicarPropuesta(7L, 10L, pdf("p.pdf")));

        TrabajoGrado radicado = casoDeUso.radicarPropuesta(7L, 1L, pdf("p.pdf"));
        assertEquals(Estado.EN_REVISION, radicado.getPropuesta().getEstado());
        // 10 días hábiles desde el lunes 2-mar-2026 según el reloj fijo
        assertEquals(LocalDate.of(2026, 3, 16), radicado.getPropuesta().getFechaLimiteConcepto());
    }

    @Test
    void laDecisionDelComiteRegistraAlResponsableYAvanza() {
        trabajoGuardado();
        casoDeUso.radicarPropuesta(7L, 1L, pdf("p.pdf"));

        TrabajoGrado trabajo = casoDeUso.registrarDecisionComite(7L, 50L, ResultadoConcepto.APROBADO, "Acta 1",
                pdf("acta-1.pdf"), null, null);

        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals("U50", trabajo.getHistorial().get(0).responsable());
        assertEquals(LocalDate.of(2026, 3, 2), trabajo.getPropuesta().getDecisiones().get(0).fecha());
        assertEquals("7/ACTA_COMITE/v1/acta-1.pdf", trabajo.getPropuesta().getDecisiones().get(0).rutaArchivo());
        // Queda registrado quién emitió el concepto en nombre del Comité
        assertEquals(50L, trabajo.getPropuesta().getDecisiones().get(0).responsableId());
    }

    /** Trabajo guardado con la propuesta aprobada, en la etapa de anteproyecto. */
    private TrabajoGrado trabajoEnAnteproyecto() {
        trabajoGuardado();
        casoDeUso.radicarPropuesta(7L, 1L, pdf("p.pdf"));
        return casoDeUso.registrarDecisionComite(7L, 50L, ResultadoConcepto.APROBADO, "Acta 1", pdf("acta-1.pdf"), null,
                null);
    }

    @Test
    void soloElDirectorRadicaElAnteproyecto() {
        trabajoEnAnteproyecto();
        assertThrows(NoAccessException.class,
                () -> casoDeUso.radicarAnteproyecto(7L, 10L, pdf("a.pdf")));

        TrabajoGrado trabajo = casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        assertEquals(Estado.EN_REVISION, trabajo.getAnteproyecto().getEstado());
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
    }

    @Test
    void elJefeDelDepartamentoDelDirectorAsignaLosEvaluadores() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));

        TrabajoGrado trabajo = casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L);

        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertEquals(List.of(20L, 21L),
                trabajo.getAnteproyecto().getEvaluadores().stream().map(e -> e.getUsuarioId()).toList());
        assertEquals("U60", trabajo.getHistorial().get(1).responsable());
        // 10 días hábiles desde el lunes 2-mar-2026 según el reloj fijo
        assertEquals(LocalDate.of(2026, 3, 16), trabajo.getAnteproyecto().getFechaLimiteEvaluacion());
        // Rol de contexto para que accedan a la evaluación
        verify(userGateway).agregarRol(20L, "EVALUADOR");
        verify(userGateway).agregarRol(21L, "EVALUADOR");
    }

    @Test
    void unJefeDeOtroDepartamentoNoAsignaEvaluadores() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        when(perfilGateway.buscarPerfilDocente(60L))
                .thenReturn(new PerfilDocente(60L, TipoVinculacion.PLANTA, new Departamento(2L, "Telemática")));

        NoAccessException error = assertThrows(NoAccessException.class,
                () -> casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L));
        assertEquals("error.trabajo_grado.otro_departamento", error.getMessage());
    }

    @Test
    void elJefeDebeTenerPerfilDocente() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        when(perfilGateway.buscarPerfilDocente(60L)).thenReturn(null);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L));
        assertEquals("error.trabajo_grado.jefe_sin_perfil", error.getBusinessRule());
    }

    @Test
    void elEvaluadorDebeTenerUnRolQueLePermitaEvaluar() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));

        // El usuario 22 solo tiene rol ASESOR
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.asignarEvaluadores(7L, 60L, 20L, 22L));
        assertEquals("error.dominio.participante.rol_no_permitido", error.getMessage());
    }

    @Test
    void losEvaluadoresSonDocentesDelDepartamentoSinJefeNiComite() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));

        // Miembro del Comité (50) o jefe de departamento (61): deciden sobre el trabajo
        for (long decisor : new long[] { 50L, 61L }) {
            BusinessRuleException error = assertThrows(BusinessRuleException.class,
                    () -> casoDeUso.asignarEvaluadores(7L, 60L, 20L, decisor));
            assertEquals("error.trabajo_grado.evaluador_con_rol_decisor", error.getBusinessRule());
        }
        // Docente de otro departamento (23)
        BusinessRuleException otroDepartamento = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.asignarEvaluadores(7L, 60L, 20L, 23L));
        assertEquals("error.trabajo_grado.evaluador_otro_departamento", otroDepartamento.getBusinessRule());
        verify(userGateway, never()).agregarRol(20L, "EVALUADOR");

        assertEquals(Proceso.EVALUACION, casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L).getEtapa());
    }

    @Test
    void soloLosEvaluadoresAsignadosEmitenConcepto() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L);

        assertThrows(NoAccessException.class, () -> casoDeUso.registrarConceptoEvaluador(7L, 99L,
                ResultadoConcepto.APROBADO, null, pdf("b.pdf"), null));

        casoDeUso.registrarConceptoEvaluador(7L, 20L, ResultadoConcepto.APROBADO, null, pdf("b1.pdf"), null);
        TrabajoGrado trabajo = casoDeUso.registrarConceptoEvaluador(7L, 21L, ResultadoConcepto.APROBADO, null,
                pdf("b2.pdf"), pdf("notas.pdf"));

        // Aprobado por ambos: sigue en evaluación hasta que el jefe lo remita con el formato C
        assertEquals(Proceso.EVALUACION, trabajo.getEtapa());
        assertEquals(LocalDate.of(2026, 3, 2), trabajo.getAnteproyecto().getConceptos().get(0).concepto().fecha());
        assertEquals(TipoDocumento.ANOTACIONES_EVALUACION,
                trabajo.getAnteproyecto().getConceptos().get(1).anotaciones().tipo());
        assertTrue(trabajo.participa(21L));
    }

    @Test
    void elConceptoExigeElFormatoB() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.registrarConceptoEvaluador(7L, 20L, ResultadoConcepto.APROBADO, null, null, null));
        assertEquals("error.dominio.anteproyecto.formato_b_obligatorio", error.getMessage());
    }

    @Test
    void elJefeDelDepartamentoRemiteAlConsejoConElFormatoC() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L);
        casoDeUso.registrarConceptoEvaluador(7L, 20L, ResultadoConcepto.APROBADO, null, pdf("b1.pdf"), null);
        casoDeUso.registrarConceptoEvaluador(7L, 21L, ResultadoConcepto.APROBADO, null, pdf("b2.pdf"), null);

        // Sin formato C no se remite
        ReglaNegocioException sinFormato = assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.remitirAnteproyectoAlConsejo(7L, 60L, null));
        assertEquals("error.dominio.anteproyecto.formato_c_obligatorio", sinFormato.getMessage());

        TrabajoGrado trabajo = casoDeUso.remitirAnteproyectoAlConsejo(7L, 60L, pdf("formato-c.pdf"));

        assertEquals(Proceso.APROBACION_CONSEJO, trabajo.getEtapa());
        verify(archivoGateway).guardar(eq(7L), eq("TI-C"), eq("Formato C"), any());
        assertEquals(1, trabajo.getAnteproyecto().getRemisiones().size());
        assertEquals("U60", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
    }

    @Test
    void unJefeDeOtroDepartamentoNoRemite() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L);
        when(perfilGateway.buscarPerfilDocente(60L))
                .thenReturn(new PerfilDocente(60L, TipoVinculacion.PLANTA, new Departamento(2L, "Telemática")));

        NoAccessException error = assertThrows(NoAccessException.class,
                () -> casoDeUso.remitirAnteproyectoAlConsejo(7L, 60L, pdf("formato-c.pdf")));
        assertEquals("error.trabajo_grado.otro_departamento", error.getMessage());
    }

    /** Trabajo con el anteproyecto aprobado por ambos evaluadores. */
    private void trabajoEnAprobacionConsejo() {
        trabajoEnAnteproyecto();
        casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf"));
        casoDeUso.asignarEvaluadores(7L, 60L, 20L, 21L);
        casoDeUso.registrarConceptoEvaluador(7L, 20L, ResultadoConcepto.APROBADO, null, pdf("b1.pdf"), null);
        casoDeUso.registrarConceptoEvaluador(7L, 21L, ResultadoConcepto.APROBADO, null, pdf("b2.pdf"), null);
        casoDeUso.remitirAnteproyectoAlConsejo(7L, 60L, pdf("formato-c.pdf"));
    }

    @Test
    void elConsejoApruebaConLaFechaActualComoNotificacion() {
        trabajoEnAprobacionConsejo();

        TrabajoGrado trabajo = casoDeUso.registrarDecisionConsejo(7L, 70L, ResultadoConcepto.APROBADO,
                pdf("res-100.pdf"), null, 9);

        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        assertEquals(70L, trabajo.getAnteproyecto().getDecisionesConsejo().get(0).responsableId());
        // Reloj fijo: lunes 2-mar-2026; investigación dura 9 meses
        assertEquals(LocalDate.of(2026, 3, 2), trabajo.getEjecucion().getFechaNotificacion());
        assertEquals(LocalDate.of(2026, 12, 2), trabajo.getEjecucion().getFechaFin());
        assertEquals("7/RESOLUCION/v1/res-100.pdf",
                trabajo.getAnteproyecto().getDecisionesConsejo().get(0).rutaArchivo());
        assertEquals("U70", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
    }

    @Test
    void siElConsejoNoApruebaVuelveAAnteproyectoSinComentarioObligatorio() {
        trabajoEnAprobacionConsejo();

        TrabajoGrado trabajo = casoDeUso.registrarDecisionConsejo(7L, 70L, ResultadoConcepto.NO_APROBADO,
                pdf("res-101.pdf"), null, 9);

        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals(Estado.NO_APROBADO, trabajo.getAnteproyecto().getEstado());
        assertEquals(Proceso.EVALUACION,
                casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a2.pdf")).getEtapa());
    }

    /** Trabajo aprobado por el Consejo (en curso desde el 2-mar-2026). */
    private void trabajoEnCurso() {
        trabajoEnAprobacionConsejo();
        casoDeUso.registrarDecisionConsejo(7L, 70L, ResultadoConcepto.APROBADO, pdf("res-100.pdf"), null, 9);
    }

    @Test
    void elDirectorSolicitaElCambioDeObjetivosYElConsejoLoDecide() {
        trabajoEnCurso();
        assertThrows(NoAccessException.class, () -> casoDeUso.solicitarCambioObjetivos(7L, 10L, "Nuevo título",
                List.of("Nuevo objetivo"), "Justificación"));

        casoDeUso.solicitarCambioObjetivos(7L, 1L, "Nuevo título", List.of("Nuevo objetivo"), "Justificación");
        TrabajoGrado trabajo = casoDeUso.resolverCambioObjetivos(7L, 70L, ResultadoConcepto.APROBADO,
                pdf("res-200.pdf"), null);

        assertEquals("Nuevo título", trabajo.getTitulo());
        assertEquals(LocalDate.of(2026, 3, 2),
                trabajo.getEjecucion().getCambiosObjetivos().get(0).decision().fecha());
    }

    @Test
    void enInvestigacionElEstudianteRadicaLosInformesMensuales() {
        trabajoEnCurso();
        // En investigación el director no radica; un usuario ajeno tampoco
        assertThrows(NoAccessException.class,
                () -> casoDeUso.registrarInformeMensual(7L, 1L, null, pdf("abril.pdf")));
        assertThrows(NoAccessException.class,
                () -> casoDeUso.registrarInformeMensual(7L, 99L, null, pdf("abril.pdf")));

        TrabajoGrado trabajo = casoDeUso.registrarInformeMensual(7L, 10L, "Avance de marzo", pdf("abril.pdf"));

        InformeMensual informe = trabajo.getEjecucion().getInformesMensuales().get(0);
        assertEquals(1, informe.numero());
        assertEquals("U10", informe.radicadoPor());
        assertEquals("Avance de marzo", informe.observaciones());
        verify(archivoGateway).guardar(eq(7L), eq("TI-D"), eq("Formato D"), any());
    }

    @Test
    void elDirectorRevisaElInformeConSusNotasSinCambiarElEstado() {
        trabajoEnCurso();
        casoDeUso.registrarInformeMensual(7L, 10L, null, pdf("abril.pdf"));
        // El estudiante no revisa informes
        assertThrows(NoAccessException.class,
                () -> casoDeUso.revisarInformeMensual(7L, 1, 10L, "Bien", null));

        TrabajoGrado trabajo = casoDeUso.revisarInformeMensual(7L, 1, 1L, "Ajustar el cronograma", pdf("notas.pdf"));

        RevisionInforme revision = trabajo.getEjecucion().getInformesMensuales().get(0).revisiones().get(0);
        assertEquals(1L, revision.revisorUsuarioId());
        assertEquals("Ajustar el cronograma", revision.observaciones());
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        verify(archivoGateway).guardar(eq(7L), eq("REVISION_INFORME"), eq("Notas de revisión"), any());
        // Una revisión por revisor
        ReglaNegocioException repetida = assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.revisarInformeMensual(7L, 1, 1L, "Otra", null));
        assertEquals("error.dominio.informe_mensual.revision_repetida", repetida.getMessage());
    }

    @Test
    void elDirectorSolicitaLaProrrogaYElConsejoLaAprueba() {
        trabajoEnCurso();
        assertThrows(NoAccessException.class,
                () -> casoDeUso.solicitarProrroga(7L, 10L, 2, pdf("prorroga.pdf")));

        casoDeUso.solicitarProrroga(7L, 1L, 2, pdf("prorroga.pdf"));
        TrabajoGrado trabajo = casoDeUso.resolverProrroga(7L, 70L, ResultadoConcepto.APROBADO,
                pdf("res-300.pdf"), null);

        assertEquals(Proceso.PRORROGA, trabajo.getEtapa());
        // 9 meses desde el 2-mar-2026 más 2 de prórroga
        assertEquals(LocalDate.of(2027, 2, 2), trabajo.getEjecucion().getFechaFin());
        assertEquals("U70", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
        assertEquals(LocalDate.of(2026, 3, 2), trabajo.getEjecucion().getProrroga().fechaSolicitud());
    }

    @Test
    void elDirectorSolicitaYReanudaLaSuspensionQueApruebaElConsejo() {
        trabajoEnCurso();
        assertThrows(NoAccessException.class,
                () -> casoDeUso.solicitarSuspension(7L, 10L, pdf("suspension.pdf")));

        casoDeUso.solicitarSuspension(7L, 1L, pdf("suspension.pdf"));
        TrabajoGrado trabajo = casoDeUso.resolverSuspension(7L, 70L, ResultadoConcepto.APROBADO,
                pdf("res-400.pdf"), null);

        assertEquals(Proceso.SUSPENDIDO, trabajo.getEtapa());
        // Aprobada el 2-mar-2026 (período 2026-1): se reanuda a más tardar el 1-ene-2027
        assertEquals(LocalDate.of(2027, 1, 1),
                trabajo.getEjecucion().suspensionActiva().get().fechaMaximaReanudacion());
        assertThrows(NoAccessException.class, () -> casoDeUso.reanudar(7L, 10L, pdf("reanudacion.pdf")));

        trabajo = casoDeUso.reanudar(7L, 1L, pdf("reanudacion.pdf"));
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        assertEquals("U1", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
        assertEquals(LocalDate.of(2026, 12, 2), trabajo.getEjecucion().getFechaFin());
    }

    @Test
    void laVerificacionDiariaReanudaLasSuspensionesVencidas() {
        trabajoEnCurso();
        casoDeUso.solicitarSuspension(7L, 1L, pdf("suspension.pdf"));
        TrabajoGrado suspendido = casoDeUso.resolverSuspension(7L, 70L, ResultadoConcepto.APROBADO,
                pdf("res-400.pdf"), null);
        when(trabajoGateway.listarPorEtapa(Proceso.SUSPENDIDO)).thenReturn(List.of(suspendido));
        ManageTrabajoGradoCUImplAdapter enEnero = casoDeUsoCon(
                Clock.fixed(LocalDate.of(2027, 1, 1).atTime(0, 5).atZone(ZoneId.of("America/Bogota")).toInstant(),
                        ZoneId.of("America/Bogota")));

        assertEquals(1, enEnero.verificarVencimientos());
        assertEquals(Proceso.EN_CURSO, suspendido.getEtapa());
    }

    /** Caso de uso con el reloj en la fecha dada (10:00, hora de Bogotá). */
    private ManageTrabajoGradoCUImplAdapter casoDeUsoEn(LocalDate fecha) {
        return casoDeUsoCon(Clock.fixed(fecha.atTime(10, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
                ZoneId.of("America/Bogota")));
    }

    /** Solicita la sustentación con monografía y formato E (sin anexos). */
    private TrabajoGrado solicitarSustentacion(ManageTrabajoGradoCUImplAdapter casoDeUso, long usuarioId) {
        return casoDeUso.solicitarSustentacion(7L, usuarioId, pdf("monografia.pdf"), pdf("formato-e.pdf"), null,
                null);
    }

    /** Decanatura (usuario 80) aprueba el paz y salvo. */
    private TrabajoGrado aprobarPazYSalvo(ManageTrabajoGradoCUImplAdapter casoDeUso) {
        when(userGateway.findById(80L)).thenReturn(usuario(80L, RoleAdmited.DECANATURA));
        return casoDeUso.revisarPazYSalvo(7L, 80L, true, null, pdf("paz-y-salvo.pdf"));
    }

    @Test
    void laSustentacionSeSolicitaProgramaCalificaYFinaliza() {
        trabajoEnCurso();
        // En curso desde el 2-mar-2026: antes de los 3 meses no se puede solicitar
        ReglaNegocioException temprano = assertThrows(ReglaNegocioException.class,
                () -> solicitarSustentacion(casoDeUso, 1L));
        assertEquals("error.dominio.sustentacion.antes_de_tiempo", temprano.getMessage());

        ManageTrabajoGradoCUImplAdapter enJulio = casoDeUsoEn(LocalDate.of(2026, 7, 6));
        assertThrows(NoAccessException.class, () -> solicitarSustentacion(enJulio, 10L));
        TrabajoGrado trabajo = enJulio.solicitarSustentacion(7L, 1L, pdf("monografia.pdf"), pdf("formato-e.pdf"), null, pdf("anexos.pdf"));
        assertEquals("7/MONOGRAFIA/v1/monografia.pdf", trabajo.getSustentacion().getRutaMonografia());
        assertEquals("7/ANEXOS/v1/anexos.pdf", trabajo.getSustentacion().getRutaAnexos());
        assertNull(trabajo.getSustentacion().getRutaPazYSalvo());
        // Sin paz y salvo aprobado no se programa; Decanatura lo aprueba
        ReglaNegocioException sinPaz = assertThrows(ReglaNegocioException.class, () -> enJulio.programarSustentacion(
                7L, 70L, 99L, 30L, LocalDate.of(2026, 7, 6).atTime(15, 0), "Salón 301", pdf("formato-e-consejo.pdf")));
        assertEquals("error.dominio.sustentacion.paz_y_salvo_pendiente", sinPaz.getMessage());
        trabajo = aprobarPazYSalvo(enJulio);
        assertEquals("7/TI-F/v1/paz-y-salvo.pdf", trabajo.getSustentacion().getRutaPazYSalvo());

        // El Consejo programa al coordinador 99 (DOCENTE) y al jurado 30 (JURADO) con el formato E
        trabajo = enJulio.programarSustentacion(7L, 70L, 99L, 30L, LocalDate.of(2026, 7, 6).atTime(15, 0),
                "Salón 301", pdf("formato-e-consejo.pdf"));
        assertEquals(EstadoSustentacion.PROGRAMADA, trabajo.getSustentacion().getEstado());
        assertEquals(99L, trabajo.getSustentacion().sesionActual().coordinador().getUsuarioId());
        assertEquals("7/TI-E/v1/formato-e-consejo.pdf", trabajo.getSustentacion().sesionActual().rutaFormatoE());
        verify(userGateway).agregarRol(99L, "JURADO");
        verify(userGateway).agregarRol(99L, "JURADO_COORDINADOR");
        verify(userGateway).agregarRol(30L, "JURADO");

        // Solo el coordinador califica (el otro jurado solo consulta), con el acta
        assertThrows(NoAccessException.class,
                () -> enJulio.calificarSustentacion(7L, 1L, Calificacion.APROBADO, null, pdf("acta.pdf")));
        NoAccessException noCoordina = assertThrows(NoAccessException.class,
                () -> enJulio.calificarSustentacion(7L, 30L, Calificacion.APROBADO, null, pdf("acta.pdf")));
        assertEquals("error.trabajo_grado.solo_jurado_coordinador", noCoordina.getMessage());
        trabajo = enJulio.calificarSustentacion(7L, 99L, Calificacion.APROBADO, null, pdf("acta-sustentacion.pdf"));
        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());

        assertThrows(NoAccessException.class, () -> enJulio.cargarVersionDefinitiva(7L, 30L, pdf("tg.pdf")));
        trabajo = enJulio.cargarVersionDefinitiva(7L, 99L, pdf("tg.pdf"));
        assertEquals(Proceso.FINALIZADO, trabajo.getEtapa());
        assertEquals("U99", trabajo.getHistorial().get(trabajo.getHistorial().size() - 1).responsable());
    }

    @Test
    void losEvaluadoresDelAnteproyectoNoSonJuradosDelMismoTrabajo() {
        trabajoEnCurso();
        ManageTrabajoGradoCUImplAdapter enJulio = casoDeUsoEn(LocalDate.of(2026, 7, 6));
        solicitarSustentacion(enJulio, 1L);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> enJulio.programarSustentacion(
                7L, 70L, 20L, 30L, LocalDate.of(2026, 7, 10).atTime(15, 0), "Salón 301", pdf("e.pdf")));
        assertEquals("error.dominio.sustentacion.jurado_con_conflicto", error.getMessage());
        verify(userGateway, never()).agregarRol(30L, "JURADO");
    }

    @Test
    void elJuradoDebeTenerUnRolQueLePermitaSerlo() {
        trabajoEnCurso();
        ManageTrabajoGradoCUImplAdapter enJulio = casoDeUsoEn(LocalDate.of(2026, 7, 6));
        solicitarSustentacion(enJulio, 1L);

        // El usuario 22 solo tiene rol ASESOR
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> enJulio.programarSustentacion(
                7L, 70L, 99L, 22L, LocalDate.of(2026, 7, 10).atTime(15, 0), "Salón 301", pdf("e.pdf")));
        assertEquals("error.dominio.participante.rol_no_permitido", error.getMessage());

        // Igual que los evaluadores: ni miembros del Comité ni docentes de otro departamento
        BusinessRuleException comite = assertThrows(BusinessRuleException.class, () -> enJulio.programarSustentacion(
                7L, 70L, 99L, 50L, LocalDate.of(2026, 7, 10).atTime(15, 0), "Salón 301", pdf("e.pdf")));
        assertEquals("error.trabajo_grado.evaluador_con_rol_decisor", comite.getBusinessRule());
        BusinessRuleException otroDepartamento = assertThrows(BusinessRuleException.class,
                () -> enJulio.programarSustentacion(7L, 70L, 99L, 23L, LocalDate.of(2026, 7, 10).atTime(15, 0),
                        "Salón 301", pdf("e.pdf")));
        assertEquals("error.trabajo_grado.evaluador_otro_departamento", otroDepartamento.getBusinessRule());
    }

    @Test
    void elDirectorSolicitaNuevaFechaYElConsejoReprogramaLaSesion() {
        trabajoEnCurso();
        ManageTrabajoGradoCUImplAdapter enJulio = casoDeUsoEn(LocalDate.of(2026, 7, 6));
        solicitarSustentacion(enJulio, 1L);
        aprobarPazYSalvo(enJulio);
        enJulio.programarSustentacion(7L, 70L, 99L, 30L, LocalDate.of(2026, 7, 20).atTime(15, 0), "Salón 301",
                pdf("e.pdf"));

        assertThrows(NoAccessException.class,
                () -> enJulio.solicitarNuevaFechaSustentacion(7L, 10L, pdf("nueva-fecha.pdf")));
        TrabajoGrado trabajo = enJulio.solicitarNuevaFechaSustentacion(7L, 1L, pdf("nueva-fecha.pdf"));
        assertEquals("7/SOLICITUD_NUEVA_FECHA/v1/nueva-fecha.pdf",
                trabajo.getSustentacion().nuevaFechaPendiente().orElseThrow().rutaSolicitud());

        trabajo = enJulio.resolverNuevaFechaSustentacion(7L, 70L, ResultadoConcepto.APROBADO,
                LocalDate.of(2026, 8, 3).atTime(9, 0), null, pdf("res-500.pdf"), null);
        assertEquals(LocalDate.of(2026, 8, 3).atTime(9, 0), trabajo.getSustentacion().sesionActual().fechaHora());
        assertEquals("Salón 301", trabajo.getSustentacion().sesionActual().salon());
        assertEquals("7/RESOLUCION/v1/res-500.pdf",
                trabajo.getSustentacion().getSolicitudesNuevaFecha().get(0).decision().rutaArchivo());
    }

    @Test
    void losAjustesAceptanAnexosYLosVerificaElCoordinador() {
        trabajoEnCurso();
        ManageTrabajoGradoCUImplAdapter enJulio = casoDeUsoEn(LocalDate.of(2026, 7, 6));
        solicitarSustentacion(enJulio, 1L);
        aprobarPazYSalvo(enJulio);
        enJulio.programarSustentacion(7L, 70L, 99L, 30L, LocalDate.of(2026, 7, 6).atTime(15, 0), "Salón 301",
                pdf("e.pdf"));
        enJulio.calificarSustentacion(7L, 99L, Calificacion.APROBADO_CON_OBSERVACIONES, "Ajustar conclusiones",
                pdf("acta.pdf"));

        TrabajoGrado trabajo = enJulio.radicarAjustesSustentacion(7L, 1L, pdf("ajustes.pdf"),
                pdf("anexos-ajustes.pdf"));
        assertEquals("7/ANEXOS/v1/anexos-ajustes.pdf", trabajo.getSustentacion().getRutaAnexosAjustes());
        assertThrows(NoAccessException.class,
                () -> enJulio.verificarAjustesSustentacion(7L, 30L, true, null));
        trabajo = enJulio.verificarAjustesSustentacion(7L, 99L, true, null);
        assertEquals(EstadoSustentacion.APROBADA, trabajo.getSustentacion().getEstado());
    }

    @Test
    void elDirectorSolicitaLaCancelacionYElConsejoLaAprueba() {
        trabajoGuardado();
        assertThrows(NoAccessException.class,
                () -> casoDeUso.solicitarCancelacion(7L, 10L, pdf("cancelacion.pdf")));

        casoDeUso.solicitarCancelacion(7L, 1L, pdf("cancelacion.pdf"));
        TrabajoGrado trabajo = casoDeUso.resolverCancelacion(7L, 70L, ResultadoConcepto.APROBADO,
                pdf("res-500.pdf"), null);

        assertEquals(Proceso.CANCELADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.CANCELACION_DIRECTOR, trabajo.getMotivoCierre());
        assertEquals("U70", trabajo.getHistorial().get(0).responsable());
        assertEquals(LocalDate.of(2026, 3, 2), trabajo.getCancelaciones().get(0).decision().fecha());
    }

    @Test
    void laVerificacionDiariaCierraLosTrabajosVencidos() {
        Director director = new Director(1L, "D1", new NombrePersona("Docente", null, "Prueba", null),
                "d1@unicauca.edu.co", TipoVinculacion.PLANTA, SISTEMAS);
        Estudiante estudiante = new Estudiante(10L, "E10", new NombrePersona("Estudiante", null, "Prueba", null),
                "e10@unicauca.edu.co", ING_SISTEMAS);
        // Vencido el 1-oct-2025 y vigente hasta el 1-dic-2026; el reloj está en el 2-mar-2026
        TrabajoGrado vencido = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L,
                Modalidad.TRABAJO_DE_INVESTIGACION, "Vencido", List.of("Objetivo"), List.of(estudiante), director,
                null, null, null, Proceso.EN_CURSO, null, null,
                new DatosEjecucion(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 10, 1), LocalDate.of(2025, 4, 1)),
                List.of(), null));
        TrabajoGrado vigente = TrabajoGrado.reconstruir(new DatosTrabajoGrado(8L,
                Modalidad.TRABAJO_DE_INVESTIGACION, "Vigente", List.of("Objetivo"), List.of(estudiante), director,
                null, null, null, Proceso.EN_CURSO, null, null,
                new DatosEjecucion(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 6, 1)),
                List.of(), null));
        when(trabajoGateway.listarPorEtapa(Proceso.EN_CURSO)).thenReturn(List.of(vencido, vigente));
        when(trabajoGateway.listarPorEtapa(Proceso.PRORROGA)).thenReturn(List.of());

        assertEquals(1, casoDeUso.verificarVencimientos());

        assertEquals(Proceso.NO_APROBADO, vencido.getEtapa());
        assertEquals(MotivoCierre.VENCIMIENTO_PLAZO, vencido.getMotivoCierre());
        assertEquals(Proceso.EN_CURSO, vigente.getEtapa());
        verify(trabajoGateway).guardar(vencido);
        verify(trabajoGateway, never()).guardar(vigente);
    }

    @Test
    void listarTodosDelegaAlGateway() {
        casoDeUso.listarTodos(70L);
        verify(trabajoGateway).listarTodos();
    }

    @Test
    void listarPorEtapaExigeLaEtapa() {
        assertThrows(BusinessRuleException.class, () -> casoDeUso.listarPorEtapa(null, 70L));
        casoDeUso.listarPorEtapa(Proceso.ANTEPROYECTO, 70L);
        verify(trabajoGateway).listarPorEtapa(Proceso.ANTEPROYECTO);
    }

    @Test
    void consultarExigeParticiparOTenerRolDeConsulta() {
        TrabajoGrado trabajo = trabajoGuardado();
        assertSame(trabajo, casoDeUso.consultar(7L, 10L));
        assertSame(trabajo, casoDeUso.consultar(7L, 50L));
        assertSame(trabajo, casoDeUso.consultar(7L, 70L));
        assertThrows(NoAccessException.class, () -> casoDeUso.consultar(7L, 99L));
    }

    @Test
    void elComiteYElJefeSoloConsultanLosTrabajosDeSuProgramaYDepartamento() {
        // Trabajo de un estudiante de Ingeniería de Sistemas con director del departamento de Sistemas
        TrabajoGrado trabajo = trabajoGuardado();
        when(trabajoGateway.listarTodos()).thenReturn(List.of(trabajo));
        when(trabajoGateway.listarPorEtapa(Proceso.PROPUESTA)).thenReturn(List.of(trabajo));

        // Comité de Sistemas, jefe de Sistemas y Consejo lo ven
        assertEquals(List.of(trabajo), casoDeUso.listarTodos(50L));
        assertEquals(List.of(trabajo), casoDeUso.listarPorEtapa(Proceso.PROPUESTA, 60L));
        assertEquals(List.of(trabajo), casoDeUso.listarTodos(70L));
        assertSame(trabajo, casoDeUso.consultar(7L, 60L));
        // Comité y jefe de Telemática no lo ven ni lo consultan
        assertTrue(casoDeUso.listarTodos(51L).isEmpty());
        assertTrue(casoDeUso.listarPorEtapa(Proceso.PROPUESTA, 61L).isEmpty());
        assertThrows(NoAccessException.class, () -> casoDeUso.consultar(7L, 51L));
        assertThrows(NoAccessException.class, () -> casoDeUso.consultar(7L, 61L));
        assertThrows(NoAccessException.class, () -> casoDeUso.obtenerArchivo(7L, 51L, "7/TI-A/v1/a.pdf"));
        // Sin perfil docente, el Comité no ve trabajos en los que no participa
        assertTrue(casoDeUso.listarTodos(52L).isEmpty());
    }

    @Test
    void soloElComiteDelProgramaEmiteElConcepto() {
        trabajoGuardado();
        casoDeUso.radicarPropuesta(7L, 1L, pdf("p.pdf"));

        NoAccessException otroPrograma = assertThrows(NoAccessException.class, () -> casoDeUso
                .registrarDecisionComite(7L, 51L, ResultadoConcepto.APROBADO, "Acta 1", pdf("acta.pdf"), null, null));
        assertEquals("error.trabajo_grado.otro_programa", otroPrograma.getMessage());
        BusinessRuleException sinPerfil = assertThrows(BusinessRuleException.class, () -> casoDeUso
                .registrarDecisionComite(7L, 52L, ResultadoConcepto.APROBADO, "Acta 1", pdf("acta.pdf"), null, null));
        assertEquals("error.trabajo_grado.comite_sin_perfil", sinPerfil.getBusinessRule());
        verify(archivoGateway, never()).guardar(any(), eq("ACTA_COMITE"), any(), any());
    }

    @Test
    void losEstudiantesDelTrabajoSonDelMismoPrograma() {
        NuevoTrabajoGrado mezclado = new NuevoTrabajoGrado(Modalidad.TRABAJO_DE_INVESTIGACION, "Título",
                List.of("Objetivo"), List.of(10L, 11L), null, null, null);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> casoDeUso.crear(1L, mezclado));
        assertEquals("error.dominio.trabajo_grado.estudiantes_de_otro_programa", error.getMessage());
    }

    // ----- Archivos -----

    @Test
    void elArchivoSeGuardaEnLaCarpetaDelTipoYElDominioSoloGuardaLaRuta() {
        trabajoGuardado();
        TrabajoGrado trabajo = casoDeUso.radicarPropuesta(7L, 1L, pdf("p.pdf"));

        verify(archivoGateway).guardar(eq(7L), eq("TI-A"), eq("Formato A"), any());
        assertEquals("7/TI-A/v1/p.pdf", trabajo.getPropuesta().getVersiones().get(0).documento().ruta());
    }

    @Test
    void soloSeAceptanArchivosPdf() {
        trabajoGuardado();
        BusinessRuleException porExtension = assertThrows(BusinessRuleException.class, () -> casoDeUso
                .radicarPropuesta(7L, 1L, new ArchivoAdjunto("p.docx", "application/msword", pdf("x").contenido())));
        assertEquals("error.archivo.formato_invalido", porExtension.getBusinessRule());
        BusinessRuleException porContenido = assertThrows(BusinessRuleException.class, () -> casoDeUso
                .radicarPropuesta(7L, 1L, new ArchivoAdjunto("p.pdf", "application/pdf", new byte[] { 1, 2, 3 })));
        assertEquals("error.archivo.formato_invalido", porContenido.getBusinessRule());
        verify(archivoGateway, never()).guardar(any(), any(), any(), any());
    }

    @Test
    void siElDominioRechazaLaOperacionSeEliminaElArchivo() {
        // En propuesta todavía no se puede radicar el anteproyecto
        trabajoGuardado();
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.radicarAnteproyecto(7L, 1L, pdf("a.pdf")));
        assertEquals("error.dominio.trabajo_grado.etapa_invalida", error.getMessage());
        verify(archivoGateway).eliminar("7/ANTEPROYECTO/v1/a.pdf");
    }

    @Test
    void sinArchivoElDominioExigeElDocumento() {
        trabajoEnCurso();
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> casoDeUso.solicitarSuspension(7L, 1L, null));
        assertEquals("error.dominio.suspension.documento_obligatorio", error.getMessage());
    }

    @Test
    void sinPermisoElDirectorNoAlmacenaArchivos() {
        trabajoGuardado();
        assertThrows(NoAccessException.class, () -> casoDeUso.radicarPropuesta(7L, 10L, pdf("p.pdf")));
        verify(archivoGateway, never()).guardar(any(), any(), any(), any());
    }

    @Test
    void losArchivosLosDescarganQuienesPuedenConsultarElTrabajo() {
        trabajoGuardado();
        ArchivoAdjunto propuesta = pdf("p.pdf");
        when(archivoGateway.obtener("7/TI-A/v1/p.pdf")).thenReturn(propuesta);

        // Participante (estudiante) y rol de consulta general (comité)
        assertSame(propuesta, casoDeUso.obtenerArchivo(7L, 10L, "7/TI-A/v1/p.pdf"));
        assertSame(propuesta, casoDeUso.obtenerArchivo(7L, 50L, "7/TI-A/v1/p.pdf"));
        // Sin relación con el trabajo
        NoAccessException sinAcceso = assertThrows(NoAccessException.class,
                () -> casoDeUso.obtenerArchivo(7L, 99L, "7/TI-A/v1/p.pdf"));
        assertEquals("error.archivo.sin_acceso", sinAcceso.getMessage());
    }

    @Test
    void soloSeEntreganArchivosDeLaCarpetaDelTrabajo() {
        trabajoGuardado();
        assertThrows(EntityNotFoundException.class, () -> casoDeUso.obtenerArchivo(7L, 1L, "8/TI-A/v1/p.pdf"));
        assertThrows(EntityNotFoundException.class, () -> casoDeUso.obtenerArchivo(7L, 1L, "7/../8/TI-A/v1/p.pdf"));
        // Ruta del trabajo que no existe en el almacenamiento
        assertThrows(EntityNotFoundException.class, () -> casoDeUso.obtenerArchivo(7L, 1L, "7/TI-A/v9/p.pdf"));
        verify(archivoGateway, never()).obtener("8/TI-A/v1/p.pdf");
    }

    @Test
    void trabajoInexistente() {
        assertThrows(EntityNotFoundException.class, () -> casoDeUso.consultar(404L, 1L));
    }

    // ----- Fase 7: anulación, consulta de Decanatura y notificaciones -----

    @Test
    void soloElDirectorAnulaYSeNotifica() {
        TrabajoGrado trabajo = trabajoGuardado();
        assertThrows(NoAccessException.class, () -> casoDeUso.anular(7L, 10L, "x"));

        TrabajoGrado anulado = casoDeUso.anular(7L, 1L, " Sin financiación ");

        assertEquals(Proceso.CANCELADO, anulado.getEtapa());
        assertEquals(MotivoCierre.ANULACION_DIRECTOR, anulado.getMotivoCierre());
        verify(notificador).notificar(trabajo, EventoTrabajoGrado.TRABAJO_ANULADO, 1L, "Sin financiación");
    }

    @Test
    void cadaOperacionNotificaSuHechoConElActor() {
        TrabajoGrado trabajo = trabajoGuardado();
        verify(notificador).notificar(trabajo, EventoTrabajoGrado.TRABAJO_REGISTRADO, 1L, null);

        casoDeUso.radicarPropuesta(7L, 1L, pdf("propuesta.pdf"));
        verify(notificador).notificar(trabajo, EventoTrabajoGrado.PROPUESTA_RADICADA, 1L, null);

        casoDeUso.registrarDecisionComite(7L, 50L, ResultadoConcepto.APROBADO, "Acta 1", pdf("acta.pdf"), null,
                null);
        verify(notificador).notificar(trabajo, EventoTrabajoGrado.DECISION_COMITE, 50L, "aprobado");
    }

    @Test
    void siLaOperacionFallaNoSeNotifica() {
        trabajoGuardado();
        // Sin documento el dominio rechaza la radicación
        assertThrows(ReglaNegocioException.class, () -> casoDeUso.radicarPropuesta(7L, 1L, null));
        verify(notificador, never()).notificar(any(), eq(EventoTrabajoGrado.PROPUESTA_RADICADA), any(), any());
    }

    @Test
    void decanaturaConsultaCualquierTrabajo() {
        trabajoGuardado();
        when(userGateway.findById(80L)).thenReturn(usuario(80L, RoleAdmited.DECANATURA));
        assertEquals("Título", casoDeUso.consultar(7L, 80L).getTitulo());
    }

    @Test
    void laTareaDiariaAvisaLosPlazosDeLosTrabajosActivos() {
        TrabajoGrado trabajo = trabajoGuardado();
        when(trabajoGateway.listarPorEtapa(Proceso.PROPUESTA)).thenReturn(List.of(trabajo));

        casoDeUso.verificarVencimientos();

        verify(notificador).avisarPlazos(trabajo, LocalDate.of(2026, 3, 2));
    }

    // ----- Un trabajo activo por estudiante -----

    /** Trabajo del estudiante 10 en la etapa dada (con su motivo de cierre si es final). */
    private TrabajoGrado trabajoDelEstudianteEn(Proceso etapa) {
        TrabajoGrado otro = casoDeUso.crear(1L, investigacion());
        if (etapa == Proceso.CANCELADO)
            otro.anular("Cambio de modalidad", "D1", LocalDate.of(2026, 3, 2).atTime(10, 0));
        else if (etapa != Proceso.PROPUESTA)
            otro = TrabajoGrado.reconstruir(new DatosTrabajoGrado(8L, Modalidad.TRABAJO_DE_INVESTIGACION, "Otro",
                    List.of("Objetivo"), otro.getEstudiantes(), otro.getDirector(), null, null, null, etapa, null, null,
                    null, List.of(), null));
        return otro;
    }

    @Test
    void unEstudianteConUnTrabajoActivoNoEntraEnOtro() {
        List<TrabajoGrado> otros = List.of(trabajoDelEstudianteEn(Proceso.EN_CURSO));
        when(trabajoGateway.listarPorParticipante(10L)).thenReturn(otros);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.crear(1L, investigacion()));
        assertEquals("error.trabajo_grado.estudiante_con_trabajo_activo", error.getBusinessRule());
    }

    @Test
    void unEstudianteQueYaAproboNoEntraEnOtro() {
        List<TrabajoGrado> otros = List.of(trabajoDelEstudianteEn(Proceso.FINALIZADO));
        when(trabajoGateway.listarPorParticipante(10L)).thenReturn(otros);
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.crear(1L, investigacion()));
        assertEquals("error.trabajo_grado.estudiante_con_trabajo_aprobado", error.getBusinessRule());
    }

    @Test
    void conUnTrabajoAnuladoONoAprobadoSiPuedeEntrarEnOtro() {
        List<TrabajoGrado> otros = List.of(trabajoDelEstudianteEn(Proceso.CANCELADO),
                trabajoDelEstudianteEn(Proceso.NO_APROBADO));
        when(trabajoGateway.listarPorParticipante(10L)).thenReturn(otros);
        assertEquals(Proceso.PROPUESTA, casoDeUso.crear(1L, investigacion()).getEtapa());
    }

    @Test
    void siElEstudianteSoloDirigeOtroTrabajoNoCuenta() {
        // El usuario 10 aparece en un trabajo activo, pero no como estudiante (ej. un trabajo ajeno)
        TrabajoGrado ajeno = mock(TrabajoGrado.class);
        when(ajeno.esEstudiante(10L)).thenReturn(false);
        when(trabajoGateway.listarPorParticipante(10L)).thenReturn(List.of(ajeno));
        assertEquals(Proceso.PROPUESTA, casoDeUso.crear(1L, investigacion()).getEtapa());
    }

    @Test
    void losEstudiantesDisponiblesExcluyenLosActivosYLosAprobados() {
        when(userGateway.listar()).thenReturn(List.of(usuario(10L, RoleAdmited.ESTUDIANTE),
                usuario(11L, RoleAdmited.ESTUDIANTE), usuario(12L, RoleAdmited.ESTUDIANTE),
                usuario(13L, RoleAdmited.ESTUDIANTE), usuario(1L, RoleAdmited.DOCENTE)));
        Estudiante e11 = new Estudiante(11L, "E11", new NombrePersona("Once", null, "Prueba", null), "e11@u.co",
                ING_SISTEMAS);
        Estudiante e12 = new Estudiante(12L, "E12", new NombrePersona("Doce", null, "Prueba", null), "e12@u.co",
                ING_SISTEMAS);
        Estudiante e13 = new Estudiante(13L, "E13", new NombrePersona("Trece", null, "Prueba", null), "e13@u.co",
                ING_SISTEMAS);
        Director director = trabajoDelEstudianteEn(Proceso.PROPUESTA).getDirector();
        java.util.function.BiFunction<Estudiante, Proceso, TrabajoGrado> trabajo = (estudiante, etapa) -> TrabajoGrado
                .reconstruir(new DatosTrabajoGrado(9L, Modalidad.TRABAJO_DE_INVESTIGACION, "T", List.of("O"),
                        List.of(estudiante), director, null, null, null, etapa, null, null, null, List.of(), null));
        when(trabajoGateway.listarTodos()).thenReturn(List.of(trabajo.apply(e11, Proceso.EN_CURSO),
                trabajo.apply(e12, Proceso.FINALIZADO), trabajo.apply(e13, Proceso.NO_APROBADO)));

        assertEquals(List.of(10L, 13L), casoDeUso.estudiantesDisponibles());
    }
}
