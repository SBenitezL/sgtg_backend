package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.infraestructure.output.percistence.entities.HistorialPasswordEntity;

public interface HistorialPasswordRepository extends JpaRepository<HistorialPasswordEntity, Long> {

    /** Contraseñas anteriores del usuario, de la más reciente a la más antigua */
    List<HistorialPasswordEntity> findByUsuarioIdOrderByFechaDescIdDesc(Long usuarioId);
}
