package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Reunión de seguimiento de un mes del plazo de ejecución, agendada por el
 * director (ver {@link Ejecucion#reuniones}). En ella se revisa el avance que
 * los estudiantes reportan en el informe mensual de ese mes (Art. 45).
 *
 * @param numero        mes del plazo al que corresponde, desde 1
 * @param fechaHora     fecha y hora agendadas
 * @param lugar         salón o enlace de la reunión
 * @param agendadaPor   código del director que la agendó
 * @param realizada     true si el director registró que se realizó
 * @param observaciones observaciones al registrarla como realizada, null si no tiene
 */
public record ReunionSeguimiento(int numero, LocalDateTime fechaHora, String lugar, String agendadaPor,
        boolean realizada, String observaciones) {

    public ReunionSeguimiento {
        if (numero < 1 || fechaHora == null || CheckString.isBlank(lugar))
            throw new ReglaNegocioException("error.dominio.reunion.datos_incompletos");
        lugar = lugar.trim();
        observaciones = CheckString.isBlank(observaciones) ? null : observaciones.trim();
    }

    /** Reunión recién agendada (o reprogramada), aún no realizada. */
    static ReunionSeguimiento agendar(int numero, LocalDateTime fechaHora, String lugar, String agendadaPor) {
        return new ReunionSeguimiento(numero, fechaHora, lugar, agendadaPor, false, null);
    }

    /** La misma reunión, registrada como realizada. */
    ReunionSeguimiento comoRealizada(String observacionesDeLaReunion) {
        return new ReunionSeguimiento(numero, fechaHora, lugar, agendadaPor, true, observacionesDeLaReunion);
    }
}
