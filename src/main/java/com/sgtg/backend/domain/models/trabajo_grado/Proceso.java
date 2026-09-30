package com.sgtg.backend.domain.models.trabajo_grado;

/**
 * Etapas del ciclo de vida de un trabajo de grado según la Resolución 231 de
 * 2020 del Consejo de Facultad FIET.
 *
 * <pre>
 * PROPUESTA ─► ANTEPROYECTO ─► EVALUACION ─► APROBACION_CONSEJO ─► EN_CURSO ─┬────────────► SUSTENTACION ─► FINALIZADO
 *                 ▲   ▲            │                 │                       ├─► PRORROGA ─┘     │   ▲
 *                 │   └────────────┘ (correcciones)  │                       │                   ▼   │
 *                 └──────────────────────────────────┘ (Consejo no aprueba)  │                  APLAZADO
 *                                                       SUSPENDIDO ◄─────────┘ (vuelve a la etapa previa)
 *
 * NO_APROBADO: desde EN_CURSO, PRORROGA, SUSTENTACION o APLAZADO.
 * CANCELADO: desde cualquier etapa no final.
 * </pre>
 *
 * Este enum solo define qué transiciones existen; las condiciones para
 * tomarlas (ej. que la suspensión regrese a la etapa en que se suspendió, o
 * que la segunda sustentación aplazada cierre el trabajo) las valida el
 * trabajo de grado.
 */
public enum Proceso {
    /**
     * Idea presentada al Comité de Programa (Art. 10). Permanece en esta etapa
     * con correcciones o rechazo, radicando una nueva versión, hasta que se
     * apruebe.
     */
    PROPUESTA,
    /** Radicación del anteproyecto ante el Departamento (Art. 13). */
    ANTEPROYECTO,
    /**
     * Revisión del anteproyecto por dos evaluadores (Art. 13). No se puede
     * rechazar: solo aprobar o devolver con correcciones.
     */
    EVALUACION,
    /** El anteproyecto espera la resolución del Consejo de Facultad (Art. 14). */
    APROBACION_CONSEJO,
    /** Desarrollo del trabajo, 9 meses desde la notificación (Art. 15). */
    EN_CURSO,
    /** Extensión de hasta 3 meses concedida una sola vez (Art. 16). */
    PRORROGA,
    /** Desarrollo suspendido una única vez hasta por un período (Art. 27). */
    SUSPENDIDO,
    /** Solicitud, programación y calificación de la sustentación (Art. 19-23). */
    SUSTENTACION,
    /** Primera sustentación aplazada; nueva sesión en máximo 60 días (Art. 23). */
    APLAZADO,
    /** Trabajo aprobado y versión definitiva entregada. */
    FINALIZADO,
    /** Trabajo no aprobado por vencimiento, calificación o inasistencia. */
    NO_APROBADO,
    /** Trabajo cancelado por decisión del Consejo de Facultad (Art. 26). */
    CANCELADO;

    /**
     * Indica si desde esta etapa existe una transición hacia la etapa destino.
     *
     * @param destino etapa a la que se quiere avanzar
     * @return true si la transición está permitida
     */
    public boolean puedeAvanzarA(Proceso destino) {
        if (destino == null || this.esFinal())
            return false;
        if (destino == CANCELADO)
            return true;
        return switch (this) {
            case PROPUESTA -> destino == ANTEPROYECTO;
            case ANTEPROYECTO -> destino == EVALUACION;
            case EVALUACION -> destino == ANTEPROYECTO || destino == APROBACION_CONSEJO;
            case APROBACION_CONSEJO -> destino == EN_CURSO || destino == ANTEPROYECTO;
            case EN_CURSO -> destino == PRORROGA || destino == SUSPENDIDO || destino == SUSTENTACION
                    || destino == NO_APROBADO;
            case PRORROGA -> destino == SUSPENDIDO || destino == SUSTENTACION || destino == NO_APROBADO;
            case SUSPENDIDO -> destino == EN_CURSO || destino == PRORROGA;
            case SUSTENTACION -> destino == FINALIZADO || destino == APLAZADO || destino == NO_APROBADO;
            case APLAZADO -> destino == SUSTENTACION || destino == NO_APROBADO;
            case FINALIZADO, NO_APROBADO, CANCELADO -> false;
        };
    }

    /**
     * @return true si la etapa no admite más transiciones
     */
    public boolean esFinal() {
        return this == FINALIZADO || this == NO_APROBADO || this == CANCELADO;
    }
}
