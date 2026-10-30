package com.sgtg.backend.domain.models.trabajo_grado;

/** Estado de una reunión de seguimiento en una fecha (se calcula; no se guarda). */
public enum EstadoReunion {
    /** Aún no se agenda y su fecha no ha pasado. */
    PENDIENTE,
    /** Agendada para hoy o una fecha futura. */
    AGENDADA,
    /** El director registró que se realizó. */
    REALIZADA,
    /** Pasó su fecha (la agendada o la sugerida) sin registrarse como realizada. */
    VENCIDA,
    /** El trabajo está suspendido y la fecha se conocerá al reanudarlo. */
    EN_SUSPENSION
}
