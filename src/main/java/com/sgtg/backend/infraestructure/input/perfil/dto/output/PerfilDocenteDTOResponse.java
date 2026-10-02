package com.sgtg.backend.infraestructure.input.perfil.dto.output;

import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.infraestructure.input.catalogo.dto.output.CatalogoDTOResponse;

public record PerfilDocenteDTOResponse(Long usuarioId, TipoVinculacion tipoVinculacion,
        CatalogoDTOResponse departamento) {

    public static PerfilDocenteDTOResponse desde(PerfilDocente perfil) {
        return new PerfilDocenteDTOResponse(perfil.getUsuarioId(), perfil.getTipoVinculacion(),
                CatalogoDTOResponse.desde(perfil.getDepartamento()));
    }
}
