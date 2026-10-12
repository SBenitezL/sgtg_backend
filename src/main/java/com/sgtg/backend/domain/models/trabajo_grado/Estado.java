package com.sgtg.backend.domain.models.trabajo_grado;

/**
 * Estado de una entrega enviada a revisión (propuesta, anteproyecto). Se
 * asigna al enviarla: antes de eso la entrega no tiene estado. Es
 * independiente de la etapa en la que se encuentra el trabajo de grado.
 */
public enum Estado {
    /** La entrega fue enviada y está en revisión. */
    EN_REVISION,
    /** La revisión solicitó correcciones; se espera una nueva versión. */
    CON_CORRECCIONES,
    /** La entrega fue aprobada. */
    APROBADO,
    /** La entrega fue rechazada. */
    RECHAZADO,
    /** La entrega no fue aprobada; se puede radicar una nueva versión. */
    NO_APROBADO;

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
            case EN_REVISION -> destino == CON_CORRECCIONES || destino == APROBADO || destino == RECHAZADO
                    || destino == NO_APROBADO;
            case CON_CORRECCIONES -> destino == EN_REVISION;
            case APROBADO, RECHAZADO, NO_APROBADO -> false;
        };
    }

    /**
     * @return true si el estado no admite más cambios
     */
    public boolean esFinal() {
        return this == APROBADO || this == RECHAZADO || this == NO_APROBADO;
    }
}
