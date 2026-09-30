package com.sgtg.backend.domain.models.trabajo_grado;

/**
 * Estado de una entrega dentro de una etapa (propuesta, anteproyecto, etc.).
 * Es independiente de la etapa en la que se encuentra el trabajo de grado.
 */
public enum Estado {
    /** La entrega fue radicada y está pendiente de revisión. */
    RADICADO,
    /** La entrega está siendo revisada. */
    EN_REVISION,
    /** La revisión solicitó correcciones; se espera una nueva radicación. */
    CON_CORRECCIONES,
    /** La entrega fue aprobada. */
    APROBADO,
    /** La entrega fue rechazada. */
    RECHAZADO;

    /**
     * Indica si desde este estado se puede pasar al estado destino.
     *
     * @param destino estado al que se quiere cambiar
     * @return true si el cambio está permitido
     */
    public boolean puedeCambiarA(Estado destino) {
        if (destino == null)
            return false;
        return switch (this) {
            case RADICADO -> destino == EN_REVISION;
            case EN_REVISION -> destino == CON_CORRECCIONES || destino == APROBADO || destino == RECHAZADO;
            case CON_CORRECCIONES -> destino == RADICADO;
            case APROBADO, RECHAZADO -> false;
        };
    }

    /**
     * @return true si el estado no admite más cambios
     */
    public boolean esFinal() {
        return this == APROBADO || this == RECHAZADO;
    }
}
