package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.PeriodoAcademico;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Suspensión del trabajo de grado (Arts. 27, 57). La solicita el director con
 * el documento adjunto y la decide el Consejo de Facultad con la resolución
 * adjunta. Si se aprueba, rige desde la fecha de la decisión, congela los días
 * que faltaban del plazo y dura como máximo hasta el fin del período académico
 * siguiente: el trabajo se reanuda a más tardar el primer día del período
 * posterior. La reanuda el director con un documento adjunto o, al llegar la
 * fecha máxima, el sistema.
 *
 * @param rutaSolicitud          ruta del documento de la solicitud
 * @param fechaSolicitud         fecha de la solicitud
 * @param decision               decisión del Consejo, null mientras esté pendiente
 * @param etapaPrevia            etapa a la que vuelve al reanudar (EN_CURSO o PRORROGA); solo si se aprobó
 * @param diasRestantes          días del plazo que faltaban al suspender; solo si se aprobó
 * @param fechaMaximaReanudacion fecha en que se reanuda a más tardar; solo si se aprobó
 * @param fechaReanudacion       fecha en que se reanudó, null mientras siga suspendido
 * @param rutaReanudacion        ruta del documento de la reanudación, null si fue automática
 */
public record Suspension(String rutaSolicitud, LocalDate fechaSolicitud, Decision decision, Proceso etapaPrevia,
        Long diasRestantes, LocalDate fechaMaximaReanudacion, LocalDate fechaReanudacion, String rutaReanudacion) {

    public Suspension {
        if (CheckString.isBlank(rutaSolicitud))
            throw new ReglaNegocioException("error.dominio.suspension.documento_obligatorio");
        if (fechaSolicitud == null)
            throw new ReglaNegocioException("error.dominio.suspension.datos_incompletos");
        rutaSolicitud = rutaSolicitud.trim();
        rutaReanudacion = CheckString.isBlank(rutaReanudacion) ? null : rutaReanudacion.trim();
        if (decision != null)
            validarDecision(decision, fechaSolicitud);
        if (decision != null && decision.esAprobatoria()) {
            if ((etapaPrevia != Proceso.EN_CURSO && etapaPrevia != Proceso.PRORROGA) || diasRestantes == null
                    || diasRestantes <= 0 || fechaMaximaReanudacion == null
                    || !fechaMaximaReanudacion.isAfter(decision.fecha()))
                throw new ReglaNegocioException("error.dominio.suspension.datos_incompletos");
        }
        if (fechaReanudacion != null) {
            if (decision == null || !decision.esAprobatoria())
                throw new ReglaNegocioException("error.dominio.suspension.sin_suspension_activa");
            if (fechaReanudacion.isBefore(decision.fecha()) || fechaReanudacion.isAfter(fechaMaximaReanudacion))
                throw new ReglaNegocioException("error.dominio.suspension.fecha_reanudacion_invalida");
        }
    }

    /** Crea una solicitud pendiente de decisión. */
    public static Suspension solicitar(String rutaSolicitud, LocalDate fecha) {
        return new Suspension(rutaSolicitud, fecha, null, null, null, null, null, null);
    }

    /**
     * Fecha en que se reanuda a más tardar una suspensión que inicia en la fecha
     * dada: el primer día del período académico posterior al siguiente.
     */
    public static LocalDate fechaMaximaReanudacion(LocalDate inicio) {
        return PeriodoAcademico.de(inicio).siguiente().fin().plusDays(1);
    }

    private static void validarDecision(Decision decision, LocalDate fechaSolicitud) {
        if (!decision.emitidaPor(Organo.CONSEJO_FACULTAD))
            throw new ReglaNegocioException("error.dominio.suspension.organo_invalido");
        if (decision.rutaArchivo() == null)
            throw new ReglaNegocioException("error.dominio.suspension.resolucion_obligatoria");
        if (decision.resultado() != ResultadoConcepto.APROBADO
                && decision.resultado() != ResultadoConcepto.NO_APROBADO)
            throw new ReglaNegocioException("error.dominio.suspension.resultado_invalido");
        if (decision.fecha().isBefore(fechaSolicitud))
            throw new ReglaNegocioException("error.dominio.suspension.fecha_invalida");
    }

    /**
     * Resuelve la solicitud pendiente.
     *
     * @param decision      decisión del Consejo de Facultad con la resolución adjunta
     * @param etapaPrevia   etapa en la que está el trabajo al decidir
     * @param diasRestantes días del plazo que faltan en la fecha de la decisión
     * @return la suspensión resuelta
     */
    public Suspension resolver(Decision decision, Proceso etapaPrevia, long diasRestantes) {
        if (!estaPendiente())
            throw new ReglaNegocioException("error.dominio.suspension.sin_solicitud_pendiente");
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.suspension.datos_incompletos");
        if (!decision.esAprobatoria())
            return new Suspension(rutaSolicitud, fechaSolicitud, decision, null, null, null, null, null);
        if (diasRestantes <= 0)
            throw new ReglaNegocioException("error.dominio.suspension.plazo_vencido");
        return new Suspension(rutaSolicitud, fechaSolicitud, decision, etapaPrevia, diasRestantes,
                fechaMaximaReanudacion(decision.fecha()), null, null);
    }

    /**
     * @param fecha           fecha de reanudación
     * @param rutaReanudacion ruta del documento, null si la reanudación es automática
     * @return la suspensión reanudada
     */
    public Suspension reanudar(LocalDate fecha, String rutaReanudacion) {
        if (!estaActiva())
            throw new ReglaNegocioException("error.dominio.suspension.sin_suspension_activa");
        if (fecha == null)
            throw new ReglaNegocioException("error.dominio.suspension.datos_incompletos");
        return new Suspension(rutaSolicitud, fechaSolicitud, decision, etapaPrevia, diasRestantes,
                fechaMaximaReanudacion, fecha, rutaReanudacion);
    }

    public boolean estaPendiente() {
        return this.decision == null;
    }

    public boolean fueAprobada() {
        return this.decision != null && this.decision.esAprobatoria();
    }

    /**
     * @return true si fue aprobada y aún no se ha reanudado
     */
    public boolean estaActiva() {
        return fueAprobada() && this.fechaReanudacion == null;
    }

    /**
     * @return fecha desde la que rige la suspensión (la de la decisión), null si no se aprobó
     */
    public LocalDate fechaInicio() {
        return fueAprobada() ? this.decision.fecha() : null;
    }

    /**
     * @return true si la reanudó el sistema al llegar la fecha máxima
     */
    public boolean fueReanudadaAutomaticamente() {
        return this.fechaReanudacion != null && this.rutaReanudacion == null;
    }
}
