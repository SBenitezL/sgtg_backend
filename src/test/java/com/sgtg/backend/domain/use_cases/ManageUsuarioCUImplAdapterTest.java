package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sgtg.backend.application.input.usuario.CambioComite;
import com.sgtg.backend.application.input.usuario.DatosPerfil;
import com.sgtg.backend.application.input.usuario.FichaUsuario;
import com.sgtg.backend.application.input.usuario.NuevoUsuario;
import com.sgtg.backend.application.input.usuario.UsuarioGestionado;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.application.output.ManageTrabajoGradoGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.infraestructure.output.exception.own.BusinessRuleException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityExistsException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityNotFoundException;
import com.sgtg.backend.infraestructure.output.exception.own.NoAccessException;
import com.sgtg.backend.infraestructure.output.formatter.ExceptionFormatterImplAdapter;

/** Administración de usuarios con sus perfiles, con los gateways simulados. */
class ManageUsuarioCUImplAdapterTest {

    private static final Departamento SISTEMAS = new Departamento(1L, "Sistemas");
    private static final Programa ING_SISTEMAS = new Programa(1L, "Ingeniería de Sistemas");

    private ManageUserGatewayIntPort userGateway;
    private ManageAuthGatewayIntPort authGateway;
    private ManageAuthServiceGatewayIntPort authService;
    private ManagePerfilGatewayIntPort perfilGateway;
    private ManageCatalogoGatewayIntPort catalogoGateway;
    private ManageTrabajoGradoGatewayIntPort trabajoGateway;
    private ManageNotificacionGatewayIntPort notificacionGateway;
    private ManageUsuarioCUImplAdapter casoDeUso;
    private Usuario docente;

    @BeforeEach
    void preparar() {
        userGateway = mock(ManageUserGatewayIntPort.class);
        authGateway = mock(ManageAuthGatewayIntPort.class);
        authService = mock(ManageAuthServiceGatewayIntPort.class);
        perfilGateway = mock(ManagePerfilGatewayIntPort.class);
        catalogoGateway = mock(ManageCatalogoGatewayIntPort.class);
        trabajoGateway = mock(ManageTrabajoGradoGatewayIntPort.class);
        notificacionGateway = mock(ManageNotificacionGatewayIntPort.class);
        casoDeUso = new ManageUsuarioCUImplAdapter(userGateway, authGateway, authService, perfilGateway,
                catalogoGateway, trabajoGateway, notificacionGateway, new ExceptionFormatterImplAdapter(),
                Clock.fixed(Instant.parse("2026-11-01T15:00:00Z"), ZoneId.of("America/Bogota")));
        docente = new Usuario(5L, "D5", TipoDocumentoIdentidad.CC, "1065", "d5@unicauca.edu.co", "Docente",
                null, "Prueba", null, List.of(new Role(1L, RoleAdmited.DOCENTE)));
        when(userGateway.findById(5L)).thenReturn(docente);
        when(authService.verificarCredenciales("d5@unicauca.edu.co", "Actual123*")).thenReturn(true);
        // Los roles pedidos existen con su identificador
        when(authGateway.findRolesByTrole(any())).thenAnswer(invocacion -> {
            List<String> nombres = invocacion.getArgument(0);
            return nombres.stream().map(nombre -> new Role(1L, RoleAdmited.valueOf(nombre))).toList();
        });
        when(catalogoGateway.buscarDepartamento(1L)).thenReturn(SISTEMAS);
        when(catalogoGateway.buscarPrograma(1L)).thenReturn(ING_SISTEMAS);
        when(perfilGateway.guardarPerfilDocente(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(perfilGateway.guardarPerfilEstudiante(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        // Los listados piden todos los perfiles de una vez: se arman con los perfiles de cada usuario de la prueba
        when(perfilGateway.listarPerfilesDocentes()).thenAnswer(invocacion -> perfiles(perfilGateway::buscarPerfilDocente));
        when(perfilGateway.listarPerfilesEstudiantes())
                .thenAnswer(invocacion -> perfiles(perfilGateway::buscarPerfilEstudiante));
    }

    /** Perfiles de los usuarios de {@code userGateway.listar()} que tienen uno, por identificador. */
    private <T> java.util.Map<Long, T> perfiles(java.util.function.Function<Long, T> buscar) {
        java.util.Map<Long, T> perfiles = new java.util.HashMap<>();
        userGateway.listar().forEach(usuario -> {
            T perfil = buscar.apply(usuario.getCusuari());
            if (perfil != null)
                perfiles.put(usuario.getCusuari(), perfil);
        });
        return perfiles;
    }

    private static NuevoUsuario nuevo(List<String> roles, DatosPerfil perfil) {
        return new NuevoUsuario("D9", TipoDocumentoIdentidad.CC, "1069", "d9@unicauca.edu.co", "Clave1234*", "Ana",
                null, "Pérez", null, roles, perfil);
    }

    /** El usuario recién guardado, con identificador 9. */
    private void alGuardarQuedaConId9(List<String> roles) {
        when(userGateway.findByEmail("d9@unicauca.edu.co")).thenReturn(new Usuario(9L, "D9",
                TipoDocumentoIdentidad.CC, "1069", "d9@unicauca.edu.co", "Ana", null, "Pérez", null,
                roles.stream().map(rol -> new Role(1L, RoleAdmited.valueOf(rol))).toList()));
    }

    // ----- Crear -----

    @Test
    void creaUnDirectorConSuPerfilDocente() {
        alGuardarQuedaConId9(List.of("DOCENTE", "DIRECTOR"));
        ArgumentCaptor<PerfilDocente> perfil = ArgumentCaptor.forClass(PerfilDocente.class);

        FichaUsuario ficha = casoDeUso.crear(nuevo(List.of("DOCENTE", "DIRECTOR"),
                new DatosPerfil(TipoVinculacion.OCASIONAL, 1L, null)));

        verify(authService).register(any(), eq("Clave1234*"));
        verify(perfilGateway).guardarPerfilDocente(perfil.capture());
        assertEquals(9L, perfil.getValue().getUsuarioId());
        assertEquals(TipoVinculacion.OCASIONAL, perfil.getValue().getTipoVinculacion());
        assertEquals(SISTEMAS, perfil.getValue().getDepartamento());
        assertEquals(9L, ficha.usuario().getCusuari());
    }

    @Test
    void creaUnEstudianteConSuPrograma() {
        alGuardarQuedaConId9(List.of("ESTUDIANTE"));
        ArgumentCaptor<PerfilEstudiante> perfil = ArgumentCaptor.forClass(PerfilEstudiante.class);

        casoDeUso.crear(nuevo(List.of("ESTUDIANTE"), new DatosPerfil(null, null, 1L)));

        verify(perfilGateway).guardarPerfilEstudiante(perfil.capture());
        assertEquals(ING_SISTEMAS, perfil.getValue().getPrograma());
        verify(perfilGateway, never()).guardarPerfilDocente(any());
    }

    @Test
    void sinPerfilSoloCreaElUsuario() {
        alGuardarQuedaConId9(List.of("COMITE"));
        FichaUsuario ficha = casoDeUso.crear(nuevo(List.of("COMITE"), null));

        verify(perfilGateway, never()).guardarPerfilDocente(any());
        verify(perfilGateway, never()).guardarPerfilEstudiante(any());
        assertNull(ficha.perfilDocente());
    }

    @Test
    void elPerfilInvalidoSeRechazaAntesDeCrearElUsuario() {
        // Vinculación sin departamento
        assertEquals("error.usuario.perfil_docente_incompleto", assertThrows(BusinessRuleException.class,
                () -> casoDeUso.crear(nuevo(List.of("DIRECTOR"), new DatosPerfil(TipoVinculacion.PLANTA, null, null))))
                .getBusinessRule());
        // Perfil docente para quien no tiene rol docente
        assertEquals("error.perfil.rol_no_corresponde", assertThrows(BusinessRuleException.class,
                () -> casoDeUso.crear(nuevo(List.of("ESTUDIANTE"), new DatosPerfil(TipoVinculacion.PLANTA, 1L, null))))
                .getBusinessRule());
        // Programa para quien no es estudiante
        assertEquals("error.perfil.rol_no_corresponde", assertThrows(BusinessRuleException.class,
                () -> casoDeUso.crear(nuevo(List.of("DOCENTE"), new DatosPerfil(null, null, 1L)))).getBusinessRule());
        // Departamento inexistente
        assertThrows(EntityNotFoundException.class, () -> casoDeUso
                .crear(nuevo(List.of("DIRECTOR"), new DatosPerfil(TipoVinculacion.PLANTA, 99L, null))));

        verify(authService, never()).register(any(), any());
    }

    @Test
    void laContrasenaDelUsuarioNuevoCumpleLaPolitica() {
        NuevoUsuario corta = new NuevoUsuario("D9", TipoDocumentoIdentidad.CC, "1069", "d9@unicauca.edu.co",
                "corta", "Ana", null, "Pérez", null, List.of("DOCENTE"), null);
        assertEquals("error.usuario.password_invalida",
                assertThrows(BusinessRuleException.class, () -> casoDeUso.crear(corta)).getBusinessRule());
        NuevoUsuario predecible = new NuevoUsuario("D9", TipoDocumentoIdentidad.CC, "1069", "d9@unicauca.edu.co",
                "Unicauca2026*", "Ana", null, "Pérez", null, List.of("DOCENTE"), null);
        assertEquals("error.usuario.password_predecible",
                assertThrows(BusinessRuleException.class, () -> casoDeUso.crear(predecible)).getBusinessRule());
        verify(authService, never()).register(any(), any());
    }

    // ----- Actualizar y listar -----

    @Test
    void actualizaDatosRolesYPerfilConservandoLaContrasena() {
        NuevoUsuario datos = new NuevoUsuario("D5", TipoDocumentoIdentidad.CC, "1065", "nuevo@unicauca.edu.co",
                " ", "Docente", null, "Prueba", null, List.of("DOCENTE", "CODIRECTOR"),
                new DatosPerfil(TipoVinculacion.PLANTA, 1L, null));

        FichaUsuario ficha = casoDeUso.actualizar(5L, datos);

        assertEquals("nuevo@unicauca.edu.co", ficha.usuario().getEmail());
        assertEquals(List.of("DOCENTE", "CODIRECTOR"),
                ficha.usuario().getRole().stream().map(Role::getTrole).toList());
        verify(userGateway).actualizarDatos(docente);
        verify(authService, never()).asignarPassword(anyLong(), anyString());
        verify(perfilGateway).guardarPerfilDocente(any());
    }

    @Test
    void alActualizarCambiaLaContrasenaSiSeIndica() {
        NuevoUsuario datos = new NuevoUsuario("D5", TipoDocumentoIdentidad.CC, "1065", "d5@unicauca.edu.co",
                "NuevaClave1*", "Docente", null, "Prueba", null, List.of("DOCENTE"), null);
        casoDeUso.actualizar(5L, datos);
        verify(authService).asignarPassword(5L, "NuevaClave1*");
    }

    @Test
    void alActualizarNoSeRepiteElCorreoDeOtroUsuario() {
        when(userGateway.existeEmailOCodigoEnOtro("otro@unicauca.edu.co", "D5", 5L)).thenReturn(true);
        NuevoUsuario datos = new NuevoUsuario("D5", TipoDocumentoIdentidad.CC, "1065", "otro@unicauca.edu.co",
                null, "Docente", null, "Prueba", null, List.of("DOCENTE"), null);

        assertThrows(EntityExistsException.class, () -> casoDeUso.actualizar(5L, datos));
        verify(userGateway, never()).actualizarDatos(any());
    }

    @Test
    void listaLosUsuariosConSusPerfiles() {
        when(userGateway.listar()).thenReturn(List.of(docente));
        PerfilDocente perfil = new PerfilDocente(5L, TipoVinculacion.PLANTA, SISTEMAS);
        when(perfilGateway.buscarPerfilDocente(5L)).thenReturn(perfil);

        List<FichaUsuario> fichas = casoDeUso.listar();

        assertEquals(1, fichas.size());
        assertEquals(perfil, fichas.get(0).perfilDocente());
    }

    @Test
    void elListadoTraeLosPerfilesEnUnaConsultaYNoUnaPorUsuario() {
        PerfilDocente perfil = new PerfilDocente(5L, TipoVinculacion.PLANTA, SISTEMAS);
        Usuario estudiante = usuario(12L, RoleAdmited.ESTUDIANTE);
        PerfilEstudiante programa = new PerfilEstudiante(12L, ING_SISTEMAS);
        when(userGateway.listar()).thenReturn(List.of(docente, estudiante));
        org.mockito.Mockito.doReturn(java.util.Map.of(5L, perfil)).when(perfilGateway).listarPerfilesDocentes();
        org.mockito.Mockito.doReturn(java.util.Map.of(12L, programa)).when(perfilGateway).listarPerfilesEstudiantes();

        List<FichaUsuario> fichas = casoDeUso.listar();

        assertEquals(perfil, fichas.get(0).perfilDocente());
        assertNull(fichas.get(0).perfilEstudiante());
        assertEquals(programa, fichas.get(1).perfilEstudiante());
        verify(perfilGateway, never()).buscarPerfilDocente(anyLong());
        verify(perfilGateway, never()).buscarPerfilEstudiante(anyLong());
    }

    // ----- Contraseña y estado -----

    private String errorAlCambiar(String actual, String nueva) {
        return assertThrows(BusinessRuleException.class, () -> casoDeUso.cambiarPassword(5L, actual, nueva, "sesion"))
                .getBusinessRule();
    }

    @Test
    void cambiaLaContrasenaConLaActualCorrectaYDevuelveElTokenNuevo() {
        when(authService.cambiarPassword(5L, "Nueva1234*", "sesion")).thenReturn("token-nuevo");
        assertEquals("token-nuevo", casoDeUso.cambiarPassword(5L, "Actual123*", "Nueva1234*", "sesion"));
    }

    @Test
    void reglasDelCambioDeContrasena() {
        when(authService.esPasswordReciente(5L, "Anterior1*")).thenReturn(true);
        assertEquals("error.usuario.password_actual_incorrecta", errorAlCambiar("Otra1234*", "Nueva1234*"));
        assertEquals("error.usuario.password_actual_incorrecta", errorAlCambiar(" ", "Nueva1234*"));
        assertEquals("error.usuario.password_invalida", errorAlCambiar("Actual123*", "corta"));
        assertEquals("error.usuario.password_invalida", errorAlCambiar("Actual123*", "sinmayuscula1*"));
        // Con el número de documento del usuario
        assertEquals("error.usuario.password_predecible", errorAlCambiar("Actual123*", "Clave1065*"));
        assertEquals("error.usuario.password_igual", errorAlCambiar("Actual123*", "Actual123*"));
        assertEquals("error.usuario.password_reciente", errorAlCambiar("Actual123*", "Anterior1*"));
        verify(authService, never()).cambiarPassword(anyLong(), any(), any());
    }

    @Test
    void elAdministradorActivaEInactivaUsuarios() {
        when(userGateway.findById(1L)).thenReturn(usuario(1L, RoleAdmited.ADMINISTRADOR));
        FichaUsuario inactivo = casoDeUso.cambiarEstado(1L, 5L, false);
        assertFalse(inactivo.usuario().isActivo());
        verify(userGateway).actualizarEstado(docente);
        // Sus sesiones abiertas se cierran
        verify(authService).cerrarSesionesDe(5L);

        assertTrue(casoDeUso.cambiarEstado(1L, 5L, true).usuario().isActivo());
        verify(authService, times(1)).cerrarSesionesDe(5L);
    }

    @Test
    void unAdministradorNoSeInactivaASiMismo() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.cambiarEstado(5L, 5L, false));
        assertEquals("error.usuario.no_puede_inactivarse", error.getBusinessRule());
        verify(userGateway, never()).actualizarEstado(any());
    }

    // ----- Gestores: Comité, jefe de departamento, Consejo y Decanatura -----

    private static final Departamento TELEMATICA = new Departamento(2L, "Telemática");

    private static Usuario usuario(long id, RoleAdmited... roles) {
        return new Usuario(id, "U" + id, TipoDocumentoIdentidad.CC, "10" + id, "u" + id + "@unicauca.edu.co",
                "Usuario", null, "Prueba", null, java.util.Arrays.stream(roles).map(rol -> new Role(1L, rol)).toList());
    }

    /**
     * Jefe de Sistemas (60), Consejo (70), docente de Sistemas (5), estudiante de
     * Ingeniería de Sistemas (12), estudiante de Telemática (11) y miembro del Comité de Sistemas (50).
     */
    private void prepararFacultad() {
        when(userGateway.findById(60L)).thenReturn(usuario(60L, RoleAdmited.JEFE_DEP, RoleAdmited.DOCENTE));
        when(perfilGateway.buscarPerfilDocente(60L)).thenReturn(new PerfilDocente(60L, TipoVinculacion.PLANTA, SISTEMAS));
        when(userGateway.findById(70L)).thenReturn(usuario(70L, RoleAdmited.CONSEJO));
        when(perfilGateway.buscarPerfilDocente(5L)).thenReturn(new PerfilDocente(5L, TipoVinculacion.PLANTA, SISTEMAS));
        Usuario estudianteTelematica = usuario(11L, RoleAdmited.ESTUDIANTE);
        when(userGateway.findById(11L)).thenReturn(estudianteTelematica);
        when(perfilGateway.buscarPerfilEstudiante(11L)).thenReturn(new PerfilEstudiante(11L, new Programa(2L, "Telemática")));
        Usuario estudianteSistemas = usuario(12L, RoleAdmited.ESTUDIANTE);
        when(userGateway.findById(12L)).thenReturn(estudianteSistemas);
        when(perfilGateway.buscarPerfilEstudiante(12L)).thenReturn(new PerfilEstudiante(12L, ING_SISTEMAS));
        Usuario comite = usuario(50L, RoleAdmited.COMITE, RoleAdmited.DOCENTE);
        when(userGateway.findById(50L)).thenReturn(comite);
        when(perfilGateway.buscarPerfilDocente(50L)).thenReturn(new PerfilDocente(50L, TipoVinculacion.PLANTA, SISTEMAS));
        when(userGateway.listar()).thenReturn(List.of(docente, estudianteTelematica, estudianteSistemas, comite));
        when(catalogoGateway.listarProgramas()).thenReturn(List.of(
                new Programa(1L, "Ingeniería de Sistemas", SISTEMAS), new Programa(2L, "Telemática", TELEMATICA)));
        when(catalogoGateway.buscarDepartamento(2L)).thenReturn(TELEMATICA);
    }

    private static String reglaSinAcceso(org.junit.jupiter.api.function.Executable accion) {
        return assertThrows(NoAccessException.class, accion).getMessage();
    }

    @Test
    void elJefeGestionaLosDocentesDeSuDepartamentoYLosEstudiantesDeSusProgramas() {
        prepararFacultad();

        List<UsuarioGestionado> gestionados = casoDeUso.listarGestionados(60L);

        assertEquals(List.of(5L, 12L, 50L), gestionados.stream().map(g -> g.ficha().usuario().getCusuari()).toList());
        // Al miembro del Comité solo lo modifica el administrador
        assertEquals(List.of(true, true, false), gestionados.stream().map(UsuarioGestionado::modificable).toList());
    }

    @Test
    void elConsejoYDecanaturaGestionanATodaLaFacultad() {
        prepararFacultad();

        assertEquals(4, casoDeUso.listarGestionados(70L).size());
        assertEquals(11L, casoDeUso.consultarFicha(70L, 11L).usuario().getCusuari());
    }

    @Test
    void elJefeNoModificaUsuariosAdministrativosNiFueraDeSuAlcance() {
        prepararFacultad();

        assertEquals("error.usuario.usuario_administrativo", reglaSinAcceso(() -> casoDeUso.cambiarEstado(60L, 50L, false)));
        assertEquals("error.usuario.fuera_de_alcance", reglaSinAcceso(() -> casoDeUso.cambiarEstado(60L, 11L, false)));
        assertEquals("error.usuario.fuera_de_alcance", reglaSinAcceso(() -> casoDeUso.consultarFicha(60L, 11L)));
        verify(userGateway, never()).actualizarEstado(any());

        assertFalse(casoDeUso.cambiarEstado(60L, 5L, false).usuario().isActivo());
    }

    @Test
    void unGestorSoloAsignaOQuitaLosRolesEstudianteYDocente() {
        prepararFacultad();

        assertEquals("error.usuario.rol_no_asignable",
                reglaSinAcceso(() -> casoDeUso.asignarRoles(70L, 5L, List.of("DOCENTE", "COMITE"))));
        assertEquals("error.usuario.rol_no_asignable",
                reglaSinAcceso(() -> casoDeUso.asignarRoles(60L, 12L, List.of("ESTUDIANTE", "ADMINISTRADOR"))));
        verify(userGateway, never()).actualizarRoles(any());

        casoDeUso.asignarRoles(60L, 5L, List.of("DOCENTE", "ESTUDIANTE"));
        verify(userGateway).actualizarRoles(docente);
    }

    @Test
    void elJefeCreaUsuariosSoloConRolesBaseYPerfilDeSuAlcance() {
        prepararFacultad();

        assertEquals("error.usuario.rol_no_asignable", reglaSinAcceso(() -> casoDeUso.crear(60L,
                nuevo(List.of("DOCENTE", "JEFE_DEP"), new DatosPerfil(TipoVinculacion.PLANTA, 1L, null)))));
        // Docente de otro departamento
        assertEquals("error.usuario.fuera_de_alcance", reglaSinAcceso(() -> casoDeUso.crear(60L,
                nuevo(List.of("DOCENTE"), new DatosPerfil(TipoVinculacion.PLANTA, 2L, null)))));
        verify(authService, never()).register(any(), any());

        alGuardarQuedaConId9(List.of("ESTUDIANTE"));
        assertEquals(9L, casoDeUso.crear(60L, nuevo(List.of("ESTUDIANTE"), new DatosPerfil(null, null, 1L)))
                .usuario().getCusuari());
    }

    @Test
    void unUsuarioSinRolAdministrativoNoGestionaUsuarios() {
        assertEquals("error.usuario.sin_permiso_gestion", reglaSinAcceso(() -> casoDeUso.listarGestionados(5L)));
    }

    // ----- Director, asesor y Comité -----

    private static java.util.Set<String> roles(Usuario usuario) {
        return usuario.getRole().stream().map(Role::getTrole).collect(java.util.stream.Collectors.toSet());
    }

    /** Trabajo en curso (o cerrado) con el docente 5 en el puesto indicado y estudiantes de Ingeniería de Sistemas */
    private TrabajoGrado trabajoCon(String puesto, boolean cerrado) {
        TrabajoGrado trabajo = mock(TrabajoGrado.class);
        Director participante = mock(Director.class);
        when(participante.getUsuarioId()).thenReturn(5L);
        when(trabajo.estaCerrado()).thenReturn(cerrado);
        if ("DIRECTOR".equals(puesto))
            when(trabajo.getDirector()).thenReturn(participante);
        if ("CODIRECTOR".equals(puesto))
            when(trabajo.getCodirector()).thenReturn(participante);
        Estudiante estudiante = mock(Estudiante.class);
        when(estudiante.getPrograma()).thenReturn(ING_SISTEMAS);
        when(trabajo.getEstudiantes()).thenReturn(List.of(estudiante));
        when(catalogoGateway.buscarPrograma(1L)).thenReturn(new Programa(1L, "Ingeniería de Sistemas", SISTEMAS));
        return trabajo;
    }

    @Test
    void unGestorTambienAsignaLosRolesDirectorYAsesor() {
        prepararFacultad();

        casoDeUso.asignarRoles(60L, 5L, List.of("DOCENTE", "DIRECTOR", "ASESOR"));

        assertEquals(java.util.Set.of("DOCENTE", "DIRECTOR", "ASESOR"), roles(docente));
        assertEquals("error.usuario.rol_no_asignable",
                reglaSinAcceso(() -> casoDeUso.asignarRoles(60L, 5L, List.of("DOCENTE", "CODIRECTOR"))));
    }

    @Test
    void noSeQuitaElRolDeDirectorAQuienDirigeUnTrabajoEnCurso() {
        docente.updateRole(List.of(new Role(1L, RoleAdmited.DOCENTE), new Role(1L, RoleAdmited.DIRECTOR)));
        TrabajoGrado enCurso = trabajoCon("DIRECTOR", false);
        when(trabajoGateway.listarPorParticipante(5L)).thenReturn(List.of(enCurso));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.asignarRoles(5L, List.of("DOCENTE")));
        assertEquals("error.usuario.rol_de_trabajo_en_uso", error.getBusinessRule());

        // Si el trabajo ya se cerró, sí se puede quitar
        TrabajoGrado cerrado = trabajoCon("DIRECTOR", true);
        when(trabajoGateway.listarPorParticipante(5L)).thenReturn(List.of(cerrado));
        casoDeUso.asignarRoles(5L, List.of("DOCENTE"));
        assertEquals(java.util.Set.of("DOCENTE"), roles(docente));
    }

    @Test
    void unDepartamentoTieneUnSoloComite() {
        prepararFacultad();

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> casoDeUso.asignarRoles(5L, List.of("DOCENTE", "COMITE")));
        assertEquals("error.usuario.comite_existente", error.getBusinessRule());
    }

    @Test
    void elComiteCedeSuRolAUnDocenteDeSuDepartamento() {
        prepararFacultad();
        Usuario comite = userGateway.findById(50L);

        CambioComite cambio = casoDeUso.cambiarComite(50L, 5L);

        assertEquals(5L, cambio.comite().usuario().getCusuari());
        assertEquals(List.of(50L), cambio.anteriores());
        assertEquals(SISTEMAS, cambio.departamento());
        assertEquals(java.util.Set.of("DOCENTE", "COMITE"), roles(docente));
        // Quien lo cede sigue como docente y se le cierran las sesiones
        assertEquals(java.util.Set.of("DOCENTE"), roles(comite));
        verify(authService).cerrarSesionesDe(50L);
        verify(notificacionGateway).guardarTodas(org.mockito.ArgumentMatchers.argThat(avisos -> avisos.size() == 2));
    }

    @Test
    void elComiteNoPasaAOtroDepartamentoNiAQuienCodirigeUnTrabajoDelPrograma() {
        prepararFacultad();
        Usuario docenteTelematica = usuario(13L, RoleAdmited.DOCENTE);
        when(userGateway.findById(13L)).thenReturn(docenteTelematica);
        when(perfilGateway.buscarPerfilDocente(13L)).thenReturn(new PerfilDocente(13L, TipoVinculacion.PLANTA, TELEMATICA));

        // El jefe de Sistemas no lo pasa a un docente de Telemática
        assertEquals("error.usuario.comite_otro_departamento", reglaSinAcceso(() -> casoDeUso.cambiarComite(60L, 13L)));
        // El Consejo sí cambia el de cualquier departamento
        assertEquals(13L, casoDeUso.cambiarComite(70L, 13L).comite().usuario().getCusuari());

        // Quien codirige un trabajo en curso de Sistemas no puede ser su Comité (Art. 11)
        TrabajoGrado codirigido = trabajoCon("CODIRECTOR", false);
        when(trabajoGateway.listarPorParticipante(5L)).thenReturn(List.of(codirigido));
        BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> casoDeUso.cambiarComite(60L, 5L));
        assertEquals("error.usuario.comite_codirector", error.getBusinessRule());
    }

    @Test
    void unGestorDelDepartamentoCreaYGestionaAsesoresExternosSinPerfil() {
        prepararFacultad();
        when(userGateway.findByEmail("d9@unicauca.edu.co")).thenReturn(usuario(9L, RoleAdmited.ASESOR));

        casoDeUso.crear(60L, nuevo(List.of("ASESOR"), null));

        verify(authService).register(any(), any());
        Usuario asesor = usuario(14L, RoleAdmited.ASESOR);
        when(userGateway.listar()).thenReturn(List.of(docente, asesor));
        assertEquals(List.of(5L, 14L), casoDeUso.listarGestionados(60L).stream()
                .map(gestionado -> gestionado.ficha().usuario().getCusuari()).toList());
    }
}
