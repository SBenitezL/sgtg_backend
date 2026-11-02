package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Solicitud del director para cambiar el título u objetivos del trabajo en
 * curso (Arts. 17-18). Primero la avala (o no) el Comité del programa; si no la
 * avala, queda resuelta como no aprobada. Si la avala, la decide el Consejo de
 * Facultad. Guarda la descripción anterior y la propuesta para conservar el
 * historial.
 *
 * @param tituloAnterior      título vigente al solicitar
 * @param objetivosAnteriores objetivos vigentes al solicitar
 * @param tituloPropuesto     título nuevo
 * @param objetivosPropuestos objetivos nuevos
 * @param justificacion       motivo del cambio
 * @param fechaSolicitud      fecha de la solicitud
 * @param avalComite          aval del Comité del programa, null mientras esté
 *                            pendiente (y en las solicitudes anteriores a este paso)
 * @param decision            decisión del Consejo, null mientras esté pendiente
 */
public record CambioObjetivos(String tituloAnterior, List<String> objetivosAnteriores, String tituloPropuesto,
        List<String> objetivosPropuestos, String justificacion, LocalDate fechaSolicitud, Decision avalComite,
        Decision decision) {

    public CambioObjetivos {
        if (CheckString.isBlank(tituloPropuesto) || objetivosPropuestos == null || objetivosPropuestos.isEmpty()
                || objetivosPropuestos.stream().anyMatch(CheckString::isBlank))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.descripcion_obligatoria");
        if (CheckString.isBlank(justificacion) || fechaSolicitud == null || tituloAnterior == null
                || objetivosAnteriores == null)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.datos_incompletos");
        objetivosAnteriores = List.copyOf(objetivosAnteriores);
        objetivosPropuestos = List.copyOf(objetivosPropuestos);
        if (tituloPropuesto.equals(tituloAnterior) && objetivosPropuestos.equals(objetivosAnteriores))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.sin_cambios");
        if (avalComite != null)
            validarAval(avalComite, fechaSolicitud);
        if (decision != null) {
            // Las solicitudes resueltas antes de existir el aval no lo tienen
            if (avalComite != null && !avalComite.esAprobatoria())
                throw new ReglaNegocioException("error.dominio.cambio_objetivos.aval_comite_pendiente");
            validarDecision(decision, fechaSolicitud);
        }
    }

    /** Crea una solicitud pendiente del aval del Comité. */
    public static CambioObjetivos solicitar(String tituloAnterior, List<String> objetivosAnteriores,
            String tituloPropuesto, List<String> objetivosPropuestos, String justificacion, LocalDate fecha) {
        return new CambioObjetivos(tituloAnterior, objetivosAnteriores, tituloPropuesto, objetivosPropuestos,
                justificacion, fecha, null, null);
    }

    private static void validarAval(Decision aval, LocalDate fechaSolicitud) {
        if (!aval.emitidaPor(Organo.COMITE_PROGRAMA))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.organo_aval_invalido");
        validarResultadoYFecha(aval, fechaSolicitud);
    }

    private static void validarDecision(Decision decision, LocalDate fechaSolicitud) {
        if (!decision.emitidaPor(Organo.CONSEJO_FACULTAD))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.organo_invalido");
        if (decision.rutaArchivo() == null)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.resolucion_obligatoria");
        validarResultadoYFecha(decision, fechaSolicitud);
    }

    private static void validarResultadoYFecha(Decision decision, LocalDate fechaSolicitud) {
        if (decision.resultado() != ResultadoConcepto.APROBADO
                && decision.resultado() != ResultadoConcepto.NO_APROBADO)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.resultado_invalido");
        if (decision.fecha().isBefore(fechaSolicitud))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.fecha_invalida");
    }

    /**
     * @param aval aval del Comité del programa (APROBADO o NO_APROBADO, con
     *             observaciones si no avala y el acta opcional)
     * @return la solicitud avalada (pasa al Consejo) o resuelta como no aprobada
     */
    public CambioObjetivos avalar(Decision aval) {
        if (!pendienteDelComite())
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.sin_aval_pendiente");
        if (aval == null)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.datos_incompletos");
        return new CambioObjetivos(tituloAnterior, objetivosAnteriores, tituloPropuesto, objetivosPropuestos,
                justificacion, fechaSolicitud, aval, null);
    }

    /**
     * @param decision decisión del Consejo de Facultad con la resolución adjunta
     * @return la solicitud resuelta
     */
    public CambioObjetivos resolver(Decision decision) {
        if (!estaPendiente())
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.sin_solicitud_pendiente");
        if (pendienteDelComite())
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.aval_comite_pendiente");
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.datos_incompletos");
        return new CambioObjetivos(tituloAnterior, objetivosAnteriores, tituloPropuesto, objetivosPropuestos,
                justificacion, fechaSolicitud, avalComite, decision);
    }

    /** Pendiente del Comité o, ya avalada, del Consejo. Una no avalada queda resuelta. */
    public boolean estaPendiente() {
        return this.decision == null && (this.avalComite == null || this.avalComite.esAprobatoria());
    }

    public boolean pendienteDelComite() {
        return this.decision == null && this.avalComite == null;
    }

    public boolean pendienteDelConsejo() {
        return this.decision == null && this.avalComite != null && this.avalComite.esAprobatoria();
    }

    public boolean fueAprobado() {
        return this.decision != null && this.decision.esAprobatoria();
    }
}
