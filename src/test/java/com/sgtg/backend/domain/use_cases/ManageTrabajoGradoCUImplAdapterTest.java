package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgtg.backend.application.input.trabajo_grado.DatosAsesor;
import com.sgtg.backend.application.input.trabajo_grado.NuevoTrabajoGrado;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageTrabajoGradoGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoInvestigacion;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;
import com.sgtg.backend.infraestructure.output.exception.own.BusinessRuleException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityNotFoundException;
import com.sgtg.backend.infraestructure.output.exception.own.NoAccessException;
import com.sgtg.backend.infraestructure.output.formatter.ExceptionFormatterImplAdapter;

/** Casos de uso de la Fase 1 con los gateways simulados. */
class ManageTrabajoGradoCUImplAdapterTest {

    private static final Departamento SISTEMAS = new Departamento(1L, "Sistemas");
    private static final Programa ING_SISTEMAS = new Programa(1L, "Ingeniería de Sistemas");
    /** Lunes 2 de marzo de 2026 a las 10:00 en Bogotá */
    private static final Clock RELOJ = Clock.fixed(
            LocalDate.of(2026, 3, 2).atTime(10, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
            ZoneId.of("America/Bogota"));

    private ManageTrabajoGradoGatewayIntPort trabajoGateway;
    private ManageUserGatewayIntPort userGateway;
    private ManagePerfilGatewayIntPort perfilGateway;
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
        casoDeUso = new ManageTrabajoGradoCUImplAdapter(trabajoGateway, userGateway, perfilGateway,
                new CalendarioHabilFalso(), RELOJ, new ExceptionFormatterImplAdapter());

        // Director (1), estudiante (10), miembro del comité (50) y usuario sin relación (99)
        when(userGateway.findById(1L)).thenReturn(usuario(1L, RoleAdmited.DOCENTE));
        when(perfilGateway.buscarPerfilDocente(1L))
                .thenReturn(new PerfilDocente(1L, TipoVinculacion.PLANTA, SISTEMAS));
        when(userGateway.findById(10L)).thenReturn(usuario(10L, RoleAdmited.ESTUDIANTE));
        when(perfilGateway.buscarPerfilEstudiante(10L)).thenReturn(new PerfilEstudiante(10L, ING_SISTEMAS));
        when(userGateway.findById(50L)).thenReturn(usuario(50L, RoleAdmited.COMITE));
        when(userGateway.findById(99L)).thenReturn(usuario(99L, RoleAdmited.DOCENTE));
        // El gateway devuelve el mismo trabajo que recibe
        when(trabajoGateway.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
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
    }

    @Test
    void creaPracticaProfesionalConAsesorExternoYCarta() {
        NuevoTrabajoGrado datos = new NuevoTrabajoGrado(Modalidad.PRACTICA_PROFESIONAL, "Título",
                List.of("Objetivo"), List.of(10L), null,
                new DatosAsesor(null, "1061000000", "Asesor", null, "Externo", null, "a@empresa.com", "Empresa",
                        true),
                "https://archivos.unicauca.edu.co/carta.pdf");

        PracticaProfesional practica = assertInstanceOf(PracticaProfesional.class, casoDeUso.crear(1L, datos));
        assertEquals("Empresa", practica.getAsesor().getOrganizacion());
        assertEquals(LocalDate.of(2026, 3, 2), practica.getCartaAceptacion().fechaCarga());
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
                () -> casoDeUso.radicarPropuesta(7L, 10L, "https://archivos.unicauca.edu.co/p.pdf"));

        TrabajoGrado radicado = casoDeUso.radicarPropuesta(7L, 1L, "https://archivos.unicauca.edu.co/p.pdf");
        assertEquals(Estado.RADICADO, radicado.getPropuesta().getEstado());
        // 10 días hábiles desde el lunes 2-mar-2026 según el reloj fijo
        assertEquals(LocalDate.of(2026, 3, 16), radicado.getPropuesta().getFechaLimiteConcepto());
    }

    @Test
    void laDecisionDelComiteRegistraAlResponsableYAvanza() {
        trabajoGuardado();
        casoDeUso.radicarPropuesta(7L, 1L, "https://archivos.unicauca.edu.co/p.pdf");

        TrabajoGrado trabajo = casoDeUso.registrarDecisionComite(7L, 50L, ResultadoConcepto.APROBADO, "Acta 1",
                "actas/acta-1.pdf", null, null);

        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals("U50", trabajo.getHistorial().get(0).responsable());
        assertEquals(LocalDate.of(2026, 3, 2), trabajo.getPropuesta().getDecisiones().get(0).fecha());
        assertEquals("actas/acta-1.pdf", trabajo.getPropuesta().getDecisiones().get(0).rutaArchivo());
    }

    @Test
    void consultarExigeParticiparOTenerRolDeConsulta() {
        TrabajoGrado trabajo = trabajoGuardado();
        assertSame(trabajo, casoDeUso.consultar(7L, 10L));
        assertSame(trabajo, casoDeUso.consultar(7L, 50L));
        assertThrows(NoAccessException.class, () -> casoDeUso.consultar(7L, 99L));
    }

    @Test
    void trabajoInexistente() {
        assertThrows(EntityNotFoundException.class, () -> casoDeUso.consultar(404L, 1L));
    }
}
