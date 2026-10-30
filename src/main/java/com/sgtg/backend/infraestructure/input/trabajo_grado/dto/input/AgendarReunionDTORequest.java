package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reunión de seguimiento que agenda (o reprograma) el director.
 *
 * @param fechaHora fecha y hora de la reunión (ej. 2027-05-12T10:00:00)
 * @param lugar     salón o enlace de la reunión
 */
public record AgendarReunionDTORequest(
        @NotNull(message = "La fecha y hora de la reunión son obligatorias") LocalDateTime fechaHora,
        @NotBlank(message = "El lugar o enlace de la reunión es obligatorio")
        @Size(max = 500, message = "El lugar o enlace admite máximo 500 caracteres") String lugar) {
}
