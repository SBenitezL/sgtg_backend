package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Concepto de un evaluador sobre una versión del anteproyecto. El evaluador se
 * referencia por su usuario y se resuelve contra los evaluadores asignados.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConceptoEvaluacionEmbeddable {
    /** Versión del anteproyecto evaluada */
    @Column(nullable = false)
    private Integer version;
    @Column(nullable = false)
    private Long evaluadorUsuarioId;
    @Enumerated(EnumType.STRING)
    private ResultadoConcepto resultado;
    @Column(length = 2000)
    private String observaciones;
    private LocalDate fecha;
    /** URL del formato B, null si no se cargó */
    @Column(length = 1000)
    private String formatoUrl;
}
