package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input;

import jakarta.validation.constraints.NotBlank;

/**
 * Documento ya cargado al servidor de archivos.
 *
 * @param url dirección http(s) desde la que se recupera
 */
public record RadicarDocumentoDTORequest(@NotBlank(message = "La URL del documento es obligatoria") String url) {
}
