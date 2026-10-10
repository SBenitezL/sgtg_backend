package com.sgtg.backend.infraestructure.input.usuario.dto.output;

import java.util.List;

import com.sgtg.backend.application.input.usuario.FichaUsuario;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.infraestructure.input.catalogo.dto.output.CatalogoDTOResponse;

/**
 * Datos públicos de un usuario con sus perfiles académicos. Nunca incluye la
 * contraseña.
 *
 * @param activo          false si el usuario fue inactivado y no puede iniciar sesión
 * @param tipoVinculacion vinculación del perfil docente, null si no tiene
 * @param departamento    departamento del perfil docente, null si no tiene
 * @param programa        programa del perfil de estudiante, null si no tiene
 */
public record UsuarioDTOResponse(Long id, String codigo, TipoDocumentoIdentidad tipoDocumento,
        String numeroDocumento, String email, String primerNombre, String segundoNombre, String primerApellido,
        String segundoApellido, List<String> roles, boolean activo, TipoVinculacion tipoVinculacion,
        CatalogoDTOResponse departamento, CatalogoDTOResponse programa) {

    public static UsuarioDTOResponse desde(FichaUsuario ficha) {
        Usuario usuario = ficha.usuario();
        return new UsuarioDTOResponse(usuario.getCusuari(), usuario.getCodigo(), usuario.getTipoDocumento(),
                usuario.getNumeroDocumento(), usuario.getEmail(), usuario.getPrimerNombre(),
                usuario.getSegundoNombre(), usuario.getPrimerApellido(), usuario.getSegundoApellido(),
                usuario.getRole().stream().map(role -> role.getTrole()).toList(), usuario.isActivo(),
                ficha.perfilDocente() == null ? null : ficha.perfilDocente().getTipoVinculacion(),
                ficha.perfilDocente() == null ? null : CatalogoDTOResponse.desde(ficha.perfilDocente().getDepartamento()),
                ficha.perfilEstudiante() == null ? null
                        : CatalogoDTOResponse.desde(ficha.perfilEstudiante().getPrograma()));
    }

    /**
     * Copia sin el documento de identidad, para los usuarios que no son
     * administradores (lo usan para elegir estudiantes, directores, etc.).
     */
    public UsuarioDTOResponse sinDocumento() {
        return new UsuarioDTOResponse(id, codigo, null, null, email, primerNombre, segundoNombre, primerApellido,
                segundoApellido, roles, activo, tipoVinculacion, departamento, programa);
    }
}
