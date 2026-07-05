package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.mapper.MapperRoleAuth;
import com.sgtg.backend.infraestructure.mapper.MapperUsuarioAuth;
import com.sgtg.backend.infraestructure.output.percistence.repositories.RoleRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

/**
 * Clase adaptadora para la implementación de la interfaz
 * ManageAuthGatewayIntPort, que se encargará de interactuar con el servicio de
 * persistencia de datos relacionado con autenticación. Esta clase implementará
 * los métodos definidos en la interfaz y utilizará los repositorios y
 * mapeadores necesarios para realizar las operaciones de búsqueda y registro de
 * usuarios.
 */
@Service
public class ManageAuthGatewayImplAdapter implements ManageAuthGatewayIntPort {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public ManageAuthGatewayImplAdapter(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Usuario findByEmail(String email) {
        return MapperUsuarioAuth.toDomain(userRepository.findByEmail(email).orElse(null));
    }

    @Override
    public boolean register(Usuario user) {
        return this.userRepository.save(MapperUsuarioAuth.toEntity(user)) != null;
    }

    @Override
    public boolean existsByEmailorCodigo(String email, String codigo) {
        return this.existsByEmailorCodigo(email, codigo);
    }

    @Override
    public List<Role> findRolesByTrole(List<String> trole) {
        return MapperRoleAuth.toDomain(this.roleRepository.findByTroleIn(trole));
    }

}
