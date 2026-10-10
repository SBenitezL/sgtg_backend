package com.sgtg.backend.infraestructure.input.reloj.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** DTOs del reloj de desarrollo. */
public final class RelojDTO {

    private RelojDTO() {
    }

    /** @param dias días a adelantar el reloj, de 1 a 3650 */
    public record AvanzarDTORequest(
            @NotNull(message = "Los días son obligatorios") @Min(value = 1, message = "Debe avanzar al menos un día")
            @Max(value = 3650, message = "No se puede avanzar más de 3650 días") Integer dias) {
    }

    /**
     * @param fecha              fecha actual de la aplicación
     * @param fechaHora          fecha y hora actuales de la aplicación
     * @param desplazamientoDias días que el reloj está adelantado
     */
    public record RelojDTOResponse(LocalDate fecha, LocalDateTime fechaHora, long desplazamientoDias) {
    }

    /** @param actualizados trabajos que cambiaron de etapa */
    public record VencimientosDTOResponse(int actualizados) {
    }
}
