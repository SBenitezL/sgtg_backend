package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Credenciales de un usuario, separadas de sus datos (tabla {@code users}): los
 * listados y consultas de usuarios nunca cargan el hash, y en la base de datos
 * se puede negar la lectura de esta tabla a quien no la necesite.
 */
@Entity
@Table(name = "credenciales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredencialEntity {

    /** Identificador del usuario: lo toma de {@link #usuario} al guardar */
    @Id
    private Long usuarioId;

    /** Usuario dueño de las credenciales (llave foránea a users); no se carga al consultarlas */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private UserEntity usuario;

    /** Hash BCrypt de la contraseña */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime fechaCambio;

    /** La contraseña la asignó un administrador: el usuario debe cambiarla antes de usar el sistema */
    @Column(nullable = false)
    private boolean debeCambiar;

    /**
     * Versión de las credenciales; va dentro de cada token. Aumenta al cambiar la
     * contraseña, y así los tokens emitidos antes dejan de servir.
     */
    @Column(nullable = false)
    private int version;
}
