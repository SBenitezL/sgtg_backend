package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.sgtg.backend.domain.models.auditoria.RegistroAuditoria;
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
 * actualiza) y sin operaciones de borrado en sus repositorios; para revisarlo
 * se consulta directamente la base de datos. Cada registro va sellado en una
 * cadena (secuencia, sello del anterior y sello propio); los registros de
 * antes de la cadena no tienen secuencia ni sello.
 */
@Entity
@Immutable
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha"),
        @Index(name = "idx_auditoria_usuario", columnList = "usuario_id"),
        @Index(name = "idx_auditoria_sesion", columnList = "sesion"),
        @Index(name = "idx_auditoria_ip", columnList = "ip"),
        @Index(name = "idx_auditoria_solicitud", columnList = "solicitud"),
        @Index(name = "uk_auditoria_secuencia", columnList = "secuencia", unique = true)
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

    /** Identificador de la petición (X-Request-Id), el mismo de los logs */
    @Column(length = 64)
    private String solicitud;

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

    /** Qué cambió (antes → después) y los archivos con su huella */
    @Column(length = 4000)
    private String detalle;

    /** Número del registro en la cadena (1, 2, 3...), sin huecos */
    private Long secuencia;

    /** Sello del registro anterior de la cadena */
    @Column(length = 64)
    private String selloAnterior;

    /** HMAC-SHA256 de la secuencia, el sello anterior y los datos del registro */
    @Column(length = 64)
    private String sello;

    public static AuditoriaEntity sellado(RegistroAuditoria r, long secuencia, String selloAnterior, String sello) {
        return new AuditoriaEntity(null, r.fecha(), r.usuarioId(), r.usuario(), r.roles(), r.sesion(), r.solicitud(),
                r.ip(), r.agenteUsuario(), r.metodo(), r.ruta(), r.operacion(), r.trabajoId(), r.parametros(),
                r.estadoHttp(), r.resultado(), r.detalle(), secuencia, selloAnterior, sello);
    }

    /** Los datos del registro, para recalcular su sello. */
    public RegistroAuditoria registro() {
        return new RegistroAuditoria(fecha, usuarioId, usuario, roles, sesion, solicitud, ip, agenteUsuario, metodo,
                ruta, operacion, trabajoId, parametros, estadoHttp, resultado, detalle);
    }
}
