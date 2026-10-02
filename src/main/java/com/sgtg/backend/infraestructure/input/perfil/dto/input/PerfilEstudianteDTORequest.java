package com.sgtg.backend.infraestructure.input.perfil.dto.input;

import jakarta.validation.constraints.NotNull;

/** Programa académico de un estudiante. */
public record PerfilEstudianteDTORequest(@NotNull(message = "El programa es obligatorio") Long programaId) {
}
