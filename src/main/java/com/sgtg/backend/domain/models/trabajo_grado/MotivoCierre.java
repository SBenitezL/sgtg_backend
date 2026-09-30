package com.sgtg.backend.domain.models.trabajo_grado;

/**
 * Causa por la que un trabajo de grado termina sin ser aprobado. Cada motivo
 * determina la etapa final a la que pasa el trabajo.
 */
public enum MotivoCierre {
    /** No se inició la sustentación o la nueva sesión dentro del plazo (Art. 21). */
    VENCIMIENTO_PLAZO(Proceso.NO_APROBADO),
    /** Calificado NO APROBADO o aplazado por segunda vez (Art. 23). */
    SUSTENTACION_NO_APROBADA(Proceso.NO_APROBADO),
    /** No se presentó a la segunda sesión de sustentación (Art. 25). */
    INASISTENCIA_SUSTENTACION(Proceso.NO_APROBADO),
    /** Cancelación solicitada por el estudiante (Art. 26). */
    CANCELACION_ESTUDIANTE(Proceso.CANCELADO),
    /** Cancelación solicitada por el director (Art. 26). */
    CANCELACION_DIRECTOR(Proceso.CANCELADO);

    private final Proceso etapaCierre;

    MotivoCierre(Proceso etapaCierre) {
        this.etapaCierre = etapaCierre;
    }

    /**
     * @return etapa final a la que pasa el trabajo con este motivo
     */
    public Proceso getEtapaCierre() {
        return etapaCierre;
    }
}
