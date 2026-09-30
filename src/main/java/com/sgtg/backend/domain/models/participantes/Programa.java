package com.sgtg.backend.domain.models.participantes;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Programa académico de pregrado en el que está inscrito un estudiante. Su
 * Comité de Programa emite el concepto sobre la propuesta (Art. 11).
 *
 * @param id     identificador del programa
 * @param nombre nombre del programa
 */
public record Programa(Long id, String nombre) {

    public Programa {
        if (id == null || CheckString.isBlank(nombre))
            throw new ReglaNegocioException("error.dominio.programa.datos_incompletos");
    }

    /**
     * @return true si ambos corresponden al mismo programa
     */
    public boolean esMismo(Programa otro) {
        return otro != null && this.id.equals(otro.id);
    }
}
