package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

/** Datos reutilizables para las pruebas del agregado de trabajo de grado. */
final class DatosPrueba {

    static final Departamento SISTEMAS = new Departamento(1L, "Sistemas");
    static final Departamento TELEMATICA = new Departamento(2L, "Telemática");
    static final Programa ING_SISTEMAS = new Programa(1L, "Ingeniería de Sistemas");
    static final List<String> OBJETIVOS = List.of("Diseñar un sistema de gestión de trabajos de grado");
    static final String RUTA_PROPUESTA = "1/tg/propuesta.pdf";
    static final String RUTA_ANTEPROYECTO = "1/tg/anteproyecto.pdf";
    static final String RUTA_FORMATO_B = "1/tg/formato-b.pdf";
    static final String RUTA_FORMATO_C = "1/tg/formato-c.pdf";
    /** Lunes 2 de marzo de 2026 */
    static final LocalDate LUNES = LocalDate.of(2026, 3, 2);

    private DatosPrueba() {
    }

    static Estudiante estudiante(long usuarioId) {
        return new Estudiante(usuarioId, "E" + usuarioId, new NombrePersona("Estudiante", null, "Prueba", null),
                "e" + usuarioId + "@unicauca.edu.co", ING_SISTEMAS);
    }

    static Director director(long usuarioId, TipoVinculacion vinculacion, Departamento departamento) {
        return new Director(usuarioId, "D" + usuarioId, new NombrePersona("Docente", null, "Prueba", null), "d" + usuarioId + "@unicauca.edu.co",
                vinculacion, departamento);
    }

    static Director directorPlanta(long usuarioId) {
        return director(usuarioId, TipoVinculacion.PLANTA, SISTEMAS);
    }

    static Evaluador evaluador(long usuarioId) {
        return new Evaluador(usuarioId, "V" + usuarioId, new NombrePersona("Evaluador", null, "Prueba", null),
                "v" + usuarioId + "@unicauca.edu.co");
    }

    static Asesor asesorOrganizacion() {
        return new Asesor(null, "1061000000", new NombrePersona("Asesor", null, "Empresa", null), "asesor@empresa.com", "Empresa S.A.S.", true);
    }

    static Documento cartaAceptacion() {
        return Documento.inicial(TipoDocumento.CARTA_ACEPTACION, "1/tg/carta.pdf",
                LUNES);
    }

    static TrabajoInvestigacion investigacion() {
        return new TrabajoInvestigacion(null, "Sistema de gestión", OBJETIVOS, List.of(estudiante(10L)),
                directorPlanta(1L), null, null);
    }

    static Decision decisionComite(ResultadoConcepto resultado, LocalDate fecha) {
        String observaciones = resultado == ResultadoConcepto.APROBADO ? null : "Precisar los objetivos";
        return new Decision(Organo.COMITE_PROGRAMA, resultado, "Acta 01", "actas/acta-01.pdf", observaciones, fecha);
    }
}
