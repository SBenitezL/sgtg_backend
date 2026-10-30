package com.sgtg.backend.infraestructure.mapper;

import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

/**
 * Clase abstracta para mapear Usuarios del servicio de autenticación entre las
 * diferentes capas de la aplicación.
 */
public abstract class MapperUsuarioAuth {
    /**
     * Mapea un UserEntity a un Usuario del dominio. Si el UserEntity es null,
     * devuelve null.
     * 
     * @param userEntity El UserEntity a mapear
     * @return Un Usuario del dominio con los datos del UserEntity, o null si el
     *         UserEntity es null
     */
    public static Usuario toDomain(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        return new Usuario(
                userEntity.getCusuari(),
                userEntity.getCodigo(),
                userEntity.getTipoDocumento(),
                userEntity.getNumeroDocumento(),
                userEntity.getEmail(),
                userEntity.getPrimerNombre(),
                userEntity.getSegundoNombre(),
                userEntity.getPrimerApellido(),
                userEntity.getSegundoApellido(),
                userEntity.getRole().stream().map(role -> role.getTrole().name()).toList().stream()
                        .map(MapperRoleAuth::toDomain).toList(),
                userEntity.isActivo());
    }

    /**
     * Mapea un Usuario del dominio a su entidad. Si el usuario es null, devuelve
     * null.
     */
    public static UserEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UserEntity.builder()
                .cusuari(usuario.getCusuari())
                .codigo(usuario.getCodigo())
                .tipoDocumento(usuario.getTipoDocumento())
                .numeroDocumento(usuario.getNumeroDocumento())
                .email(usuario.getEmail())
                .primerNombre(usuario.getPrimerNombre())
                .segundoNombre(usuario.getSegundoNombre())
                .primerApellido(usuario.getPrimerApellido())
                .segundoApellido(usuario.getSegundoApellido())
                .role(usuario.getRole().stream().map(MapperRoleAuth::toEntity).toList())
                .activo(usuario.isActivo())
                .build();
    }
}
