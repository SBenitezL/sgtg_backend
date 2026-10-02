package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import com.sgtg.backend.application.input.trabajo_grado.DatosAsesor;

/**
 * Asesor del trabajo. Si tiene cuenta basta con {@code usuarioId}; si es
 * externo sin cuenta se envían sus datos (el código puede ser su documento de
 * identidad). Segundo nombre y segundo apellido son opcionales.
 */
public record AsesorDTORequest(Long usuarioId, String codigo, String primerNombre, String segundoNombre,
        String primerApellido, String segundoApellido, String email, String organizacion, boolean externo) {

    public DatosAsesor toDatos() {
        return new DatosAsesor(usuarioId, codigo, primerNombre, segundoNombre, primerApellido, segundoApellido, email,
                organizacion, externo);
    }
}
