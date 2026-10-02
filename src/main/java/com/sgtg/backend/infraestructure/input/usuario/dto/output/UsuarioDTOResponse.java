package com.sgtg.backend.infraestructure.input.usuario.dto.output;

import java.util.List;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Datos públicos de un usuario. Nunca incluye la contraseña.
 */
public record UsuarioDTOResponse(Long id, String codigo, TipoDocumentoIdentidad tipoDocumento,
        String numeroDocumento, String email, String primerNombre, String segundoNombre, String primerApellido,
        String segundoApellido, List<String> roles) {

    public static UsuarioDTOResponse desde(Usuario usuario) {
        return new UsuarioDTOResponse(usuario.getCusuari(), usuario.getCodigo(), usuario.getTipoDocumento(),
                usuario.getNumeroDocumento(), usuario.getEmail(), usuario.getPrimerNombre(),
                usuario.getSegundoNombre(), usuario.getPrimerApellido(), usuario.getSegundoApellido(),
                usuario.getRole().stream().map(role -> role.getTrole()).toList());
    }
}
