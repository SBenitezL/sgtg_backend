package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.sgtg.backend.domain.models.auditoria.TipoAlerta;

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

/** Alerta de seguridad avisada a los administradores. */
@Entity
@Table(name = "alertas_seguridad", indexes = @Index(name = "idx_alertas_tipo_clave", columnList = "tipo, clave"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AlertaSeguridadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private TipoAlerta tipo;

    @Column(nullable = false, length = 255)
    private String clave;

    @Column(nullable = false, length = 1000)
    private String detalle;
}
