package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.models.notificacion.EventoTrabajoGrado;
import com.sgtg.backend.domain.models.notificacion.TipoNotificacion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;

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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Notificación del buzón de un usuario. */
@Entity
@Table(name = "notificaciones", indexes = {
        @Index(name = "idx_notificaciones_usuario", columnList = "usuarioId"),
        @Index(name = "idx_notificaciones_usuario_clave", columnList = "usuarioId, clave") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    private Long trabajoId;

    @Enumerated(EnumType.STRING)
    private Proceso etapa;

    @Enumerated(EnumType.STRING)
    private EventoTrabajoGrado evento;

    private String clave;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean leida;

    /** Sesión de sustentación a la que se refiere, null si no aplica */
    private Integer sesion;
}
