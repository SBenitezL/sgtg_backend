package com.sgtg.backend.domain.models.comun;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Decisión de un órgano colegiado (Comité de Programa, Departamento o Consejo
 * de Facultad). Si no es aprobatoria debe incluir observaciones, salvo que el
 * órgano las soporte en un documento adjunto (ver
 * {@link Organo#exigeObservacionesSiNoAprueba()}).
 *
 * @param organo        órgano que emite la decisión
 * @param resultado     resultado de la decisión
 * @param acta          número de acta o resolución, opcional
 * @param rutaArchivo   ruta del archivo de soporte (ej. el acta) en el servidor
 *                      de archivos, opcional
 * @param observaciones observaciones del órgano
 * @param fecha         fecha de la decisión
 * @param responsableId usuario que registró la decisión en nombre del órgano;
 *                      null si no se conoce (decisiones anteriores a este dato)
 */
public record Decision(Organo organo, ResultadoConcepto resultado, String acta, String rutaArchivo,
        String observaciones, LocalDate fecha, Long responsableId) {

    public Decision {
        if (organo == null || resultado == null || fecha == null)
            throw new ReglaNegocioException("error.dominio.decision.datos_incompletos");
        if (resultado != ResultadoConcepto.APROBADO && organo.exigeObservacionesSiNoAprueba()
                && CheckString.isBlank(observaciones))
            throw new ReglaNegocioException("error.dominio.decision.observaciones_obligatorias");
        rutaArchivo = CheckString.isBlank(rutaArchivo) ? null : rutaArchivo.trim();
    }

    /** Decisión sin responsable registrado. */
    public Decision(Organo organo, ResultadoConcepto resultado, String acta, String rutaArchivo,
            String observaciones, LocalDate fecha) {
        this(organo, resultado, acta, rutaArchivo, observaciones, fecha, null);
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
