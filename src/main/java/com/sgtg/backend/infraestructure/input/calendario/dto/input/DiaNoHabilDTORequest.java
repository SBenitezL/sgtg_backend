package com.sgtg.backend.infraestructure.input.calendario.dto.input;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Día no hábil institucional a registrar (fecha en formato AAAA-MM-DD). */
public record DiaNoHabilDTORequest(@NotNull(message = "La fecha es obligatoria") LocalDate fecha,
        @NotBlank(message = "La descripción es obligatoria") String descripcion) {
}
