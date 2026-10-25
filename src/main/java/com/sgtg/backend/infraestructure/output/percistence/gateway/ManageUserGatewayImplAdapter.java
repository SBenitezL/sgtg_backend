package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.mapper.MapperRoleAuth;
import com.sgtg.backend.infraestructure.mapper.MapperUsuarioAuth;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity.RoleType;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.RoleRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageUserGatewayImplAdapter implements ManageUserGatewayIntPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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
        // Lista modificable: Hibernate la vacía y la vuelve a llenar al guardar
        entity.setRole(new ArrayList<>(usuario.getRole().stream().map(MapperRoleAuth::toEntity).toList()));
        userRepository.save(entity);
    }

    @Override
    @Transactional
    public void agregarRol(Long usuarioId, String rol) {
        UserEntity entity = userRepository.findById(usuarioId).orElseThrow();
        RoleType tipo = RoleType.valueOf(rol);
        if (entity.getRole().stream().anyMatch(actual -> actual.getTrole() == tipo))
            return;
        List<RoleEntity> roles = new ArrayList<>(entity.getRole());
        roles.add(roleRepository.findByTrole(tipo).orElseThrow());
        entity.setRole(roles);
        userRepository.save(entity);
    }

    @Override
    @Transactional
    public void quitarRol(Long usuarioId, String rol) {
        UserEntity entity = userRepository.findById(usuarioId).orElse(null);
        if (entity == null)
            return;
        RoleType tipo = RoleType.valueOf(rol);
        List<RoleEntity> restantes = entity.getRole().stream().filter(actual -> actual.getTrole() != tipo).toList();
        if (restantes.size() == entity.getRole().size() || restantes.isEmpty())
            return;
        entity.setRole(new ArrayList<>(restantes));
        userRepository.save(entity);
    }

    @Override
    public List<Usuario> listar() {
        return userRepository.findAll(Sort.by("cusuari")).stream().map(MapperUsuarioAuth::toDomain).toList();
    }

    @Override
    @Transactional
    public void actualizarDatos(Usuario usuario) {
        UserEntity entity = userRepository.findById(usuario.getCusuari()).orElseThrow();
        entity.setCodigo(usuario.getCodigo());
        entity.setTipoDocumento(usuario.getTipoDocumento());
        entity.setNumeroDocumento(usuario.getNumeroDocumento());
        entity.setEmail(usuario.getEmail());
        entity.setPrimerNombre(usuario.getPrimerNombre());
        entity.setSegundoNombre(usuario.getSegundoNombre());
        entity.setPrimerApellido(usuario.getPrimerApellido());
        entity.setSegundoApellido(usuario.getSegundoApellido());
        // Lista modificable: Hibernate la vacía y la vuelve a llenar al guardar
        entity.setRole(new ArrayList<>(usuario.getRole().stream().map(MapperRoleAuth::toEntity).toList()));
        userRepository.save(entity);
    }

    @Override
    public boolean existeEmailOCodigoEnOtro(String email, String codigo, Long usuarioId) {
        return userRepository.existsByEmailAndCusuariNot(email, usuarioId)
                || userRepository.existsByCodigoAndCusuariNot(codigo, usuarioId);
    }

    @Override
    public boolean existeDocumentoEnOtro(TipoDocumentoIdentidad tipoDocumento, String numeroDocumento,
            Long usuarioId) {
        return userRepository.existsByTipoDocumentoAndNumeroDocumentoAndCusuariNot(tipoDocumento, numeroDocumento,
                usuarioId);
    }

    @Override
    @Transactional
    public void actualizarEstado(Usuario usuario) {
        UserEntity entity = userRepository.findById(usuario.getCusuari()).orElseThrow();
        entity.setActivo(usuario.isActivo());
        userRepository.save(entity);
    }
}
