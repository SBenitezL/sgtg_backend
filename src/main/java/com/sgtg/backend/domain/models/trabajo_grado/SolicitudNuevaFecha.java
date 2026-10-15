package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Solicitud del director de una nueva fecha para la sesión de sustentación
 * programada. Se radica antes de la fecha designada, con el documento de la
 * solicitud, y la decide el Consejo de Facultad con la resolución. Si la
 * aprueba, fija la nueva fecha y hora (y, si quiere, otro salón) y la sesión
 * se reprograma con los mismos jurados.
 *
 * @param sesion            número de la sesión que se reprograma
 * @param rutaSolicitud     ruta del documento de la solicitud
 * @param fechaSolicitud    fecha de la solicitud
 * @param fechaHoraAnterior fecha y hora de la sesión al solicitar
 * @param decision          decisión del Consejo, null mientras esté pendiente
 * @param nuevaFechaHora    fecha y hora aprobadas, null si no se aprobó
 * @param nuevoSalon        salón aprobado, null si se conserva el anterior
 */
public record SolicitudNuevaFecha(int sesion, String rutaSolicitud, LocalDate fechaSolicitud,
        LocalDateTime fechaHoraAnterior, Decision decision, LocalDateTime nuevaFechaHora, String nuevoSalon) {

    public SolicitudNuevaFecha {
        if (sesion < 1 || fechaSolicitud == null || fechaHoraAnterior == null)
            throw new ReglaNegocioException("error.dominio.nueva_fecha.datos_incompletos");
        if (CheckString.isBlank(rutaSolicitud))
            throw new ReglaNegocioException("error.dominio.nueva_fecha.documento_obligatorio");
        rutaSolicitud = rutaSolicitud.trim();
        nuevoSalon = CheckString.isBlank(nuevoSalon) ? null : nuevoSalon.trim();
        if (decision != null)
            validarDecision(decision, fechaSolicitud, nuevaFechaHora);
    }

    /** Crea una solicitud pendiente de decisión. */
    public static SolicitudNuevaFecha solicitar(int sesion, String rutaSolicitud, LocalDate fecha,
            LocalDateTime fechaHoraAnterior) {
        return new SolicitudNuevaFecha(sesion, rutaSolicitud, fecha, fechaHoraAnterior, null, null, null);
    }

    private static void validarDecision(Decision decision, LocalDate fechaSolicitud, LocalDateTime nuevaFechaHora) {
        if (!decision.emitidaPor(Organo.CONSEJO_FACULTAD))
            throw new ReglaNegocioException("error.dominio.nueva_fecha.organo_invalido");
        if (decision.rutaArchivo() == null)
            throw new ReglaNegocioException("error.dominio.nueva_fecha.resolucion_obligatoria");
        if (decision.resultado() != ResultadoConcepto.APROBADO
                && decision.resultado() != ResultadoConcepto.NO_APROBADO)
            throw new ReglaNegocioException("error.dominio.nueva_fecha.resultado_invalido");
        if (decision.fecha().isBefore(fechaSolicitud))
            throw new ReglaNegocioException("error.dominio.nueva_fecha.fecha_invalida");
        if (decision.esAprobatoria() && nuevaFechaHora == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.nueva_fecha_obligatoria");
    }

    /**
     * @param decision       decisión del Consejo con la resolución adjunta
     * @param nuevaFechaHora fecha y hora de la sesión si se aprueba
     * @param nuevoSalon     salón si se aprueba (opcional)
     * @return la solicitud resuelta
     */
    public SolicitudNuevaFecha resolver(Decision decision, LocalDateTime nuevaFechaHora, String nuevoSalon) {
        if (!estaPendiente())
            throw new ReglaNegocioException("error.dominio.nueva_fecha.sin_solicitud_pendiente");
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.nueva_fecha.datos_incompletos");
        boolean aprobada = decision.esAprobatoria();
        return new SolicitudNuevaFecha(sesion, rutaSolicitud, fechaSolicitud, fechaHoraAnterior, decision,
                aprobada ? nuevaFechaHora : null, aprobada ? nuevoSalon : null);
    }

    public boolean estaPendiente() {
        return this.decision == null;
    }

    public boolean fueAprobada() {
        return this.decision != null && this.decision.esAprobatoria();
    }
}
