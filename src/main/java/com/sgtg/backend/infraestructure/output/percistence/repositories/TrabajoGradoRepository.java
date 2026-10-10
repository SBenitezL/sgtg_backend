package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.TrabajoGradoEntity;

public interface TrabajoGradoRepository extends JpaRepository<TrabajoGradoEntity, Long> {

    /**
     * Trabajos en los que el usuario es estudiante, director, codirector, asesor,
     * evaluador o jurado, del más reciente al más antiguo.
     */
    @Query("""
            select distinct t from TrabajoGradoEntity t
            left join t.estudiantes e
            left join t.direccion d
            left join t.anteproyectoEvaluadores ev
            left join t.sustentacionSesiones ss
            where e.usuarioId = :usuarioId or d.usuarioId = :usuarioId or t.asesor.usuarioId = :usuarioId
               or ev.usuarioId = :usuarioId or ss.jurado1.usuarioId = :usuarioId or ss.jurado2.usuarioId = :usuarioId
            order by t.id desc
            """)
    List<TrabajoGradoEntity> findByParticipante(@Param("usuarioId") Long usuarioId);

    /** Trabajos en la etapa indicada, del más reciente al más antiguo. */
    List<TrabajoGradoEntity> findByEtapaOrderByIdDesc(Proceso etapa);

    List<TrabajoGradoEntity> findAllByOrderByIdDesc();
}
