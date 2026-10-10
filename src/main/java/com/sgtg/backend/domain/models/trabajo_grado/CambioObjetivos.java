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
 * curso, y la decisión del Consejo de Facultad sobre ella (Arts. 17-18). Guarda
 * la descripción anterior y la propuesta para conservar el historial.
 *
 * @param tituloAnterior      título vigente al solicitar
 * @param objetivosAnteriores objetivos vigentes al solicitar
 * @param tituloPropuesto     título nuevo
 * @param objetivosPropuestos objetivos nuevos
 * @param justificacion       motivo del cambio
 * @param fechaSolicitud      fecha de la solicitud
 * @param decision            decisión del Consejo, null mientras esté pendiente
 */
public record CambioObjetivos(String tituloAnterior, List<String> objetivosAnteriores, String tituloPropuesto,
        List<String> objetivosPropuestos, String justificacion, LocalDate fechaSolicitud, Decision decision) {

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
        if (decision != null)
            validarDecision(decision, fechaSolicitud);
    }

    /** Crea una solicitud pendiente de decisión. */
    public static CambioObjetivos solicitar(String tituloAnterior, List<String> objetivosAnteriores,
            String tituloPropuesto, List<String> objetivosPropuestos, String justificacion, LocalDate fecha) {
        return new CambioObjetivos(tituloAnterior, objetivosAnteriores, tituloPropuesto, objetivosPropuestos,
                justificacion, fecha, null);
    }

    private static void validarDecision(Decision decision, LocalDate fechaSolicitud) {
        if (!decision.emitidaPor(Organo.CONSEJO_FACULTAD))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.organo_invalido");
        if (decision.rutaArchivo() == null)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.resolucion_obligatoria");
        if (decision.resultado() != ResultadoConcepto.APROBADO
                && decision.resultado() != ResultadoConcepto.NO_APROBADO)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.resultado_invalido");
        if (decision.fecha().isBefore(fechaSolicitud))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.fecha_invalida");
    }

    /**
     * @param decision decisión del Consejo de Facultad con la resolución adjunta
     * @return la solicitud resuelta
     */
    public CambioObjetivos resolver(Decision decision) {
        if (!estaPendiente())
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.sin_solicitud_pendiente");
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.datos_incompletos");
        return new CambioObjetivos(tituloAnterior, objetivosAnteriores, tituloPropuesto, objetivosPropuestos,
                justificacion, fechaSolicitud, decision);
    }

    public boolean estaPendiente() {
        return this.decision == null;
    }

    public boolean fueAprobado() {
        return this.decision != null && this.decision.esAprobatoria();
    }
}
