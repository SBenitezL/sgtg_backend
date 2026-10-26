package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Corrección de los datos del trabajo (los participantes no cambian): en la
 * propuesta, antes de la primera radicación o después de que el Comité no la
 * apruebe; en el anteproyecto, mientras se puede radicar una versión.
 */
public record ActualizarPropuestaDTORequest(
        @NotBlank(message = "El título es obligatorio") String titulo,
        @NotEmpty(message = "Se debe indicar al menos un objetivo") List<String> objetivos,
        @Size(max = 4000, message = "La descripción admite máximo 4000 caracteres") String descripcion,
        @Size(max = 300, message = "La línea de investigación admite máximo 300 caracteres") String lineaInvestigacion) {
}
