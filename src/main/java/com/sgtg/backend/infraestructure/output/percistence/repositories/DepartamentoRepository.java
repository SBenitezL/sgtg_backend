package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.infraestructure.output.percistence.entities.DepartamentoEntity;

public interface DepartamentoRepository extends JpaRepository<DepartamentoEntity, Long> {
    boolean existsByNombreIgnoreCase(String nombre);

    List<DepartamentoEntity> findAllByOrderByNombreAsc();
}
