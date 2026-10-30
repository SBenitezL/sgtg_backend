package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.application.output.ManageIntentosAccesoGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.acceso.IntentosAcceso;
import com.sgtg.backend.infraestructure.output.exception.own.BadCredentialException;
import com.sgtg.backend.infraestructure.output.formatter.ExceptionFormatterImplAdapter;

/** Inicio de sesión con los gateways simulados. */
class ManageAuthCUImplAdapterTest {

    private static final String EMAIL = "d1@unicauca.edu.co";

    private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00Z");

    private ManageAuthServiceGatewayIntPort authService;
    private ManageAuthGatewayIntPort persistencia;
    private IntentosEnMemoria intentos;
    private ManageAuthCUImplAdapter casoDeUso;

    /** Intentos de acceso guardados en memoria */
    private static class IntentosEnMemoria implements ManageIntentosAccesoGatewayIntPort {
        private final Map<String, IntentosAcceso> registros = new HashMap<>();

        @Override
        public IntentosAcceso obtener(String correo) {
            IntentosAcceso guardado = registros.get(correo);
            return guardado == null ? IntentosAcceso.sinFallos(correo)
                    : new IntentosAcceso(correo, guardado.getFallos(), guardado.getUltimoFallo(),
                            guardado.getBloqueadoHasta());
        }

        @Override
        public void guardar(IntentosAcceso intentos) {
            registros.put(intentos.getCorreo(), intentos);
        }

        @Override
        public void eliminar(String correo) {
            registros.remove(correo);
        }

        @Override
        public int depurar(LocalDateTime antesDe) {
            return 0;
        }
    }

    private static Usuario usuario(boolean activo) {
        return new Usuario(1L, "D1", TipoDocumentoIdentidad.CC, "1061", EMAIL, "Docente", null, "Prueba",
                null, List.of(new Role(null, RoleAdmited.DOCENTE)), activo);
    }

    private ManageAuthCUImplAdapter casoDeUsoEn(Instant momento) {
        return new ManageAuthCUImplAdapter(authService, persistencia, intentos, new ExceptionFormatterImplAdapter(),
                Clock.fixed(momento, ZoneId.of("America/Bogota")));
    }

    @BeforeEach
    void preparar() {
        authService = mock(ManageAuthServiceGatewayIntPort.class);
        persistencia = mock(ManageAuthGatewayIntPort.class);
        intentos = new IntentosEnMemoria();
        casoDeUso = casoDeUsoEn(AHORA);
        when(authService.generarToken(EMAIL)).thenReturn("token");
    }

    private String mensajeDelLogin(ManageAuthCUImplAdapter caso, String email, String password) {
        return assertThrows(BadCredentialException.class, () -> caso.login(email, password)).getMessage();
    }

    @Test
    void trasCincoFallosSeguidosElCorreoQuedaBloqueadoAunqueLaContrasenaSeaCorrecta() {
        when(authService.verificarCredenciales(EMAIL, "Clave123*")).thenReturn(true);
        when(persistencia.findByEmail(EMAIL)).thenReturn(usuario(true));
        for (int i = 1; i < IntentosAcceso.MAXIMO_FALLOS; i++)
            assertEquals("error.auth.invalid_credentials", mensajeDelLogin(casoDeUso, EMAIL, "otra"));
        // El quinto fallo bloquea; con otra forma de escribir el correo sigue bloqueado
        assertEquals("error.auth.acceso_bloqueado", mensajeDelLogin(casoDeUso, EMAIL, "otra"));
        assertEquals("error.auth.acceso_bloqueado", mensajeDelLogin(casoDeUso, " D1@Unicauca.edu.co ", "Clave123*"));
        verify(authService, never()).generarToken(any());

        // Pasado el bloqueo vuelve a entrar
        assertEquals("token", casoDeUsoEn(AHORA.plus(Duration.ofMinutes(15))).login(EMAIL, "Clave123*"));
    }

    @Test
    void unCorreoInexistenteTambienSeBloquea() {
        for (int i = 1; i < IntentosAcceso.MAXIMO_FALLOS; i++)
            mensajeDelLogin(casoDeUso, "nadie@unicauca.edu.co", "Clave123*");
        assertEquals("error.auth.acceso_bloqueado", mensajeDelLogin(casoDeUso, "nadie@unicauca.edu.co", "Clave123*"));
    }

    @Test
    void unInicioDeSesionExitosoReiniciaLosFallos() {
        when(authService.verificarCredenciales(EMAIL, "Clave123*")).thenReturn(true);
        when(persistencia.findByEmail(EMAIL)).thenReturn(usuario(true));
        for (int i = 1; i < IntentosAcceso.MAXIMO_FALLOS; i++)
            mensajeDelLogin(casoDeUso, EMAIL, "otra");

        casoDeUso.login(EMAIL, "Clave123*");

        assertFalse(intentos.obtener(EMAIL).tieneRegistro());
        assertEquals("error.auth.invalid_credentials", mensajeDelLogin(casoDeUso, EMAIL, "otra"));
    }

    @Test
    void cerrarSesionCierraLaDelToken() {
        casoDeUso.logout("token");
        verify(authService).cerrarSesion("token");

        casoDeUso.logout(null);
        verify(authService, never()).cerrarSesion(null);
    }

    @Test
    void conCredencialesCorrectasDevuelveElToken() {
        when(authService.verificarCredenciales(EMAIL, "Clave123*")).thenReturn(true);
        when(persistencia.findByEmail(EMAIL)).thenReturn(usuario(true));

        assertEquals("token", casoDeUso.login(EMAIL, "Clave123*"));
    }

    @Test
    void correoInexistenteYContrasenaIncorrectaRespondenIgual() {
        // verificarCredenciales devuelve false en ambos casos
        BadCredentialException inexistente = assertThrows(BadCredentialException.class,
                () -> casoDeUso.login("nadie@unicauca.edu.co", "Clave123*"));
        BadCredentialException incorrecta = assertThrows(BadCredentialException.class,
                () -> casoDeUso.login(EMAIL, "otra"));

        assertEquals("error.auth.invalid_credentials", inexistente.getMessage());
        assertEquals(inexistente.getMessage(), incorrecta.getMessage());
        verify(authService, never()).generarToken(any());
    }

    @Test
    void sinCorreoOContrasenaNoConsultaLasCredenciales() {
        assertThrows(BadCredentialException.class, () -> casoDeUso.login(" ", "Clave123*"));
        assertThrows(BadCredentialException.class, () -> casoDeUso.login(EMAIL, null));
        verify(authService, never()).verificarCredenciales(any(), any());
    }

    @Test
    void unUsuarioInactivoNoIniciaSesionAunqueLaContrasenaSeaCorrecta() {
        when(authService.verificarCredenciales(EMAIL, "Clave123*")).thenReturn(true);
        when(persistencia.findByEmail(EMAIL)).thenReturn(usuario(false));

        BadCredentialException error = assertThrows(BadCredentialException.class,
                () -> casoDeUso.login(EMAIL, "Clave123*"));
        assertEquals("error.auth.usuario_inactivo", error.getMessage());
        verify(authService, never()).generarToken(any());
    }

    @Test
    void elEstadoInactivoSoloSeRevelaConLaContrasenaCorrecta() {
        when(persistencia.findByEmail(EMAIL)).thenReturn(usuario(false));

        BadCredentialException error = assertThrows(BadCredentialException.class,
                () -> casoDeUso.login(EMAIL, "otra"));
        assertEquals("error.auth.invalid_credentials", error.getMessage());
    }
}
