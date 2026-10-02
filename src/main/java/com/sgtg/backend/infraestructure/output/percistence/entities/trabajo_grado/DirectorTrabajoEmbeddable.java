package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Director o codirector de un trabajo de grado, con la copia de su
 * vinculación y departamento al momento de la asignación.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectorTrabajoEmbeddable {

    /** Papel en el trabajo */
    public enum RolDireccion {
        DIRECTOR,
        CODIRECTOR
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolDireccion rol;
    @Column(nullable = false)
    private Long usuarioId;
    private String codigo;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String email;
    @Enumerated(EnumType.STRING)
    private TipoVinculacion tipoVinculacion;
    private Long departamentoId;
    private String departamentoNombre;
}
