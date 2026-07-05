package com.sgtg.backend.infraestructure.mapper;

import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.input.auth.dto.input.RegisterDTORequest;
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
                userEntity.getEmail(),
                userEntity.getPassword(),
                userEntity.getNombres(),
                userEntity.getApellidos(),
                userEntity.getRole().stream().map(role -> role.getTrole().name()).toList().stream()
                        .map(MapperRoleAuth::toDomain).toList());
    }

    /**
     * Mapea un RegisterDTORequest a un Usuario del dominio. Si el
     * RegisterDTORequest es null, devuelve null.
     * 
     * @param registerRequest El RegisterDTORequest a mapear
     * @return Un Usuario del dominio con los datos del RegisterDTORequest, o null
     *         si el RegisterDTORequest es null
     */
    public static Usuario toDomain(RegisterDTORequest registerRequest) {
        if (registerRequest == null) {
            return null;
        }
        return new Usuario(
                null,
                registerRequest.getCodigo(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getNombres(),
                registerRequest.getApellidos(),
                registerRequest.getRole().stream().map(MapperRoleAuth::toDomain).toList());
    }

    public static UserEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UserEntity.builder()
                .cusuari(usuario.getCusuari())
                .codigo(usuario.getCodigo())
                .email(usuario.getEmail())
                .password(usuario.getPassword())
                .nombres(usuario.getNombres())
                .apellidos(usuario.getApellidos())
                .role(usuario.getRole().stream().map(MapperRoleAuth::toEntity).toList())
                .build();
    }
}
