package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Registro histórico de un cambio de etapa del trabajo de grado. Solo puede
 * representar transiciones permitidas por {@link Proceso#puedeAvanzarA}.
 *
 * @param origen      etapa desde la que se avanzó
 * @param destino     etapa a la que se avanzó
 * @param fecha       fecha y hora del cambio
 * @param responsable código del usuario que realizó el cambio
 * @param observacion comentario opcional sobre el cambio
 */
public record CambioEtapa(Proceso origen, Proceso destino, LocalDateTime fecha, String responsable,
        String observacion) {

    public CambioEtapa {
        if (origen == null || destino == null || fecha == null || CheckString.isBlank(responsable))
            throw new ReglaNegocioException("error.dominio.cambio_etapa.datos_incompletos");
        if (!origen.puedeAvanzarA(destino))
            throw new ReglaNegocioException("error.dominio.proceso.transicion_invalida");
    }
}
