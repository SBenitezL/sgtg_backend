package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Informe mensual de una práctica profesional: solo se guarda la ruta del documento. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformeMensualEmbeddable {
    @Column(nullable = false)
    private Integer numero;
    @Column(length = 1000)
    private String rutaArchivo;
    private LocalDate fecha;
}
