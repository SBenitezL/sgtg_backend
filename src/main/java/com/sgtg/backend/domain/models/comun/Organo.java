package com.sgtg.backend.domain.models.comun;

/** Órganos colegiados que emiten decisiones sobre el trabajo de grado. */
public enum Organo {
    /** Emite concepto sobre la propuesta y avala cambios de objetivos. */
    COMITE_PROGRAMA(true),
    /** Asigna evaluadores y remite el anteproyecto al Consejo. */
    DEPARTAMENTO(true),
    /**
     * Aprueba anteproyectos, prórrogas, suspensiones y cancelaciones. Sus
     * decisiones se soportan en la resolución adjunta, que contiene las
     * observaciones; por eso el comentario es opcional.
     */
    CONSEJO_FACULTAD(false);

    /** Indica si una decisión no aprobatoria debe incluir observaciones */
    private final boolean exigeObservacionesSiNoAprueba;

    Organo(boolean exigeObservacionesSiNoAprueba) {
        this.exigeObservacionesSiNoAprueba = exigeObservacionesSiNoAprueba;
    }

    /**
     * @return true si una decisión no aprobatoria del órgano debe incluir
     *         observaciones
     */
    public boolean exigeObservacionesSiNoAprueba() {
        return exigeObservacionesSiNoAprueba;
    }
}
