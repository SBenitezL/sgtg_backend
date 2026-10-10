package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inasistencia o verificación de ajustes registrada por un jurado; el jurado
 * se resuelve contra los de su sesión.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroJuradoEmbeddable {
    @Column(nullable = false)
    private Integer sesion;
    @Column(nullable = false)
    private Long juradoUsuarioId;
    private Boolean favorable;
    @Column(length = 2000)
    private String observaciones;
    private LocalDate fecha;
}
