package com.sgtg.backend.infraestructure.input.catalogo.dto.input;

import jakarta.validation.constraints.NotBlank;

/** Nombre de un nuevo departamento (el programa lleva además su departamento: {@link ProgramaDTORequest}). */
public record NombreDTORequest(@NotBlank(message = "El nombre es obligatorio") String nombre) {
}
