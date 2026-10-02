package com.sgtg.backend.application.input.trabajo_grado;

/**
 * Asesor indicado al crear un trabajo de grado. Si tiene cuenta en el sistema
 * basta con {@code usuarioId}; si es externo sin cuenta se envían sus datos
 * (el código puede ser su documento de identidad). Segundo nombre y segundo
 * apellido son opcionales.
 */
public record DatosAsesor(Long usuarioId, String codigo, String primerNombre, String segundoNombre,
        String primerApellido, String segundoApellido, String email, String organizacion, boolean externo) {
}
