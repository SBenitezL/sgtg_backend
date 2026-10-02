package com.sgtg.backend.infraestructure.input.perfil.dto.output;

import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.infraestructure.input.catalogo.dto.output.CatalogoDTOResponse;

public record PerfilEstudianteDTOResponse(Long usuarioId, CatalogoDTOResponse programa) {

    public static PerfilEstudianteDTOResponse desde(PerfilEstudiante perfil) {
        return new PerfilEstudianteDTOResponse(perfil.getUsuarioId(), CatalogoDTOResponse.desde(perfil.getPrograma()));
    }
}
