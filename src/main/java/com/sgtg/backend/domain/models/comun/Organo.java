package com.sgtg.backend.domain.models.comun;

/** Órganos colegiados que emiten decisiones sobre el trabajo de grado. */
public enum Organo {
    /** Emite concepto sobre la propuesta y avala cambios de objetivos. */
    COMITE_PROGRAMA,
    /** Asigna evaluadores y remite el anteproyecto al Consejo. */
    DEPARTAMENTO,
    /** Aprueba anteproyectos, prórrogas, suspensiones y cancelaciones. */
    CONSEJO_FACULTAD
}
