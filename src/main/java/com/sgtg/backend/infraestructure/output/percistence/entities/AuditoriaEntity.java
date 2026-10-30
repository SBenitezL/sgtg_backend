package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.sgtg.backend.domain.models.auditoria.ResultadoAuditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro de auditoría. Inmutable ({@link Immutable}: Hibernate nunca lo
 * actualiza) y sin operaciones de borrado en su repositorio; para revisarlo
 * se consulta directamente la base de datos.
 */
@Entity
@Immutable
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha"),
        @Index(name = "idx_auditoria_usuario", columnList = "usuario_id"),
        @Index(name = "idx_auditoria_sesion", columnList = "sesion"),
        @Index(name = "idx_auditoria_ip", columnList = "ip")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 255)
    private String usuario;

    @Column(length = 300)
    private String roles;

    @Column(length = 36)
    private String sesion;

    @Column(length = 64)
    private String ip;

    @Column(length = 500)
    private String agenteUsuario;

    @Column(nullable = false, length = 10)
    private String metodo;

    @Column(nullable = false, length = 1000)
    private String ruta;

    @Column(length = 200)
    private String operacion;

    private Long trabajoId;

    @Column(length = 500)
    private String parametros;

    @Column(nullable = false)
    private int estadoHttp;

    /** Texto y no enum de MySQL, para poder agregar resultados sin modificar la columna */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ResultadoAuditoria resultado;
}
