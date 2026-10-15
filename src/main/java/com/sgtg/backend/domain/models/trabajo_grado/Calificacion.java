package com.sgtg.backend.domain.models.trabajo_grado;

/**
 * Calificación de la sustentación que emite el jurado coordinador (Arts.
 * 22-23); define el resultado de la sesión.
 */
public enum Calificacion {
    /** El trabajo no se aprueba y se cierra. */
    NO_APROBADO,
    /** Se programa una nueva sesión (solo una vez); en la segunda cierra el trabajo. */
    APLAZADO,
    /** El director tiene 10 días hábiles para radicar los ajustes, que verifica el jurado coordinador. */
    APROBADO_CON_OBSERVACIONES,
    /** El jurado coordinador carga la versión definitiva y el trabajo finaliza. */
    APROBADO
}
