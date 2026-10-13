package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.participantes.Evaluador;

/**
 * Estado persistido de la etapa de anteproyecto, usado para reconstruirla.
 *
 * @param versiones               versiones radicadas, en orden, cada una con su estado
 * @param evaluadores             evaluadores asignados, vacío si aún no se asignan
 * @param conceptos               conceptos de los evaluadores sobre todas las versiones, en orden
 * @param decisionesConsejo       decisiones del Consejo de Facultad, en orden
 * @param remisiones              formatos C con los que el Jefe de departamento remitió al Consejo cada versión aprobada
 * @param fechaLimiteEvaluacion   fecha límite de los evaluadores para la versión actual
 * @param fechaLimiteCorrecciones fecha límite del director para reenviar con correcciones
 */
public record DatosAnteproyecto(List<VersionDocumento> versiones, List<Evaluador> evaluadores,
        List<ConceptoEvaluacion> conceptos, List<Decision> decisionesConsejo, List<Documento> remisiones,
        LocalDate fechaLimiteEvaluacion, LocalDate fechaLimiteCorrecciones) {

    public DatosAnteproyecto {
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
        evaluadores = evaluadores == null ? List.of() : List.copyOf(evaluadores);
        conceptos = conceptos == null ? List.of() : List.copyOf(conceptos);
        decisionesConsejo = decisionesConsejo == null ? List.of() : List.copyOf(decisionesConsejo);
        remisiones = remisiones == null ? List.of() : List.copyOf(remisiones);
    }

    /** Estado persistido sin remisiones al Consejo. */
    public DatosAnteproyecto(List<VersionDocumento> versiones, List<Evaluador> evaluadores,
            List<ConceptoEvaluacion> conceptos, List<Decision> decisionesConsejo,
            LocalDate fechaLimiteEvaluacion, LocalDate fechaLimiteCorrecciones) {
        this(versiones, evaluadores, conceptos, decisionesConsejo, List.of(), fechaLimiteEvaluacion,
                fechaLimiteCorrecciones);
    }

    /** Anteproyecto sin radicar. */
    public static DatosAnteproyecto vacio() {
        return new DatosAnteproyecto(List.of(), List.of(), List.of(), List.of(), List.of(), null, null);
    }
}
