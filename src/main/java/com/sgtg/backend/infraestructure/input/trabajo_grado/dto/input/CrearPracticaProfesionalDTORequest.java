package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.util.List;

import com.sgtg.backend.application.input.trabajo_grado.NuevoTrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Nueva práctica profesional (individual). El director es el docente
 * autenticado.
 *
 * @param urlCartaAceptacion URL de la carta de aceptación ya cargada al servidor de archivos
 */
public record CrearPracticaProfesionalDTORequest(
        @NotBlank(message = "El título es obligatorio") String titulo,
        @NotEmpty(message = "Se debe indicar al menos un objetivo") List<String> objetivos,
        @NotNull(message = "El estudiante es obligatorio") Long estudianteId,
        Long codirectorId,
        @NotNull(message = "El asesor de la organización es obligatorio") @Valid AsesorDTORequest asesor,
        @NotBlank(message = "La carta de aceptación es obligatoria") String urlCartaAceptacion) {

    public NuevoTrabajoGrado toNuevoTrabajo() {
        return new NuevoTrabajoGrado(Modalidad.PRACTICA_PROFESIONAL, titulo, objetivos, List.of(estudianteId),
                codirectorId, asesor.toDatos(), urlCartaAceptacion);
    }
}
