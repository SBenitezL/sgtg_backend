package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Solicitud del director para cambiar el título u objetivos del trabajo en curso.
 *
 * @param titulo        título propuesto (puede ser el mismo si solo cambian los objetivos)
 * @param objetivos     objetivos propuestos
 * @param justificacion motivo del cambio
 */
public record SolicitudCambioObjetivosDTORequest(
        @NotBlank(message = "El título es obligatorio") String titulo,
        @NotEmpty(message = "Debe indicar al menos un objetivo") List<String> objetivos,
        @NotBlank(message = "La justificación es obligatoria") String justificacion) {
}
