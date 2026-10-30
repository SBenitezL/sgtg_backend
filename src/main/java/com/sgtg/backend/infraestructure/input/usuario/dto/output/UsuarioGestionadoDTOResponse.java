package com.sgtg.backend.infraestructure.input.usuario.dto.output;

import com.sgtg.backend.application.input.usuario.UsuarioGestionado;

/**
 * Usuario de la gestión de usuarios, con sus datos completos (incluido el
 * documento) y si el gestor lo puede modificar.
 */
public record UsuarioGestionadoDTOResponse(UsuarioDTOResponse usuario, boolean modificable) {

    public static UsuarioGestionadoDTOResponse desde(UsuarioGestionado gestionado) {
        return new UsuarioGestionadoDTOResponse(UsuarioDTOResponse.desde(gestionado.ficha()),
                gestionado.modificable());
    }
}
