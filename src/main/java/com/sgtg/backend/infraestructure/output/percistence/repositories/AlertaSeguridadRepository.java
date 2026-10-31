package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sgtg.backend.domain.models.auditoria.TipoAlerta;
import com.sgtg.backend.infraestructure.output.percistence.entities.AlertaSeguridadEntity;

public interface AlertaSeguridadRepository extends JpaRepository<AlertaSeguridadEntity, Long> {

    boolean existsByTipoAndClaveAndFechaAfter(TipoAlerta tipo, String clave, LocalDateTime desde);
}
