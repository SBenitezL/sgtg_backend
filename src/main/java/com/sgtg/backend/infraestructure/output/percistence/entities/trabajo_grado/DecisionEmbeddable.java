package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Decisión de un órgano colegiado. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEmbeddable {
    @Enumerated(EnumType.STRING)
    private Organo organo;
    @Enumerated(EnumType.STRING)
    private ResultadoConcepto resultado;
    private String acta;
    @Column(length = 1000)
    private String rutaArchivo;
    @Column(length = 2000)
    private String observaciones;
    private LocalDate fecha;
}
