package com.sgtg.backend.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.comun.NombrePersona;

class UsuarioTest {

    private static Usuario usuario(TipoDocumentoIdentidad tipo, String numero, String segundoNombre) {
        return new Usuario(null, "E001", tipo, numero, "e001@unicauca.edu.co", "Ana", segundoNombre, "Pérez",
                " ", List.of(new Role(null, RoleAdmited.ESTUDIANTE)));
    }

    @Test
    void registroValidoConDocumentoNumerico() {
        Usuario usuario = usuario(TipoDocumentoIdentidad.CC, "1061234567", "María");
        assertTrue(usuario.isNumeroDocumentoValido());
        assertTrue(usuario.isValidRegister());
    }

    @ParameterizedTest
    @ValueSource(strings = { "10612A4567", "1061-234", " ", "1061 234" })
    void elNumeroDeDocumentoSoloAdmiteDigitos(String numero) {
        Usuario usuario = usuario(TipoDocumentoIdentidad.PASAPORTE, numero, null);
        assertFalse(usuario.isNumeroDocumentoValido());
        assertFalse(usuario.isValidRegister());
    }

    @Test
    void tipoDeDocumentoYPrimerNombreSonObligatorios() {
        assertFalse(usuario(null, "1061234567", null).isValidRegister());
        Usuario sinPrimerNombre = new Usuario(null, "E001", TipoDocumentoIdentidad.CC, "1061234567",
                "e001@unicauca.edu.co", " ", null, "Pérez", null,
                List.of(new Role(null, RoleAdmited.ESTUDIANTE)));
        assertFalse(sinPrimerNombre.isValidRegister());
    }

    @Test
    void segundoNombreYSegundoApellidoSonOpcionales() {
        Usuario usuario = usuario(TipoDocumentoIdentidad.CE, "123456", "");
        assertTrue(usuario.isValidRegister());
        assertNull(usuario.getSegundoNombre());
        assertNull(usuario.getSegundoApellido());
    }

    @Test
    void tipoDocumentoTieneSuDescripcion() {
        assertEquals("Cédula de Ciudadanía (CC)", TipoDocumentoIdentidad.CC.getDescripcion());
        assertEquals("Cédula de Extranjería (CE)", TipoDocumentoIdentidad.CE.getDescripcion());
        assertEquals("Pasaporte", TipoDocumentoIdentidad.PASAPORTE.getDescripcion());
    }

    @Test
    void nombrePersonaExigePrimerNombreYPrimerApellido() {
        assertThrows(ReglaNegocioException.class, () -> new NombrePersona(" ", null, "Pérez", null));
        assertThrows(ReglaNegocioException.class, () -> new NombrePersona("Ana", null, null, null));
        assertEquals("Ana María Pérez Gómez", new NombrePersona("Ana", "María", "Pérez", "Gómez").completo());
        assertEquals("Ana Pérez", new NombrePersona("Ana", " ", "Pérez", "").completo());
    }

    @Test
    void unUsuarioNuevoEstaActivoYSePuedeInactivar() {
        Usuario usuario = usuario(TipoDocumentoIdentidad.CC, "1061234567", null);
        assertTrue(usuario.isActivo());
        usuario.cambiarEstado(false);
        assertFalse(usuario.isActivo());
    }
}
