package com.sgtg.backend.infraestructure.output.percistence.gateway;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.mapper.MapperRoleAuth;
import com.sgtg.backend.infraestructure.mapper.MapperUsuarioAuth;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageUserGatewayImplAdapter implements ManageUserGatewayIntPort {

    private final UserRepository userRepository;

    @Override
    public Usuario findByEmail(String email) {
        return MapperUsuarioAuth.toDomain(userRepository.findByEmail(email).orElse(null));
    }

    @Override
    public Usuario findById(Long usuarioId) {
        if (usuarioId == null)
            return null;
        return MapperUsuarioAuth.toDomain(userRepository.findById(usuarioId).orElse(null));
    }

    @Override
    @Transactional
    public void actualizarRoles(Usuario usuario) {
        UserEntity entity = userRepository.findById(usuario.getCusuari()).orElseThrow();
        entity.setRole(usuario.getRole().stream().map(MapperRoleAuth::toEntity).toList());
        userRepository.save(entity);
    }
}
