package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Registro de un jurado sobre una sesión: la inasistencia del estudiante o la
 * verificación de los ajustes de una sustentación aprobada con observaciones.
 *
 * @param sesion        número de la sesión
 * @param jurado        jurado que registra
 * @param favorable     en una verificación, true si los ajustes cumplen; en una
 *                      inasistencia siempre false
 * @param observaciones obligatorias si no es favorable
 * @param fecha         fecha del registro
 */
public record RegistroJurado(int sesion, Evaluador jurado, boolean favorable, String observaciones,
        LocalDate fecha) {

    public RegistroJurado {
        if (jurado == null || fecha == null || sesion < 1)
            throw new ReglaNegocioException("error.dominio.sustentacion.registro_incompleto");
        if (!favorable && CheckString.isBlank(observaciones))
            throw new ReglaNegocioException("error.dominio.sustentacion.observaciones_obligatorias");
    }
}
