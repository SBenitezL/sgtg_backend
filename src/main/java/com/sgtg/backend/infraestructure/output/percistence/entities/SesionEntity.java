package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.models.acceso.SesionUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sesión de un usuario ({@link SesionUsuario}). Del token de renovación solo
 * se guarda su huella SHA-256: con la base no se puede renovar ninguna sesión.
 */
@Entity
@Table(name = "sesiones", indexes = @Index(name = "idx_sesiones_usuario", columnList = "usuario_id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SesionEntity {

    /** Identificador de la sesión: el mismo de la columna {@code sesion} de la auditoría */
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private int versionCredencial;

    @Column(nullable = false)
    private LocalDateTime creada;

    @Column(nullable = false)
    private LocalDateTime ultimoUso;

    @Column(nullable = false, length = 64)
    private String huellaActual;

    @Column(length = 64)
    private String huellaAnterior;

    private LocalDateTime rotada;

    /** Cuándo se cerró (por el usuario, por un cambio de contraseña o por reutilizar un token); null si sigue */
    private LocalDateTime revocada;

    public static SesionEntity desde(SesionUsuario s) {
        return new SesionEntity(s.getId(), s.getUsuarioId(), s.getVersionCredencial(), s.getCreada(),
                s.getUltimoUso(), s.getHuellaActual(), s.getHuellaAnterior(), s.getRotada(), s.getRevocada());
    }

    public SesionUsuario aDominio() {
        return new SesionUsuario(id, usuarioId, versionCredencial, creada, ultimoUso, huellaActual, huellaAnterior,
                rotada, revocada);
    }
}
