package com.sgtg.backend.domain.models.trabajo_grado;

import java.util.List;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;

/**
 * Estado persistido de un trabajo de grado, usado por la infraestructura para
 * reconstruir el agregado con {@link TrabajoGrado#reconstruir}. La
 * reconstrucción no vuelve a aplicar las reglas de creación: el estado ya fue
 * validado cuando se creó y modificó el trabajo.
 *
 * Crecerá con cada fase a medida que el agregado incorpore nuevas etapas.
 *
 * @param cartaAceptacion solo para práctica profesional, null en investigación
 * @param motivoCierre    null si el trabajo sigue activo
 */
public record DatosTrabajoGrado(Long id, Modalidad modalidad, String titulo, List<String> objetivos,
        List<Estudiante> estudiantes, Director director, Director codirector, Asesor asesor,
        Documento cartaAceptacion, Proceso etapa, DatosPropuesta propuesta, List<CambioEtapa> historial,
        MotivoCierre motivoCierre) {

    public DatosTrabajoGrado {
        if (id == null || modalidad == null || etapa == null || director == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.reconstruccion_incompleta");
        objetivos = objetivos == null ? List.of() : List.copyOf(objetivos);
        estudiantes = estudiantes == null ? List.of() : List.copyOf(estudiantes);
        historial = historial == null ? List.of() : List.copyOf(historial);
        propuesta = propuesta == null ? DatosPropuesta.vacia() : propuesta;
    }
}
