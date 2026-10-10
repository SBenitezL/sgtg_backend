package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Calificación que emite un jurado en una sesión de sustentación. Si no es
 * APROBADO debe incluir observaciones. Puede adjuntar la ruta del acta de
 * sustentación (formato G).
 *
 * @param sesion        número de la sesión
 * @param jurado        jurado que califica
 * @param calificacion  calificación
 * @param observaciones observaciones del jurado
 * @param rutaActa      ruta del acta (formato G), opcional
 * @param fecha         fecha de la calificación
 */
public record CalificacionJurado(int sesion, Evaluador jurado, Calificacion calificacion, String observaciones,
        String rutaActa, LocalDate fecha) {

    public CalificacionJurado {
        if (jurado == null || calificacion == null || fecha == null || sesion < 1)
            throw new ReglaNegocioException("error.dominio.sustentacion.calificacion_incompleta");
        if (calificacion != Calificacion.APROBADO && CheckString.isBlank(observaciones))
            throw new ReglaNegocioException("error.dominio.sustentacion.observaciones_obligatorias");
        rutaActa = CheckString.isBlank(rutaActa) ? null : rutaActa.trim();
    }
}
