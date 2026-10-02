package com.sgtg.backend.application.input.trabajo_grado;

import java.util.List;

import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;

/**
 * Datos para crear un trabajo de grado. El director es el docente que lo crea.
 *
 * @param modalidad          investigación o práctica profesional
 * @param estudiantesIds     usuarios de los estudiantes (uno solo en práctica profesional)
 * @param codirectorId       usuario del codirector, null si no tiene
 * @param asesor             asesor, null si no tiene (obligatorio en práctica profesional)
 * @param urlCartaAceptacion URL de la carta de aceptación, solo práctica profesional
 */
public record NuevoTrabajoGrado(Modalidad modalidad, String titulo, List<String> objetivos, List<Long> estudiantesIds,
        Long codirectorId, DatosAsesor asesor, String urlCartaAceptacion) {
}
