package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sgtg.backend.infraestructure.output.percistence.entities.AuditoriaCadenaEntity;

import jakarta.persistence.LockModeType;

public interface AuditoriaCadenaRepository extends JpaRepository<AuditoriaCadenaEntity, Long> {

    /** El final de la cadena, bloqueado hasta que termine la transacción (SELECT ... FOR UPDATE) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM AuditoriaCadenaEntity c WHERE c.id = :id")
    Optional<AuditoriaCadenaEntity> bloquear(@Param("id") Long id);
}
