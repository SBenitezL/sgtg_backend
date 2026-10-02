package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.TrabajoGradoEntity;

public interface TrabajoGradoRepository extends JpaRepository<TrabajoGradoEntity, Long> {

    /** Trabajos en los que el usuario es estudiante, director, codirector o asesor, del más reciente al más antiguo. */
    @Query("""
            select distinct t from TrabajoGradoEntity t
            left join t.estudiantes e
            left join t.direccion d
            where e.usuarioId = :usuarioId or d.usuarioId = :usuarioId or t.asesor.usuarioId = :usuarioId
            order by t.id desc
            """)
    List<TrabajoGradoEntity> findByParticipante(@Param("usuarioId") Long usuarioId);
}
