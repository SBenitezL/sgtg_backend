package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sesión de sustentación con sus dos jurados (columnas jurado1_* y jurado2_*). */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionSustentacionEmbeddable {
    @Column(nullable = false)
    private Integer numero;
    private LocalDateTime fechaHora;
    private String salon;
    private LocalDate fechaProgramacion;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "usuarioId", column = @Column(name = "jurado1_usuario_id")),
            @AttributeOverride(name = "codigo", column = @Column(name = "jurado1_codigo")),
            @AttributeOverride(name = "primerNombre", column = @Column(name = "jurado1_primer_nombre")),
            @AttributeOverride(name = "segundoNombre", column = @Column(name = "jurado1_segundo_nombre")),
            @AttributeOverride(name = "primerApellido", column = @Column(name = "jurado1_primer_apellido")),
            @AttributeOverride(name = "segundoApellido", column = @Column(name = "jurado1_segundo_apellido")),
            @AttributeOverride(name = "email", column = @Column(name = "jurado1_email")) })
    private EvaluadorTrabajoEmbeddable jurado1;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "usuarioId", column = @Column(name = "jurado2_usuario_id")),
            @AttributeOverride(name = "codigo", column = @Column(name = "jurado2_codigo")),
            @AttributeOverride(name = "primerNombre", column = @Column(name = "jurado2_primer_nombre")),
            @AttributeOverride(name = "segundoNombre", column = @Column(name = "jurado2_segundo_nombre")),
            @AttributeOverride(name = "primerApellido", column = @Column(name = "jurado2_primer_apellido")),
            @AttributeOverride(name = "segundoApellido", column = @Column(name = "jurado2_segundo_apellido")),
            @AttributeOverride(name = "email", column = @Column(name = "jurado2_email")) })
    private EvaluadorTrabajoEmbeddable jurado2;
}
