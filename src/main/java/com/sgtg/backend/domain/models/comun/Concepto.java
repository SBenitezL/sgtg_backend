package com.sgtg.backend.domain.models.comun;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Participante;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Opinión emitida por una persona (evaluador, jurado o asesor). Si no es
 * aprobatoria debe incluir observaciones que justifiquen las correcciones o el
 * rechazo.
 *
 * @param emisor        persona que emite el concepto
 * @param resultado     resultado del concepto
 * @param observaciones observaciones del emisor
 * @param fecha         fecha en que se emitió
 */
public record Concepto(Participante emisor, ResultadoConcepto resultado, String observaciones, LocalDate fecha) {

    public Concepto {
        if (emisor == null || resultado == null || fecha == null)
            throw new ReglaNegocioException("error.dominio.concepto.datos_incompletos");
        if (resultado != ResultadoConcepto.APROBADO && CheckString.isBlank(observaciones))
            throw new ReglaNegocioException("error.dominio.concepto.observaciones_obligatorias");
    }

    public boolean esAprobado() {
        return this.resultado == ResultadoConcepto.APROBADO;
    }

    public boolean requiereCorrecciones() {
        return this.resultado == ResultadoConcepto.CON_CORRECCIONES;
    }
}
