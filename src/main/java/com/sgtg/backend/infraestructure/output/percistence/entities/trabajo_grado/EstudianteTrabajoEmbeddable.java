package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estudiante de un trabajo de grado, con la copia de sus datos y programa al
 * momento de la asignación.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstudianteTrabajoEmbeddable {
    @Column(nullable = false)
    private Long usuarioId;
    private String codigo;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String email;
    private Long programaId;
    private String programaNombre;
}
