package com.sgtg.backend.infraestructure.output.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByTrole(RoleEntity.RoleType trole);
}
