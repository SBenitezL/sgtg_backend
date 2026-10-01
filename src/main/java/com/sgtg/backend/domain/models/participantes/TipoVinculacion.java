package com.sgtg.backend.domain.models.participantes;

/** Forma en que un docente está vinculado con la universidad (Art. 6). */
public enum TipoVinculacion {
    PLANTA,
    /** Docente temporal. */
    OCASIONAL,
    /** Docente temporal. */
    CATEDRA,
    /** Estudiante de maestría o doctorado. */
    ESTUDIANTE_POSGRADO;

    /**
     * Los docentes temporales y los estudiantes de posgrado que dirijan un trabajo
     * de grado deben tener un codirector de planta (Art. 6).
     *
     * @return true si quien dirige con esta vinculación requiere codirector
     */
    public boolean requiereCodirector() {
        return this != PLANTA;
    }

    /**
     * @return true si la vinculación corresponde a un docente de la universidad
     *         (de planta o temporal)
     */
    public boolean esDocente() {
        return this != ESTUDIANTE_POSGRADO;
    }
}
