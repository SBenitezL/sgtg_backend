package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.SISTEMAS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.TELEMATICA;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.asesorOrganizacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.cartaAceptacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.director;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.directorPlanta;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.estudiante;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

/** Reglas de creación del agregado (Arts. 6, 9, 10, 29, 32). */
class CreacionTrabajoGradoTest {

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    private static TrabajoInvestigacion investigacion(List<Estudiante> estudiantes, Director director,
            Director codirector) {
        return new TrabajoInvestigacion(null, "Título", OBJETIVOS, estudiantes, director, codirector, null);
    }

    @Test
    void trabajoNuevoIniciaEnPropuestaSinHistorial() {
        TrabajoInvestigacion trabajo = DatosPrueba.investigacion();

        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
        assertEquals(Modalidad.TRABAJO_DE_INVESTIGACION, trabajo.getModalidad());
        assertTrue(trabajo.getHistorial().isEmpty());
        assertNull(trabajo.getPropuesta().getEstado());
        assertNull(trabajo.getMotivoCierre());
        assertFalse(trabajo.estaCerrado());
    }

    @Test
    void investigacionAdmiteUnoODosEstudiantes() {
        assertDoesNotThrow(() -> investigacion(List.of(estudiante(10L)), directorPlanta(1L), null));
        assertDoesNotThrow(() -> investigacion(List.of(estudiante(10L), estudiante(11L)), directorPlanta(1L), null));
        assertRegla("error.dominio.investigacion.cantidad_estudiantes",
                () -> investigacion(List.of(estudiante(10L), estudiante(11L), estudiante(12L)), directorPlanta(1L),
                        null));
        assertRegla("error.dominio.trabajo_grado.estudiantes_obligatorios",
                () -> investigacion(List.of(), directorPlanta(1L), null));
    }

    @Test
    void unEstudianteNoPuedeEstarDosVeces() {
        assertRegla("error.dominio.trabajo_grado.estudiante_repetido",
                () -> investigacion(List.of(estudiante(10L), estudiante(10L)), directorPlanta(1L), null));
    }

    @Test
    void tituloObjetivosYDirectorSonObligatorios() {
        assertRegla("error.dominio.trabajo_grado.titulo_obligatorio", () -> new TrabajoInvestigacion(null, " ",
                OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L), null, null));
        assertRegla("error.dominio.trabajo_grado.objetivos_obligatorios", () -> new TrabajoInvestigacion(null,
                "Título", List.of(), List.of(estudiante(10L)), directorPlanta(1L), null, null));
        assertRegla("error.dominio.trabajo_grado.objetivos_obligatorios", () -> new TrabajoInvestigacion(null,
                "Título", List.of("Objetivo", " "), List.of(estudiante(10L)), directorPlanta(1L), null, null));
        assertRegla("error.dominio.trabajo_grado.director_obligatorio",
                () -> investigacion(List.of(estudiante(10L)), null, null));
    }

    @Test
    void directorTemporalRequiereCodirectorDePlantaDelMismoDepartamento() {
        Director catedra = director(1L, TipoVinculacion.CATEDRA, SISTEMAS);
        List<Estudiante> estudiantes = List.of(estudiante(10L));

        assertRegla("error.dominio.trabajo_grado.codirector_de_planta_obligatorio",
                () -> investigacion(estudiantes, catedra, null));
        assertRegla("error.dominio.trabajo_grado.codirector_de_planta_obligatorio",
                () -> investigacion(estudiantes, catedra, director(2L, TipoVinculacion.PLANTA, TELEMATICA)));
        assertRegla("error.dominio.trabajo_grado.codirector_de_planta_obligatorio",
                () -> investigacion(estudiantes, catedra, director(2L, TipoVinculacion.OCASIONAL, SISTEMAS)));
        assertDoesNotThrow(() -> investigacion(estudiantes, catedra, directorPlanta(2L)));
    }

    @Test
    void estudianteDePosgradoComoDirectorRequiereCodirector() {
        Director posgrado = director(1L, TipoVinculacion.ESTUDIANTE_POSGRADO, SISTEMAS);
        assertRegla("error.dominio.trabajo_grado.codirector_de_planta_obligatorio",
                () -> investigacion(List.of(estudiante(10L)), posgrado, null));
    }

    @Test
    void directorDePlantaPuedeTenerCodirectorOpcional() {
        assertDoesNotThrow(() -> investigacion(List.of(estudiante(10L)), directorPlanta(1L), null));
        assertDoesNotThrow(() -> investigacion(List.of(estudiante(10L)), directorPlanta(1L),
                director(2L, TipoVinculacion.CATEDRA, TELEMATICA)));
    }

    @Test
    void codirectorSoloPuedeSerDocenteDeLaUniversidad() {
        assertRegla("error.dominio.trabajo_grado.codirector_debe_ser_docente",
                () -> investigacion(List.of(estudiante(10L)), directorPlanta(1L),
                        director(2L, TipoVinculacion.ESTUDIANTE_POSGRADO, SISTEMAS)));
    }

    @Test
    void codirectorYEstudiantesDebenSerPersonasDistintasAlDirector() {
        assertRegla("error.dominio.trabajo_grado.codirector_igual_director",
                () -> investigacion(List.of(estudiante(10L)), directorPlanta(1L), directorPlanta(1L)));
        // El usuario 1 figura como director y como estudiante
        assertRegla("error.dominio.trabajo_grado.director_es_estudiante",
                () -> investigacion(List.of(estudiante(1L)), directorPlanta(1L), null));
    }

    @Test
    void practicaProfesionalEsIndividualConAsesorYCarta() {
        PracticaProfesional practica = new PracticaProfesional(null, "Título", OBJETIVOS, estudiante(10L),
                directorPlanta(1L), null, asesorOrganizacion(), cartaAceptacion());

        assertEquals(Modalidad.PRACTICA_PROFESIONAL, practica.getModalidad());
        assertEquals(1, practica.getEstudiantes().size());
        assertEquals(10L, practica.getEstudiante().getUsuarioId());
    }

    @Test
    void practicaProfesionalRechazaDatosFaltantes() {
        assertRegla("error.dominio.trabajo_grado.estudiantes_obligatorios",
                () -> new PracticaProfesional(null, "Título", OBJETIVOS, null, directorPlanta(1L), null,
                        asesorOrganizacion(), cartaAceptacion()));
        assertRegla("error.dominio.practica.asesor_obligatorio",
                () -> new PracticaProfesional(null, "Título", OBJETIVOS, estudiante(10L), directorPlanta(1L), null,
                        null, cartaAceptacion()));
        assertRegla("error.dominio.practica.carta_aceptacion_obligatoria",
                () -> new PracticaProfesional(null, "Título", OBJETIVOS, estudiante(10L), directorPlanta(1L), null,
                        asesorOrganizacion(), null));
        Documento otroDocumento = Documento.inicial(TipoDocumento.PROPUESTA,
                "https://archivos.unicauca.edu.co/tg/propuesta.pdf", DatosPrueba.LUNES);
        assertRegla("error.dominio.practica.carta_aceptacion_obligatoria",
                () -> new PracticaProfesional(null, "Título", OBJETIVOS, estudiante(10L), directorPlanta(1L), null,
                        asesorOrganizacion(), otroDocumento));
    }

    @Test
    void identificaAlDirectorYALosParticipantes() {
        TrabajoInvestigacion trabajo = new TrabajoInvestigacion(null, "Título", OBJETIVOS,
                List.of(estudiante(10L), estudiante(11L)), director(1L, TipoVinculacion.CATEDRA, SISTEMAS),
                directorPlanta(2L), null);

        assertTrue(trabajo.esDirector(1L));
        assertFalse(trabajo.esDirector(2L));
        assertTrue(trabajo.participa(1L));
        assertTrue(trabajo.participa(2L));
        assertTrue(trabajo.participa(11L));
        assertFalse(trabajo.participa(99L));
        assertFalse(trabajo.participa(null));
    }

    @Test
    void lasListasExpuestasNoSePuedenModificar() {
        TrabajoInvestigacion trabajo = DatosPrueba.investigacion();
        assertThrows(UnsupportedOperationException.class, () -> trabajo.getEstudiantes().clear());
        assertThrows(UnsupportedOperationException.class, () -> trabajo.getObjetivos().clear());
        assertThrows(UnsupportedOperationException.class, () -> trabajo.getHistorial().clear());
    }
}
