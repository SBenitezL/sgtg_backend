package com.sgtg.backend.domain.models.participantes;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Programa académico de pregrado en el que está inscrito un estudiante. Su
 * Comité de Programa emite el concepto sobre la propuesta (Art. 11). Está
 * adscrito a un departamento de la Facultad: los miembros del Comité son
 * docentes de ese departamento.
 *
 * @param id           identificador del programa
 * @param nombre       nombre del programa
 * @param departamento departamento al que está adscrito; es null en la copia
 *                     que guarda el trabajo de grado, que solo identifica el
 *                     programa (el departamento se consulta en el catálogo)
 */
public record Programa(Long id, String nombre, Departamento departamento) {

    public Programa {
        if (id == null || CheckString.isBlank(nombre))
            throw new ReglaNegocioException("error.dominio.programa.datos_incompletos");
    }

    /** Programa identificado solo por su id y nombre, sin el departamento. */
    public Programa(Long id, String nombre) {
        this(id, nombre, null);
    }

    /**
     * @return true si ambos corresponden al mismo programa
     */
    public boolean esMismo(Programa otro) {
        return otro != null && this.id.equals(otro.id);
    }

    /**
     * @return true si el programa está adscrito al departamento indicado
     */
    public boolean esDelDepartamento(Departamento otro) {
        return this.departamento != null && this.departamento.esMismo(otro);
    }
}
