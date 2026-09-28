package com.sgtg.backend.domain.models;

/**
 * Etapas del ciclo de vida de un trabajo de grado.
 *
 * <pre>
 * PROPUESTA ─► ANTEPROYECTO ─► EVALUACION ─► EN_CURSO ─┬─────────────► SUSTENTACION ─► FINALIZADO
 *    ▲              ▲              │                   └─► PRORROGA ─┘   │    ▲    │
 *    │              └──────────────┘                                     ▼    │    │
 *    │         (correcciones de evaluadores)       CORRECCIONES_SUSTENTACION ┘    │
 *    └────────────────────── segunda sustentación reprobada ──────────────────────┘
 *
 * Desde cualquier etapa no final ─► ABANDONADO (vencimiento de plazos)
 * </pre>
 *
 * Solo se avanza cuando el concepto de la etapa es emitido; no existen
 * devoluciones manuales a etapas anteriores. Este enum define qué transiciones
 * existen; las condiciones para tomarlas (ej. que ambos evaluadores aprueben,
 * o que la sustentación se repruebe por segunda vez) las valida el trabajo de
 * grado.
 */
public enum Proceso {
    /** Se vencieron los plazos de alguna etapa sin cumplirla. */
    ABANDONADO,
    /**
     * Presentación de la propuesta ante el consejo. Permanece en esta etapa
     * mientras haya correcciones o si es rechazada (se presenta una nueva
     * propuesta), hasta que el consejo la apruebe.
     */
    PROPUESTA,
    /** Radicación del documento de anteproyecto ante el coordinador. */
    ANTEPROYECTO,
    /**
     * Revisión del anteproyecto por los dos evaluadores asignados. No se puede
     * rechazar: solo aprobar o devolver con correcciones.
     */
    EVALUACION,
    /** Desarrollo del trabajo, inicia con la circular de aprobación. */
    EN_CURSO,
    /** Extensión opcional del tiempo del trabajo, se puede tomar una sola vez. */
    PRORROGA,
    /** Sustentación ante los evaluadores en el salón y fecha programados. */
    SUSTENTACION,
    /** Plazo de 3 meses para corregir tras reprobar la primera sustentación. */
    CORRECCIONES_SUSTENTACION,
    /** Trabajo de grado culminado y aprobado. */
    FINALIZADO;

    /**
     * Indica si desde esta etapa existe una transición hacia la etapa destino.
     *
     * @param destino etapa a la que se quiere avanzar
     * @return true si la transición está permitida
     */
    public boolean puedeAvanzarA(Proceso destino) {
        if (destino == null || this.esFinal())
            return false;
        if (destino == ABANDONADO)
            return true;
        return switch (this) {
            case PROPUESTA -> destino == ANTEPROYECTO;
            case ANTEPROYECTO -> destino == EVALUACION;
            // Con correcciones de los evaluadores se devuelve al anteproyecto
            case EVALUACION -> destino == ANTEPROYECTO || destino == EN_CURSO;
            // Sustentación al culminar el trabajo o antes, por solicitud del director
            case EN_CURSO -> destino == PRORROGA || destino == SUSTENTACION;
            case PRORROGA -> destino == SUSTENTACION;
            // PROPUESTA solo aplica si la sustentación ya se había reprobado una vez
            case SUSTENTACION -> destino == FINALIZADO || destino == CORRECCIONES_SUSTENTACION
                    || destino == PROPUESTA;
            case CORRECCIONES_SUSTENTACION -> destino == SUSTENTACION;
            case FINALIZADO, ABANDONADO -> false;
        };
    }

    /**
     * @return true si la etapa no admite más transiciones
     */
    public boolean esFinal() {
        return this == FINALIZADO || this == ABANDONADO;
    }
}
