package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.infraestructure.output.percistence.entities.DiaNoHabilEntity;

public interface DiaNoHabilRepository extends JpaRepository<DiaNoHabilEntity, LocalDate> {
    List<DiaNoHabilEntity> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}
