package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.LUNES;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.SISTEMAS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_PROPUESTA;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.asesorOrganizacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.cartaAceptacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.decisionComite;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.director;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.directorPlanta;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.estudiante;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

/** Reconstrucción del agregado desde su estado persistido. */
class ReconstruccionTrabajoGradoTest {

    private static DatosPropuesta propuestaRadicada() {
        return new DatosPropuesta(List.of(Documento.inicial(TipoDocumento.PROPUESTA, RUTA_PROPUESTA, LUNES)),
                List.of(), Estado.RADICADO, LocalDate.of(2026, 3, 16));
    }

    @Test
    void reconstruyeSegunLaModalidadConSuEstado() {
        DatosTrabajoGrado datos = new DatosTrabajoGrado(7L, Modalidad.PRACTICA_PROFESIONAL, "Título", OBJETIVOS,
                List.of(estudiante(10L)), directorPlanta(1L), null, asesorOrganizacion(), cartaAceptacion(),
                Proceso.PROPUESTA, propuestaRadicada(), null, null, List.of(), null);

        TrabajoGrado trabajo = TrabajoGrado.reconstruir(datos);

        PracticaProfesional practica = assertInstanceOf(PracticaProfesional.class, trabajo);
        assertEquals(7L, practica.getId());
        assertEquals(Proceso.PROPUESTA, practica.getEtapa());
        assertEquals(Estado.RADICADO, practica.getPropuesta().getEstado());
        assertEquals(TipoDocumento.CARTA_ACEPTACION, practica.getCartaAceptacion().tipo());
    }

    @Test
    void noVuelveAAplicarLasReglasDeCreacion() {
        // Director de cátedra sin codirector: inválido al crear, pero se acepta al reconstruir
        DatosTrabajoGrado datos = new DatosTrabajoGrado(7L, Modalidad.TRABAJO_DE_INVESTIGACION, "Título", OBJETIVOS,
                List.of(estudiante(10L)), director(1L, TipoVinculacion.CATEDRA, SISTEMAS), null, null, null,
                Proceso.PROPUESTA, null, null, null, null, null);

        TrabajoGrado trabajo = TrabajoGrado.reconstruir(datos);
        assertEquals(TipoVinculacion.CATEDRA, trabajo.getDirector().getTipoVinculacion());
    }

    @Test
    void elTrabajoReconstruidoSigueOperando() {
        DatosTrabajoGrado datos = new DatosTrabajoGrado(7L, Modalidad.TRABAJO_DE_INVESTIGACION, "Título", OBJETIVOS,
                List.of(estudiante(10L)), directorPlanta(1L), null, null, null, Proceso.PROPUESTA,
                propuestaRadicada(), null, null, List.of(), null);

        TrabajoGrado trabajo = TrabajoGrado.reconstruir(datos);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(3)), "C001",
                LUNES.plusDays(3).atStartOfDay());

        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals(1, trabajo.getHistorial().size());
    }

    @Test
    void exigeLosDatosMinimos() {
        assertThrows(ReglaNegocioException.class, () -> new DatosTrabajoGrado(null, Modalidad.PRACTICA_PROFESIONAL,
                "Título", OBJETIVOS, List.of(), directorPlanta(1L), null, null, null, Proceso.PROPUESTA, null, null,
                null, null, null));
        assertThrows(ReglaNegocioException.class, () -> new DatosTrabajoGrado(7L, null, "Título", OBJETIVOS,
                List.of(), directorPlanta(1L), null, null, null, Proceso.PROPUESTA, null, null, null, null, null));
    }
}
