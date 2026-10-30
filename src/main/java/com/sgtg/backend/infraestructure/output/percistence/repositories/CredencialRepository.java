package com.sgtg.backend.infraestructure.output.percistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.infraestructure.output.percistence.entities.CredencialEntity;

public interface CredencialRepository extends JpaRepository<CredencialEntity, Long> {
}
