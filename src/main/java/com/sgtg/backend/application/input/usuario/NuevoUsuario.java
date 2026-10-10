package com.sgtg.backend.application.input.usuario;

import java.util.List;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;

/**
 * Datos para crear o actualizar un usuario.
 *
 * @param numeroDocumento solo dígitos
 * @param password        al crear es obligatoria; al actualizar, vacía conserva la actual
 * @param segundoNombre   opcional
 * @param segundoApellido opcional
 * @param roles           nombres de los roles a asignar, al menos uno
 * @param perfil          perfil académico a registrar, {@link DatosPerfil#NINGUNO} si no tiene
 */
public record NuevoUsuario(String codigo, TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
        String email, String password, String primerNombre, String segundoNombre, String primerApellido,
        String segundoApellido, List<String> roles, DatosPerfil perfil) {

    public NuevoUsuario {
        perfil = perfil == null ? DatosPerfil.NINGUNO : perfil;
    }
}
