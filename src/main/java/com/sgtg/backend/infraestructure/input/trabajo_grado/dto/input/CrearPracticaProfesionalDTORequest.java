package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.util.List;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;
import com.sgtg.backend.application.input.trabajo_grado.NuevoTrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Nueva práctica profesional (individual). El director es el docente
 * autenticado. Se envía como la parte {@code datos} de la petición; la carta
 * de aceptación va en la parte {@code archivo}.
 */
public record CrearPracticaProfesionalDTORequest(
        @NotBlank(message = "El título es obligatorio") String titulo,
        @NotEmpty(message = "Se debe indicar al menos un objetivo") List<String> objetivos,
        @NotNull(message = "El estudiante es obligatorio") Long estudianteId,
        Long codirectorId,
        @NotNull(message = "El asesor de la organización es obligatorio") @Valid AsesorDTORequest asesor) {

    /**
     * @param cartaAceptacion archivo de la carta de aceptación
     */
    public NuevoTrabajoGrado toNuevoTrabajo(ArchivoAdjunto cartaAceptacion) {
        return new NuevoTrabajoGrado(Modalidad.PRACTICA_PROFESIONAL, titulo, objetivos, List.of(estudianteId),
                codirectorId, asesor.toDatos(), cartaAceptacion);
    }
}
