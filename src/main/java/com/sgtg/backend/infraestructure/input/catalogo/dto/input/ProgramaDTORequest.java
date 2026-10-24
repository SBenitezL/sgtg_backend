package com.sgtg.backend.infraestructure.input.catalogo.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos de un nuevo programa académico.
 *
 * @param nombre         nombre del programa, único
 * @param departamentoId departamento al que está adscrito
 */
public record ProgramaDTORequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El departamento del programa es obligatorio") Long departamentoId) {
}
