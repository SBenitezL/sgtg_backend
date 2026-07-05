package com.sgtg.backend.infraestructure.mapper;

import java.util.ArrayList;
import java.util.List;

import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity.RoleType;

public abstract class MapperRoleAuth {

    public static Role toDomain(String role) {
        if (role == null) {
            return null;
        }
        return new Role(null, Role.RoleAdmited.valueOf(role));
    }

    public static List<Role> toDomain(List<RoleEntity> role) {
        List<Role> response = new ArrayList<>();
        for (RoleEntity current : role) {
            response.add(new Role(current.getCrole(), RoleAdmited.valueOf(current.getTrole().name())));
        }
        return response;
    }

    public static RoleType toEntity(String role) {
        if (role == null) {
            return null;
        }
        return RoleType.valueOf(role);
    }

    public static RoleEntity toEntity(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleEntity(role.getCrole(), toEntity(role.getTrole()));
    }

}
