package com.sgtg.backend.infraestructure.output.percistence.repositories;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity.RoleType;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

/**
 * Repositorio para la entidad UserEntity, que proporciona operaciones de acceso
 * a datos para los usuarios.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmailOrCodigo(String email, String codigo);

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumentoIdentidad tipoDocumento, String numeroDocumento);

    boolean existsByEmailAndCusuariNot(String email, Long cusuari);

    boolean existsByCodigoAndCusuariNot(String codigo, Long cusuari);

    boolean existsByTipoDocumentoAndNumeroDocumentoAndCusuariNot(TipoDocumentoIdentidad tipoDocumento,
            String numeroDocumento, Long cusuari);

    /** Usuarios activos que tienen el rol */
    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.role r WHERE r.trole = :rol AND u.activo = true")
    List<UserEntity> activosConRol(@Param("rol") RoleType rol);
}
