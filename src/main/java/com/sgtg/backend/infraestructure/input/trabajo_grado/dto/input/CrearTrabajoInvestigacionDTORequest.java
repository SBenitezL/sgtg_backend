package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import java.util.List;

import com.sgtg.backend.application.input.trabajo_grado.NuevoTrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Nuevo trabajo de investigación. El director es el docente autenticado.
 *
 * @param estudiantesIds usuarios de 1 o 2 estudiantes
 * @param codirectorId   usuario del codirector, opcional
 * @param asesor         asesor, opcional
 * @param descripcion        descripción general, opcional
 * @param lineaInvestigacion línea de investigación, opcional
 */
public record CrearTrabajoInvestigacionDTORequest(
        @NotBlank(message = "El título es obligatorio") String titulo,
        @NotEmpty(message = "Se debe indicar al menos un objetivo") List<String> objetivos,
        @NotEmpty(message = "Se debe indicar al menos un estudiante") List<Long> estudiantesIds,
        Long codirectorId,
        @Valid AsesorDTORequest asesor,
        @Size(max = 4000, message = "La descripción admite máximo 4000 caracteres") String descripcion,
        @Size(max = 300, message = "La línea de investigación admite máximo 300 caracteres") String lineaInvestigacion) {

    public NuevoTrabajoGrado toNuevoTrabajo() {
        return new NuevoTrabajoGrado(Modalidad.TRABAJO_DE_INVESTIGACION, titulo, objetivos, estudiantesIds,
                codirectorId, asesor == null ? null : asesor.toDatos(), null, descripcion, lineaInvestigacion);
    }
}
