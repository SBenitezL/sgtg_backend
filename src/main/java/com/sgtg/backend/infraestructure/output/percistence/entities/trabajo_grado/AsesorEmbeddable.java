package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Asesor de un trabajo de grado. Todas las columnas son opcionales: si el
 * trabajo no tiene asesor quedan en null. El usuario es null si es un asesor
 * externo sin cuenta.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsesorEmbeddable {
    @Column(name = "asesor_usuario_id")
    private Long usuarioId;
    @Column(name = "asesor_codigo")
    private String codigo;
    @Column(name = "asesor_primer_nombre")
    private String primerNombre;
    @Column(name = "asesor_segundo_nombre")
    private String segundoNombre;
    @Column(name = "asesor_primer_apellido")
    private String primerApellido;
    @Column(name = "asesor_segundo_apellido")
    private String segundoApellido;
    @Column(name = "asesor_email")
    private String email;
    @Column(name = "asesor_organizacion")
    private String organizacion;
    @Column(name = "asesor_externo")
    private Boolean externo;
}
