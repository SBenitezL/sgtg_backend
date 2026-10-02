package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Versión de un documento: solo se guarda la URL del servidor de archivos. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoEmbeddable {
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipo;
    @Column(length = 1000)
    private String url;
    private Integer version;
    private LocalDate fechaCarga;
}
