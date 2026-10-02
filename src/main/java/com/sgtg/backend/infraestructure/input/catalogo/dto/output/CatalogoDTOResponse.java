package com.sgtg.backend.infraestructure.input.catalogo.dto.output;

import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Programa;

/** Elemento de un catálogo (departamento o programa). */
public record CatalogoDTOResponse(Long id, String nombre) {

    public static CatalogoDTOResponse desde(Departamento departamento) {
        return departamento == null ? null : new CatalogoDTOResponse(departamento.id(), departamento.nombre());
    }

    public static CatalogoDTOResponse desde(Programa programa) {
        return programa == null ? null : new CatalogoDTOResponse(programa.id(), programa.nombre());
    }
}
