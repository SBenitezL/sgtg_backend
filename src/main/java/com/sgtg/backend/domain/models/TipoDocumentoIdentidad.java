package com.sgtg.backend.domain.models;

/**
 * Tipo de documento de identidad de un usuario. Se llama así para no
 * confundirlo con {@code TipoDocumento} de trabajo de grado (propuesta,
 * anteproyecto, etc.).
 */
public enum TipoDocumentoIdentidad {
    CC("Cédula de Ciudadanía (CC)"),
    CE("Cédula de Extranjería (CE)"),
    PASAPORTE("Pasaporte");

    /** Nombre para mostrar */
    private final String descripcion;

    TipoDocumentoIdentidad(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
