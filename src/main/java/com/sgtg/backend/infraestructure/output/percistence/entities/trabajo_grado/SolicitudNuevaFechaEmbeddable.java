package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud del director de una nueva fecha para la sesión de sustentación,
 * con la decisión del Consejo (null mientras esté pendiente).
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudNuevaFechaEmbeddable {
    @Column(nullable = false)
    private Integer sesion;
    @Column(length = 1000)
    private String rutaSolicitud;
    private LocalDate fechaSolicitud;
    private LocalDateTime fechaHoraAnterior;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "organo", column = @Column(name = "decision_organo")),
            @AttributeOverride(name = "resultado", column = @Column(name = "decision_resultado")),
            @AttributeOverride(name = "acta", column = @Column(name = "decision_acta")),
            @AttributeOverride(name = "rutaArchivo", column = @Column(name = "decision_ruta_archivo", length = 1000)),
            @AttributeOverride(name = "observaciones", column = @Column(name = "decision_observaciones", length = 2000)),
            @AttributeOverride(name = "fecha", column = @Column(name = "decision_fecha")) })
    private DecisionEmbeddable decision;
    private LocalDateTime nuevaFechaHora;
    private String nuevoSalon;
}
