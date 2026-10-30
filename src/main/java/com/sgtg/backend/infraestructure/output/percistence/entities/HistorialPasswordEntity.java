package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Hashes de las contraseñas anteriores de un usuario, para que no las repita. */
@Entity
@Table(name = "historial_passwords", indexes = @Index(name = "idx_historial_passwords_usuario", columnList = "usuario_id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPasswordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Solo para la llave foránea a users; no se carga */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    @Getter(lombok.AccessLevel.NONE)
    private UserEntity usuario;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    /** Hasta cuándo fue la contraseña vigente */
    @Column(nullable = false)
    private LocalDateTime fecha;

    public HistorialPasswordEntity(Long usuarioId, String passwordHash, LocalDateTime fecha) {
        this(null, usuarioId, null, passwordHash, fecha);
    }
}
