package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

/**
 * Estado persistido de la ejecución del trabajo de grado, usado para
 * reconstruirla.
 *
 * @param fechaNotificacion       fecha de notificación de la aprobación del Consejo
 * @param fechaFin                fin del plazo de ejecución, incluidas la prórroga y la reanudación
 * @param fechaMinimaSustentacion fecha desde la que se puede solicitar la sustentación
 * @param cambiosObjetivos        solicitudes de cambio de título u objetivos, en orden
 * @param informesMensuales       informes mensuales radicados (solo práctica profesional), en orden
 * @param prorroga                prórroga solicitada, null si no se ha solicitado
 * @param suspensiones            solicitudes de suspensión, en orden
 * @param reuniones               reuniones de seguimiento agendadas, por número de mes
 */
public record DatosEjecucion(LocalDate fechaNotificacion, LocalDate fechaFin, LocalDate fechaMinimaSustentacion,
        List<CambioObjetivos> cambiosObjetivos, List<InformeMensual> informesMensuales, Prorroga prorroga,
        List<Suspension> suspensiones, List<ReunionSeguimiento> reuniones) {

    public DatosEjecucion {
        if (fechaNotificacion == null || fechaFin == null || fechaMinimaSustentacion == null)
            throw new ReglaNegocioException("error.dominio.ejecucion.datos_incompletos");
        cambiosObjetivos = cambiosObjetivos == null ? List.of() : List.copyOf(cambiosObjetivos);
        informesMensuales = informesMensuales == null ? List.of() : List.copyOf(informesMensuales);
        suspensiones = suspensiones == null ? List.of() : List.copyOf(suspensiones);
        reuniones = reuniones == null ? List.of() : List.copyOf(reuniones);
    }

    /** Ejecución sin reuniones de seguimiento agendadas. */
    public DatosEjecucion(LocalDate fechaNotificacion, LocalDate fechaFin, LocalDate fechaMinimaSustentacion,
            List<CambioObjetivos> cambiosObjetivos, List<InformeMensual> informesMensuales, Prorroga prorroga,
            List<Suspension> suspensiones) {
        this(fechaNotificacion, fechaFin, fechaMinimaSustentacion, cambiosObjetivos, informesMensuales, prorroga,
                suspensiones, List.of());
    }

    /** Ejecución sin cambios de objetivos, informes, prórroga ni suspensiones. */
    public DatosEjecucion(LocalDate fechaNotificacion, LocalDate fechaFin, LocalDate fechaMinimaSustentacion) {
        this(fechaNotificacion, fechaFin, fechaMinimaSustentacion, List.of(), List.of(), null, List.of());
    }
}
