package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Revisión del paz y salvo (formato F) que registra Decanatura. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevisionPazYSalvoEmbeddable {
    @Column(length = 1000, nullable = false)
    private String rutaDocumento;
    @Column(nullable = false)
    private boolean aprobado;
    @Column(length = 2000)
    private String observaciones;
    private LocalDate fecha;
}
