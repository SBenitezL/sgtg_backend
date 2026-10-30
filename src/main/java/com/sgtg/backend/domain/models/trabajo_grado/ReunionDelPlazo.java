package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

/**
 * Reunión de seguimiento del calendario del plazo, con lo que se sabe de ella
 * en una fecha.
 *
 * @param numero        mes del plazo, desde 1
 * @param fechaSugerida fecha en que corresponde (un mes tras otro desde la
 *                      notificación, corrida por las suspensiones); null
 *                      mientras el trabajo está suspendido y aún no se conoce
 * @param agendada      reunión agendada por el director, null si no se ha agendado
 * @param conInforme    true si ya se radicó el informe mensual de ese mes
 * @param estado        estado en la fecha de la consulta
 */
public record ReunionDelPlazo(int numero, LocalDate fechaSugerida, ReunionSeguimiento agendada, boolean conInforme,
        EstadoReunion estado) {

    /** Fecha de la reunión: la agendada o, si no se ha agendado, la sugerida (null en una suspensión). */
    public LocalDate fecha() {
        return agendada != null ? agendada.fechaHora().toLocalDate() : fechaSugerida;
    }
}
