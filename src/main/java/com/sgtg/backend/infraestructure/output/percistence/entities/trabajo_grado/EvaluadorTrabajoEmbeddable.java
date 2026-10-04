package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Evaluador asignado al anteproyecto, con la copia de sus datos al momento de la asignación. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluadorTrabajoEmbeddable {
    @Column(nullable = false)
    private Long usuarioId;
    private String codigo;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String email;
}
