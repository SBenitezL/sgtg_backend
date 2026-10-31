package com.sgtg.backend.infraestructure.output.percistence.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.infraestructure.output.percistence.entities.HuellaArchivoEntity;

public interface HuellaArchivoRepository extends JpaRepository<HuellaArchivoEntity, String> {
}
