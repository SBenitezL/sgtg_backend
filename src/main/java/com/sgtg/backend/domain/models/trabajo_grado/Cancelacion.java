package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Solicitud de cancelación del trabajo de grado (Arts. 26, 56). La radica el
 * director con el formato de solicitud adjunto, desde cualquier etapa no
 * final, y la decide el Consejo de Facultad con la resolución adjunta. Si no
 * se aprueba, el trabajo sigue su curso y se puede volver a solicitar.
 *
 * @param rutaSolicitud    ruta del formato de solicitud de cancelación
 * @param fechaSolicitud   fecha de la solicitud
 * @param etapaAlSolicitar etapa en la que estaba el trabajo al solicitar
 * @param decision         decisión del Consejo, null mientras esté pendiente
 */
public record Cancelacion(String rutaSolicitud, LocalDate fechaSolicitud, Proceso etapaAlSolicitar,
        Decision decision) {

    public Cancelacion {
        if (CheckString.isBlank(rutaSolicitud))
            throw new ReglaNegocioException("error.dominio.cancelacion.documento_obligatorio");
        if (fechaSolicitud == null || etapaAlSolicitar == null)
            throw new ReglaNegocioException("error.dominio.cancelacion.datos_incompletos");
        rutaSolicitud = rutaSolicitud.trim();
        if (decision != null)
            validarDecision(decision, fechaSolicitud);
    }

    /** Crea una solicitud pendiente de decisión. */
    public static Cancelacion solicitar(String rutaSolicitud, LocalDate fecha, Proceso etapaActual) {
        return new Cancelacion(rutaSolicitud, fecha, etapaActual, null);
    }

    private static void validarDecision(Decision decision, LocalDate fechaSolicitud) {
        if (!decision.emitidaPor(Organo.CONSEJO_FACULTAD))
            throw new ReglaNegocioException("error.dominio.cancelacion.organo_invalido");
        if (decision.rutaArchivo() == null)
            throw new ReglaNegocioException("error.dominio.cancelacion.resolucion_obligatoria");
        if (decision.resultado() != ResultadoConcepto.APROBADO
                && decision.resultado() != ResultadoConcepto.NO_APROBADO)
            throw new ReglaNegocioException("error.dominio.cancelacion.resultado_invalido");
        if (decision.fecha().isBefore(fechaSolicitud))
            throw new ReglaNegocioException("error.dominio.cancelacion.fecha_invalida");
    }

    /**
     * @param decision decisión del Consejo de Facultad con la resolución adjunta
     * @return la solicitud resuelta
     */
    public Cancelacion resolver(Decision decision) {
        if (!estaPendiente())
            throw new ReglaNegocioException("error.dominio.cancelacion.sin_solicitud_pendiente");
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.cancelacion.datos_incompletos");
        return new Cancelacion(rutaSolicitud, fechaSolicitud, etapaAlSolicitar, decision);
    }

    public boolean estaPendiente() {
        return this.decision == null;
    }

    public boolean fueAprobada() {
        return this.decision != null && this.decision.esAprobatoria();
    }
}
