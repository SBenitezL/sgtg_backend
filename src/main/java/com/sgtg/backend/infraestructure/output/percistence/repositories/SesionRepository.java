package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sgtg.backend.infraestructure.output.percistence.entities.SesionEntity;

import jakarta.persistence.LockModeType;

public interface SesionRepository extends JpaRepository<SesionEntity, String> {

    /** La sesión bloqueada hasta que termine la transacción: dos renovaciones simultáneas van en orden */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SesionEntity s WHERE s.id = :id")
    Optional<SesionEntity> bloquear(@Param("id") String id);

    /** Cierra las sesiones abiertas del usuario, salvo la indicada (null: todas) */
    @Modifying
    @Query("UPDATE SesionEntity s SET s.revocada = :ahora WHERE s.usuarioId = :usuarioId AND s.revocada IS NULL"
            + " AND (:excepto IS NULL OR s.id <> :excepto)")
    int revocarDeUsuario(@Param("usuarioId") Long usuarioId, @Param("excepto") String excepto,
            @Param("ahora") LocalDateTime ahora);

    /** Borra las sesiones que ya no pueden usarse desde antes de la fecha */
    @Modifying
    @Query("DELETE FROM SesionEntity s WHERE s.creada < :creadaAntesDe OR s.ultimoUso < :usadaAntesDe"
            + " OR s.revocada < :usadaAntesDe")
    int depurar(@Param("creadaAntesDe") LocalDateTime creadaAntesDe, @Param("usadaAntesDe") LocalDateTime usadaAntesDe);
}
