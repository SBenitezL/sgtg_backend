package com.sgtg.backend.domain.models.comun;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Día no hábil institucional (receso, cierre de la universidad, etc.) que
 * registra un administrador. Se suma a los fines de semana y a los festivos
 * de ley para calcular los plazos en días hábiles.
 *
 * @param fecha       día no hábil
 * @param descripcion motivo
 */
public record DiaNoHabil(LocalDate fecha, String descripcion) {

    public DiaNoHabil {
        if (fecha == null || CheckString.isBlank(descripcion))
            throw new ReglaNegocioException("error.dominio.dia_no_habil.datos_incompletos");
    }
}
