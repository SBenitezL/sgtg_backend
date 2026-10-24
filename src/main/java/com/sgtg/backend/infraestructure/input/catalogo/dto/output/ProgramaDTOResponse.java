package com.sgtg.backend.infraestructure.input.catalogo.dto.output;

import com.sgtg.backend.domain.models.participantes.Programa;

/**
 * Programa académico del catálogo.
 *
 * @param id           identificador del programa
 * @param nombre       nombre del programa
 * @param departamento departamento al que está adscrito (null en programas
 *                     registrados antes de exigirlo)
 */
public record ProgramaDTOResponse(Long id, String nombre, CatalogoDTOResponse departamento) {

    public static ProgramaDTOResponse desde(Programa programa) {
        return programa == null ? null
                : new ProgramaDTOResponse(programa.id(), programa.nombre(),
                        CatalogoDTOResponse.desde(programa.departamento()));
    }
}
