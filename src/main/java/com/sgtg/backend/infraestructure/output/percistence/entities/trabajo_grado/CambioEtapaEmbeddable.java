package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDateTime;

import com.sgtg.backend.domain.models.trabajo_grado.Proceso;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entrada del historial de cambios de etapa. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioEtapaEmbeddable {
    @Enumerated(EnumType.STRING)
    private Proceso origen;
    @Enumerated(EnumType.STRING)
    private Proceso destino;
    private LocalDateTime fecha;
    private String responsable;
    @Column(length = 2000)
    private String observacion;
}
