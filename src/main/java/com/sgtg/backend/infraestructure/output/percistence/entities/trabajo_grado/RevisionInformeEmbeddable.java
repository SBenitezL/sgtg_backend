package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Revisión de un informe mensual por el director, el codirector o el asesor. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevisionInformeEmbeddable {
    /** Consecutivo del informe revisado */
    @Column(nullable = false)
    private Integer informeNumero;
    @Column(nullable = false)
    private Long revisorUsuarioId;
    private String revisor;
    @Column(length = 2000)
    private String observaciones;
    /** Ruta del PDF con las notas, null si no se adjuntó */
    @Column(length = 1000)
    private String rutaArchivo;
    private LocalDate fecha;
}
