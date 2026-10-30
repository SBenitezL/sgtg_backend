package com.sgtg.backend.domain.models.participantes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;

class ParticipantesTest {

    private final Departamento sistemas = new Departamento(1L, "Sistemas");
    private final Departamento telematica = new Departamento(2L, "Telemática");
    private final Programa ingSistemas = new Programa(1L, "Ingeniería de Sistemas");

    private static Usuario usuario(Long id, String codigo, RoleAdmited... roles) {
        List<Role> listaRoles = Arrays.stream(roles).map(rol -> new Role(null, rol)).toList();
        return new Usuario(id, codigo, TipoDocumentoIdentidad.CC, "10" + id, codigo.toLowerCase() + "@unicauca.edu.co", "Nombre", "Segundo", "Apellido", null, listaRoles);
    }

    private Director director(Long id, TipoVinculacion vinculacion, Departamento departamento) {
        return new Director(usuario(id, "D" + id, RoleAdmited.DOCENTE),
                new PerfilDocente(id, vinculacion, departamento));
    }

    @Test
    void seCreaDesdeElUsuarioYSuPerfilSinCopiarCredenciales() {
        Usuario usuario = usuario(10L, "E001", RoleAdmited.ESTUDIANTE);
        Estudiante estudiante = new Estudiante(usuario, new PerfilEstudiante(10L, ingSistemas));

        assertEquals(10L, estudiante.getUsuarioId());
        assertEquals("E001", estudiante.getCodigo());
        assertEquals("e001@unicauca.edu.co", estudiante.getEmail());
        assertEquals(new NombrePersona("Nombre", "Segundo", "Apellido", null), estudiante.getNombre());
        assertEquals("Nombre Segundo Apellido", estudiante.getNombreCompleto());
        assertEquals(ingSistemas, estudiante.getPrograma());
        assertTrue(estudiante.tieneUsuario());
    }

    @Test
    void validaQueElUsuarioTengaUnRolPermitido() {
        Usuario docente = usuario(1L, "D001", RoleAdmited.DOCENTE);
        Usuario estudiante = usuario(2L, "E001", RoleAdmited.ESTUDIANTE);

        assertDoesNotThrow(() -> new Director(docente, new PerfilDocente(1L, TipoVinculacion.PLANTA, sistemas)));
        assertDoesNotThrow(() -> new Evaluador(docente));
        assertThrows(ReglaNegocioException.class,
                () -> new Director(estudiante, new PerfilDocente(2L, TipoVinculacion.PLANTA, sistemas)));
        assertThrows(ReglaNegocioException.class,
                () -> new Estudiante(docente, new PerfilEstudiante(1L, ingSistemas)));
        assertThrows(ReglaNegocioException.class, () -> new Evaluador(usuario(3L, "X001")));
    }

    @Test
    void elPerfilDebePertenecerAlUsuario() {
        Usuario docente = usuario(1L, "D001", RoleAdmited.DOCENTE);
        Usuario estudiante = usuario(2L, "E001", RoleAdmited.ESTUDIANTE);

        assertThrows(ReglaNegocioException.class,
                () -> new Director(docente, new PerfilDocente(99L, TipoVinculacion.PLANTA, sistemas)));
        assertThrows(ReglaNegocioException.class, () -> new Director(docente, null));
        assertThrows(ReglaNegocioException.class,
                () -> new Estudiante(estudiante, new PerfilEstudiante(99L, ingSistemas)));
    }

    @Test
    void directorConservaLaVinculacionDelMomentoDeLaAsignacion() {
        Usuario docente = usuario(1L, "D001", RoleAdmited.DOCENTE);
        PerfilDocente perfil = new PerfilDocente(1L, TipoVinculacion.OCASIONAL, sistemas);
        Director director = new Director(docente, perfil);

        perfil.actualizarVinculacion(TipoVinculacion.PLANTA);

        assertEquals(TipoVinculacion.OCASIONAL, director.getTipoVinculacion());
        assertTrue(director.requiereCodirector());
    }

    @Test
    void codirectorDebeSerDePlantaDelDepartamento() {
        assertTrue(director(1L, TipoVinculacion.PLANTA, sistemas).esDePlantaDe(sistemas));
        assertFalse(director(2L, TipoVinculacion.PLANTA, telematica).esDePlantaDe(sistemas));
        assertFalse(director(3L, TipoVinculacion.CATEDRA, sistemas).esDePlantaDe(sistemas));
    }

    @Test
    void unaPersonaPuedeCumplirVariosRoles() {
        Usuario docente = usuario(1L, "D001", RoleAdmited.DOCENTE);
        Director director = new Director(docente, new PerfilDocente(1L, TipoVinculacion.PLANTA, sistemas));
        Evaluador evaluador = new Evaluador(docente);

        assertTrue(director.esMismaPersona(evaluador));
        assertFalse(director.esMismaPersona(new Evaluador(usuario(2L, "D002", RoleAdmited.JURADO))));
    }

    @Test
    void usuarioEsObligatorioExceptoParaElAsesor() {
        assertThrows(ReglaNegocioException.class, () -> new Evaluador((Usuario) null));
        assertThrows(ReglaNegocioException.class,
                () -> new Evaluador(null, "D001", new NombrePersona("Ana", null, "Pérez", null), "ana@unicauca.edu.co"));
        assertThrows(ReglaNegocioException.class,
                () -> new Estudiante(null, "E001", new NombrePersona("Luis", null, "Gómez", null), "luis@unicauca.edu.co", ingSistemas));

        Asesor externo = new Asesor(null, "1061000000", new NombrePersona("Luis", null, "Mora", null), "luis@empresa.com", "Empresa S.A.S.", true);
        assertFalse(externo.tieneUsuario());
    }

    @Test
    void mismaPersonaSinUsuarioSeComparaPorCodigo() {
        Asesor externo = new Asesor(null, "1061000000", new NombrePersona("Luis", null, "Mora", null), "luis@empresa.com", "Empresa S.A.S.", true);
        Asesor mismo = new Asesor(null, "1061000000", new NombrePersona("Luis", null, "Mora", null), "luis@empresa.com", "Empresa S.A.S.", true);
        assertTrue(externo.esMismaPersona(mismo));
    }

    @Test
    void datosObligatoriosDeParticipantesYPerfiles() {
        assertThrows(ReglaNegocioException.class,
                () -> new Evaluador(1L, " ", new NombrePersona("Ana", null, "Pérez", null), "ana@unicauca.edu.co"));
        assertThrows(ReglaNegocioException.class,
                () -> new Estudiante(1L, "E001", new NombrePersona("Luis", null, "Gómez", null), "luis@unicauca.edu.co", null));
        assertThrows(ReglaNegocioException.class,
                () -> new Director(1L, "D001", new NombrePersona("Ana", null, "Pérez", null), "ana@unicauca.edu.co", TipoVinculacion.PLANTA, null));
        assertThrows(ReglaNegocioException.class, () -> new PerfilDocente(1L, null, sistemas));
        assertThrows(ReglaNegocioException.class, () -> new PerfilEstudiante(null, ingSistemas));
        assertThrows(ReglaNegocioException.class, () -> new Departamento(null, "Sistemas"));
        assertThrows(ReglaNegocioException.class, () -> new Programa(1L, " "));
    }

    @Test
    void soloElDocenteDePlantaNoRequiereCodirector() {
        assertFalse(TipoVinculacion.PLANTA.requiereCodirector());
    }

    @ParameterizedTest
    @EnumSource(value = TipoVinculacion.class, names = "PLANTA", mode = EnumSource.Mode.EXCLUDE)
    void temporalesYEstudiantesDePosgradoRequierenCodirector(TipoVinculacion tipo) {
        assertTrue(tipo.requiereCodirector());
    }

    @Test
    void asesorExternoExigeOrganizacion() {
        assertThrows(ReglaNegocioException.class,
                () -> new Asesor(1L, "A001", new NombrePersona("Luis", null, "Mora", null), "luis@empresa.com", " ", true));
        assertDoesNotThrow(() -> new Asesor(usuario(2L, "A002", RoleAdmited.ASESOR), null, false));
    }
}
