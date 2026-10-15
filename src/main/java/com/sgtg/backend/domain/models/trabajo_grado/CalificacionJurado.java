package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Calificación de una sesión de sustentación, que emite el jurado coordinador.
 * Si no es APROBADO debe incluir observaciones. El acta de sustentación
 * (formato G) es obligatoria.
 *
 * @param sesion        número de la sesión
 * @param jurado        jurado coordinador que califica
 * @param calificacion  calificación
 * @param observaciones observaciones del jurado
 * @param rutaActa      ruta del acta (formato G)
 * @param fecha         fecha de la calificación
 */
public record CalificacionJurado(int sesion, Evaluador jurado, Calificacion calificacion, String observaciones,
        String rutaActa, LocalDate fecha) {

    public CalificacionJurado {
        if (jurado == null || calificacion == null || fecha == null || sesion < 1)
            throw new ReglaNegocioException("error.dominio.sustentacion.calificacion_incompleta");
        if (calificacion != Calificacion.APROBADO && CheckString.isBlank(observaciones))
            throw new ReglaNegocioException("error.dominio.sustentacion.observaciones_obligatorias");
        if (CheckString.isBlank(rutaActa))
            throw new ReglaNegocioException("error.dominio.sustentacion.acta_obligatoria");
        rutaActa = rutaActa.trim();
    }
}
