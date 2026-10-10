package com.sgtg.backend.domain.models.trabajo_grado;

/**
 * Calificación de la sustentación que emite cada jurado (Arts. 22-23). El
 * orden de favorabilidad define la calificación final cuando los jurados no
 * coinciden: se toma la menos favorable.
 */
public enum Calificacion {
    /** El trabajo no se aprueba y se cierra. */
    NO_APROBADO(1),
    /** Se programa una nueva sesión (solo una vez); en la segunda cierra el trabajo. */
    APLAZADO(2),
    /** El director tiene 10 días hábiles para radicar los ajustes, que verifican los jurados. */
    APROBADO_CON_OBSERVACIONES(3),
    /** Los jurados cargan la versión definitiva y el trabajo finaliza. */
    APROBADO(4);

    /** Mayor es más favorable */
    private final int favorabilidad;

    Calificacion(int favorabilidad) {
        this.favorabilidad = favorabilidad;
    }

    /**
     * @return la calificación menos favorable entre esta y la otra
     */
    public Calificacion menosFavorable(Calificacion otra) {
        return otra != null && otra.favorabilidad < this.favorabilidad ? otra : this;
    }
}
