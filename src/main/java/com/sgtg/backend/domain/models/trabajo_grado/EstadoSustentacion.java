package com.sgtg.backend.domain.models.trabajo_grado;

/** Estado de la sustentación dentro de su etapa. */
public enum EstadoSustentacion {
    /** El director la solicitó; falta que el Consejo la programe. */
    SOLICITADA,
    /** El Consejo asignó jurados, fecha, hora y salón; se espera la calificación. */
    PROGRAMADA,
    /** Aprobada con observaciones: el director debe radicar los ajustes en 10 días hábiles. */
    EN_AJUSTES,
    /** El director radicó los ajustes; los jurados deben verificarlos. */
    AJUSTES_RADICADOS,
    /** Aprobada; los jurados deben cargar la versión definitiva. */
    APROBADA,
    /** Primera sesión aplazada; el Consejo debe programar la segunda en máximo 60 días calendario. */
    APLAZADA,
    /** Los jurados cargaron la versión definitiva: el trabajo finalizó. */
    FINALIZADA,
    /** No aprobada: el trabajo se cerró. */
    NO_APROBADA
}
