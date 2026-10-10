package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.output.exception.own.BadCredentialException;
import com.sgtg.backend.infraestructure.output.formatter.ExceptionFormatterImplAdapter;

/** Inicio de sesión con los gateways simulados. */
class ManageAuthCUImplAdapterTest {

    private static final String EMAIL = "d1@unicauca.edu.co";

    private ManageAuthServiceGatewayIntPort authService;
    private ManageAuthGatewayIntPort persistencia;
    private ManageAuthCUImplAdapter casoDeUso;

    private static Usuario usuario(boolean activo) {
        return new Usuario(1L, "D1", TipoDocumentoIdentidad.CC, "1061", EMAIL, "hash", "Docente", null, "Prueba",
                null, List.of(new Role(null, RoleAdmited.DOCENTE)), activo);
    }

    @BeforeEach
    void preparar() {
        authService = mock(ManageAuthServiceGatewayIntPort.class);
        persistencia = mock(ManageAuthGatewayIntPort.class);
        casoDeUso = new ManageAuthCUImplAdapter(authService, persistencia, new ExceptionFormatterImplAdapter());
        when(authService.generarToken(EMAIL)).thenReturn("token");
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
