package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.trabajo_grado.Calificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Calificación de un jurado; el jurado se resuelve contra los de su sesión. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalificacionJuradoEmbeddable {
    @Column(nullable = false)
    private Integer sesion;
    @Column(nullable = false)
    private Long juradoUsuarioId;
    @Enumerated(EnumType.STRING)
    private Calificacion calificacion;
    @Column(length = 2000)
    private String observaciones;
    @Column(length = 1000)
    private String rutaActa;
    private LocalDate fecha;
}
