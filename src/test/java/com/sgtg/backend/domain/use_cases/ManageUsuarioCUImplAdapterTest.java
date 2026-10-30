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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sgtg.backend.application.input.usuario.DatosPerfil;
import com.sgtg.backend.application.input.usuario.FichaUsuario;
import com.sgtg.backend.application.input.usuario.NuevoUsuario;
import com.sgtg.backend.application.input.usuario.UsuarioGestionado;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
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
    private ManageUsuarioCUImplAdapter casoDeUso;
    private Usuario docente;

    @BeforeEach
    void preparar() {
        userGateway = mock(ManageUserGatewayIntPort.class);
        authGateway = mock(ManageAuthGatewayIntPort.class);
        authService = mock(ManageAuthServiceGatewayIntPort.class);
        perfilGateway = mock(ManagePerfilGatewayIntPort.class);
        catalogoGateway = mock(ManageCatalogoGatewayIntPort.class);
        casoDeUso = new ManageUsuarioCUImplAdapter(userGateway, authGateway, authService, perfilGateway,
                catalogoGateway, new ExceptionFormatterImplAdapter());
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

    // ----- Contraseña y estado -----

    private String errorAlCambiar(String actual, String nueva) {
        return assertThrows(BusinessRuleException.class, () -> casoDeUso.cambiarPassword(5L, actual, nueva))
                .getBusinessRule();
    }

    @Test
    void cambiaLaContrasenaConLaActualCorrectaYDevuelveElTokenNuevo() {
        when(authService.cambiarPassword(5L, "Nueva1234*")).thenReturn("token-nuevo");
        assertEquals("token-nuevo", casoDeUso.cambiarPassword(5L, "Actual123*", "Nueva1234*"));
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
        verify(authService, never()).cambiarPassword(anyLong(), any());
    }

    @Test
    void elAdministradorActivaEInactivaUsuarios() {
        when(userGateway.findById(1L)).thenReturn(usuario(1L, RoleAdmited.ADMINISTRADOR));
        FichaUsuario inactivo = casoDeUso.cambiarEstado(1L, 5L, false);
        assertFalse(inactivo.usuario().isActivo());
        verify(userGateway).actualizarEstado(docente);

        assertTrue(casoDeUso.cambiarEstado(1L, 5L, true).usuario().isActivo());
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
}
