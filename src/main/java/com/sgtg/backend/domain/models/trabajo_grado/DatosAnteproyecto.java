package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.participantes.Evaluador;

/**
 * Estado persistido de la etapa de anteproyecto, usado para reconstruirla.
 *
 * @param versiones               versiones radicadas, en orden
 * @param evaluadores             evaluadores asignados, vacío si aún no se asignan
 * @param conceptos               conceptos de los evaluadores sobre todas las versiones, en orden
 * @param estado                  estado de la versión actual, null si no se ha radicado
 * @param fechaLimiteEvaluacion   fecha límite de los evaluadores para la versión actual
 * @param fechaLimiteCorrecciones fecha límite del director para reenviar con correcciones
 */
public record DatosAnteproyecto(List<Documento> versiones, List<Evaluador> evaluadores,
        List<ConceptoEvaluacion> conceptos, Estado estado, LocalDate fechaLimiteEvaluacion,
        LocalDate fechaLimiteCorrecciones) {

    public DatosAnteproyecto {
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
        evaluadores = evaluadores == null ? List.of() : List.copyOf(evaluadores);
        conceptos = conceptos == null ? List.of() : List.copyOf(conceptos);
    }

    /** Anteproyecto sin radicar. */
    public static DatosAnteproyecto vacio() {
        return new DatosAnteproyecto(List.of(), List.of(), List.of(), null, null, null);
    }
}
