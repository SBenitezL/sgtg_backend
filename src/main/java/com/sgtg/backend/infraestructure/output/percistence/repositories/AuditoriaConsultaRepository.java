package com.sgtg.backend.infraestructure.output.percistence.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sgtg.backend.domain.models.auditoria.ResultadoAuditoria;
import com.sgtg.backend.infraestructure.output.percistence.entities.AuditoriaEntity;

/**
 * Consultas de la auditoría para la detección de alertas y la verificación de
 * la cadena. No tiene operaciones para modificar ni borrar, y ningún endpoint
 * la expone.
 */
public interface AuditoriaConsultaRepository extends Repository<AuditoriaEntity, Long> {

    @Query("SELECT a.ip FROM AuditoriaEntity a WHERE a.operacion = :login AND a.resultado <> :exitosa"
            + " AND a.fecha >= :desde AND a.ip IS NOT NULL GROUP BY a.ip HAVING COUNT(a) >= :minimo")
    List<String> ipsConLoginsFallidos(@Param("login") String login, @Param("desde") LocalDateTime desde,
            @Param("minimo") long minimo, @Param("exitosa") ResultadoAuditoria exitosa);

    @Query("SELECT a.usuario FROM AuditoriaEntity a WHERE a.operacion = :login AND a.resultado <> :exitosa"
            + " AND a.fecha >= :desde AND a.usuario IS NOT NULL GROUP BY a.usuario HAVING COUNT(a) >= :minimo")
    List<String> correosConLoginsFallidos(@Param("login") String login, @Param("desde") LocalDateTime desde,
            @Param("minimo") long minimo, @Param("exitosa") ResultadoAuditoria exitosa);

    @Query("SELECT a.usuario FROM AuditoriaEntity a WHERE a.resultado = :resultado AND a.fecha >= :desde"
            + " AND a.usuario IS NOT NULL GROUP BY a.usuario HAVING COUNT(a) >= :minimo")
    List<String> usuariosConResultado(@Param("desde") LocalDateTime desde, @Param("minimo") long minimo,
            @Param("resultado") ResultadoAuditoria resultado);

    /** Sesiones de tokens con firma válida que no autenticaron (el filtro JWT solo pone la sesión en esos) */
    @Query("SELECT DISTINCT a.sesion FROM AuditoriaEntity a WHERE a.resultado = :noAutenticado"
            + " AND a.fecha >= :desde AND a.sesion IS NOT NULL AND a.usuarioId IS NULL")
    List<String> sesionesRechazadas(@Param("desde") LocalDateTime desde,
            @Param("noAutenticado") ResultadoAuditoria noAutenticado);

    /** Registros sellados después de una secuencia, en orden */
    @Query("SELECT a FROM AuditoriaEntity a WHERE a.secuencia > :despuesDe ORDER BY a.secuencia")
    List<AuditoriaEntity> selladosDespuesDe(@Param("despuesDe") long despuesDe, Pageable pagina);

    /** Registros sin sello guardados después del primero sellado: alguien les quitó la secuencia */
    @Query("SELECT COUNT(a) FROM AuditoriaEntity a WHERE a.secuencia IS NULL AND a.id >"
            + " (SELECT MIN(b.id) FROM AuditoriaEntity b WHERE b.secuencia IS NOT NULL)")
    long sinSelloDentroDeLaCadena();
}
