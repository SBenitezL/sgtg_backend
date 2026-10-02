package com.sgtg.backend.infraestructure.input.perfil.dto.input;

import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

import jakarta.validation.constraints.NotNull;

/** Vinculación y departamento de un docente. */
public record PerfilDocenteDTORequest(
        @NotNull(message = "La vinculación es obligatoria") TipoVinculacion tipoVinculacion,
        @NotNull(message = "El departamento es obligatorio") Long departamentoId) {
}
