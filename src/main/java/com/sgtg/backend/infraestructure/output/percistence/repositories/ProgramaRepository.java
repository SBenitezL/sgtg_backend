package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.infraestructure.output.percistence.entities.ProgramaEntity;

public interface ProgramaRepository extends JpaRepository<ProgramaEntity, Long> {
    boolean existsByNombreIgnoreCase(String nombre);

    List<ProgramaEntity> findAllByOrderByNombreAsc();
}
