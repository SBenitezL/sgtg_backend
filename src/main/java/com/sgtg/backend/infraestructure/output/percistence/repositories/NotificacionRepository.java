package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.infraestructure.output.percistence.entities.NotificacionEntity;

public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {
    List<NotificacionEntity> findByUsuarioIdOrderByIdDesc(Long usuarioId);

    boolean existsByUsuarioIdAndClave(Long usuarioId, String clave);

    @Modifying
    @Transactional
    @Query("update NotificacionEntity n set n.leida = true where n.usuarioId = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Transactional
    @Query("delete from NotificacionEntity n where n.usuarioId = :usuarioId")
    int eliminarPorUsuario(@Param("usuarioId") Long usuarioId);
}
