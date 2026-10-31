package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Huella SHA-256 de un archivo guardado, calculada al guardarlo. Si el archivo
 * cambia en el disco, deja de coincidir.
 */
@Entity
@Table(name = "huellas_archivos")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HuellaArchivoEntity {

    /** Ruta relativa al directorio de los archivos */
    @Id
    @Column(length = 500)
    private String ruta;

    /** SHA-256 en hexadecimal */
    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false)
    private long tamano;

    /** Cuándo se calculó */
    @Column(nullable = false)
    private LocalDateTime fecha;
}
