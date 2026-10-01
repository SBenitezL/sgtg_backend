package com.sgtg.backend.domain.models.trabajo_grado;

import java.util.List;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;

/**
 * Trabajo de grado en la modalidad de Trabajo de Investigación (Capítulo II).
 * Puede realizarse de forma individual o en grupo de hasta dos estudiantes
 * (Art. 9) y puede tener un asesor, interno o externo (Art. 10).
 */
public class TrabajoInvestigacion extends TrabajoGrado {
    /** Cantidad máxima de estudiantes (Art. 9) */
    public static final int MAX_ESTUDIANTES = 2;

    /**
     * Crea un trabajo de investigación en la etapa de propuesta.
     *
     * @param codirector obligatorio si el director no es de planta, null si no aplica
     * @param asesor     opcional, null si no tiene
     */
    public TrabajoInvestigacion(Long id, String titulo, List<String> objetivos, List<Estudiante> estudiantes,
            Director director, Director codirector, Asesor asesor) {
        super(id, titulo, objetivos, estudiantes, director, codirector, asesor);
        if (estudiantes.size() > MAX_ESTUDIANTES)
            throw new ReglaNegocioException("error.dominio.investigacion.cantidad_estudiantes");
    }

    @Override
    public Modalidad getModalidad() {
        return Modalidad.TRABAJO_DE_INVESTIGACION;
    }
}
