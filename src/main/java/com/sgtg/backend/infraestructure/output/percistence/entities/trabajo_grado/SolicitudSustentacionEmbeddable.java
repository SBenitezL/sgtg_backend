package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Solicitud de sustentación anterior cuyo paz y salvo no aprobó Decanatura. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudSustentacionEmbeddable {
    @Column(length = 1000)
    private String rutaMonografia;
    @Column(length = 1000, nullable = false)
    private String rutaDocumentoFinal;
    @Column(length = 1000)
    private String rutaConceptoAsesor;
    @Column(length = 1000)
    private String rutaAnexos;
    @Column(nullable = false)
    private LocalDate fechaSolicitud;
}
