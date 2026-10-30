package com.sgtg.backend.infraestructure.output.percistence.repositories;

import org.springframework.data.repository.Repository;

import com.sgtg.backend.infraestructure.output.percistence.entities.AuditoriaEntity;

/**
 * Repositorio de la auditoría: solo guarda. No extiende {@code JpaRepository}
 * para que no existan métodos de consulta, actualización ni borrado.
 */
public interface AuditoriaRepository extends Repository<AuditoriaEntity, Long> {

    AuditoriaEntity save(AuditoriaEntity registro);
}
