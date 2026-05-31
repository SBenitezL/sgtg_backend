package com.sgtg.backend.infraestructure.output.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

/**
 * Repositorio para la entidad UserEntity, que proporciona operaciones de acceso
 * a datos para los usuarios.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

}
