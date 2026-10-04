package com.sgtg.backend.domain.models.trabajo_grado;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Concepto;

/**
 * Concepto de un evaluador sobre una versión del anteproyecto (Art. 13). Puede
 * acompañarse del formato B (evaluación del anteproyecto) cargado al servidor
 * de archivos.
 *
 * @param version  versión del anteproyecto evaluada
 * @param concepto concepto del evaluador: APROBADO o CON_CORRECCIONES
 * @param formato  formato B, null si no se cargó
 */
public record ConceptoEvaluacion(int version, Concepto concepto, Documento formato) {

    public ConceptoEvaluacion {
        if (version < 1 || concepto == null)
            throw new ReglaNegocioException("error.dominio.concepto_evaluacion.datos_incompletos");
        if (formato != null && formato.tipo() != TipoDocumento.EVALUACION_ANTEPROYECTO)
            throw new ReglaNegocioException("error.dominio.concepto_evaluacion.formato_invalido");
    }

    /**
     * @return true si el concepto lo emitió el usuario indicado
     */
    public boolean emitidoPor(Long usuarioId) {
        return usuarioId != null && usuarioId.equals(this.concepto.emisor().getUsuarioId());
    }
}
