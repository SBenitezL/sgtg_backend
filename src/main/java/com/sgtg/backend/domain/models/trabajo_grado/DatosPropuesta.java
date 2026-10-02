package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.comun.Decision;

/**
 * Estado persistido de la etapa de propuesta, usado para reconstruirla.
 *
 * @param versiones           versiones radicadas, en orden
 * @param decisiones          decisiones del Comité, en orden
 * @param estado              estado de la versión actual, null si no se ha radicado
 * @param fechaLimiteConcepto fecha límite del concepto de la versión actual
 */
public record DatosPropuesta(List<Documento> versiones, List<Decision> decisiones, Estado estado,
        LocalDate fechaLimiteConcepto) {

    public DatosPropuesta {
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
        decisiones = decisiones == null ? List.of() : List.copyOf(decisiones);
    }

    /** Propuesta sin radicar. */
    public static DatosPropuesta vacia() {
        return new DatosPropuesta(List.of(), List.of(), null, null);
    }
}
