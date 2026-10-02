package com.sgtg.backend.application.input.usuario;

import java.util.List;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;

/**
 * Datos para crear un usuario.
 *
 * @param numeroDocumento solo dígitos
 * @param segundoNombre   opcional
 * @param segundoApellido opcional
 * @param roles           nombres de los roles a asignar, al menos uno
 */
public record NuevoUsuario(String codigo, TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
        String email, String password, String primerNombre, String segundoNombre, String primerApellido,
        String segundoApellido, List<String> roles) {
}
