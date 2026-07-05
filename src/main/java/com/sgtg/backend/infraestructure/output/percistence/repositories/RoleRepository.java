package com.sgtg.backend.infraestructure.output.percistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByTrole(RoleEntity.RoleType trole);

    List<RoleEntity> findByTroleIn(List<String> trole);
}
