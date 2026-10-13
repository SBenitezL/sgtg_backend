package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Versión de la propuesta: la ruta del documento y el estado de su revisión. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionDocumentoEmbeddable {
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipo;
    @Column(length = 1000)
    private String url;
    private Integer version;
    private LocalDate fechaCarga;
    @Enumerated(EnumType.STRING)
    private Estado estado;
}
