package com.sgtg.backend.domain.models.participantes;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Departamento de la Facultad al que está adscrito un docente. Recibe los
 * anteproyectos y asigna sus evaluadores (Art. 13).
 *
 * @param id     identificador del departamento
 * @param nombre nombre del departamento
 */
public record Departamento(Long id, String nombre) {

    public Departamento {
        if (id == null || CheckString.isBlank(nombre))
            throw new ReglaNegocioException("error.dominio.departamento.datos_incompletos");
    }

    /**
     * @return true si ambos corresponden al mismo departamento
     */
    public boolean esMismo(Departamento otro) {
        return otro != null && this.id.equals(otro.id);
    }
}
