package com.sgtg.backend.domain.models.notificacion;

/** Relevancia de la notificación; el front la muestra con un color e ícono distintos. */
public enum TipoNotificacion {
    /** Avance normal del trabajo (radicaciones, asignaciones). */
    INFO,
    /** Una decisión o un resultado registrado. */
    CONFIRMACION,
    /** Un plazo por vencer o una acción pendiente. */
    ALERTA,
    /** Un plazo vencido o un cierre sin aprobación. */
    ERROR
}
