package com.sgtg.backend.domain.models.comun;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Decisión de un órgano colegiado (Comité de Programa, Departamento o Consejo
 * de Facultad). Si no es aprobatoria debe incluir observaciones.
 *
 * @param organo        órgano que emite la decisión
 * @param resultado     resultado de la decisión
 * @param acta          número de acta o resolución, opcional
 * @param observaciones observaciones del órgano
 * @param fecha         fecha de la decisión
 */
public record Decision(Organo organo, ResultadoConcepto resultado, String acta, String observaciones,
        LocalDate fecha) {

    public Decision {
        if (organo == null || resultado == null || fecha == null)
            throw new ReglaNegocioException("error.dominio.decision.datos_incompletos");
        if (resultado != ResultadoConcepto.APROBADO && CheckString.isBlank(observaciones))
            throw new ReglaNegocioException("error.dominio.decision.observaciones_obligatorias");
    }

    public boolean esAprobatoria() {
        return this.resultado == ResultadoConcepto.APROBADO;
    }

    /**
     * @param esperado órgano que debería emitir la decisión
     * @return true si la decisión fue emitida por ese órgano
     */
    public boolean emitidaPor(Organo esperado) {
        return this.organo == esperado;
    }
}
