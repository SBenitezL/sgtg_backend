package com.sgtg.backend.domain.models.trabajo_grado;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Concepto;

/**
 * Concepto de un evaluador sobre una versión del anteproyecto (Art. 13), con
 * el formato B (evaluación del anteproyecto) y, si el evaluador lo adjunta, un
 * documento con sus anotaciones.
 *
 * @param version     versión del anteproyecto evaluada
 * @param concepto    concepto del evaluador: APROBADO o CON_CORRECCIONES
 * @param formato     formato B (obligatorio al registrar un concepto nuevo; null en datos anteriores)
 * @param anotaciones documento con las anotaciones del evaluador, null si no lo adjuntó
 */
public record ConceptoEvaluacion(int version, Concepto concepto, Documento formato, Documento anotaciones) {

    public ConceptoEvaluacion {
        if (version < 1 || concepto == null)
            throw new ReglaNegocioException("error.dominio.concepto_evaluacion.datos_incompletos");
        if (formato != null && formato.tipo() != TipoDocumento.EVALUACION_ANTEPROYECTO)
            throw new ReglaNegocioException("error.dominio.concepto_evaluacion.formato_invalido");
        if (anotaciones != null && anotaciones.tipo() != TipoDocumento.ANOTACIONES_EVALUACION)
            throw new ReglaNegocioException("error.dominio.concepto_evaluacion.formato_invalido");
    }

    /** Concepto sin documento de anotaciones. */
    public ConceptoEvaluacion(int version, Concepto concepto, Documento formato) {
        this(version, concepto, formato, null);
    }

    /**
     * @return true si el concepto lo emitió el usuario indicado
     */
    public boolean emitidoPor(Long usuarioId) {
        return usuarioId != null && usuarioId.equals(this.concepto.emisor().getUsuarioId());
    }
}
