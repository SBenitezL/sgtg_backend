package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.comun.Decision;

/**
 * Estado persistido de la etapa de propuesta, usado para reconstruirla.
 *
 * @param versiones           versiones radicadas, en orden, cada una con su estado
 * @param decisiones          decisiones del Comité, en orden
 * @param fechaLimiteConcepto fecha límite del concepto de la versión actual
 */
public record DatosPropuesta(List<VersionDocumento> versiones, List<Decision> decisiones,
        LocalDate fechaLimiteConcepto) {

    public DatosPropuesta {
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
        decisiones = decisiones == null ? List.of() : List.copyOf(decisiones);
    }

    /** Propuesta sin radicar. */
    public static DatosPropuesta vacia() {
        return new DatosPropuesta(List.of(), List.of(), null);
    }
}
