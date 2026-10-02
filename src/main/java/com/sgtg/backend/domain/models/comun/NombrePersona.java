package com.sgtg.backend.domain.models.comun;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Nombre de una persona: primer nombre y primer apellido obligatorios; segundo
 * nombre y segundo apellido opcionales (null si no tiene).
 */
public record NombrePersona(String primerNombre, String segundoNombre, String primerApellido,
        String segundoApellido) {

    public NombrePersona {
        if (CheckString.isBlank(primerNombre) || CheckString.isBlank(primerApellido))
            throw new ReglaNegocioException("error.dominio.nombre.datos_incompletos");
        segundoNombre = CheckString.isBlank(segundoNombre) ? null : segundoNombre;
        segundoApellido = CheckString.isBlank(segundoApellido) ? null : segundoApellido;
    }

    /** @return nombres y apellidos separados por espacio, omitiendo los que no tiene */
    public String completo() {
        return Stream.of(primerNombre, segundoNombre, primerApellido, segundoApellido)
                .filter(parte -> parte != null)
                .collect(Collectors.joining(" "));
    }
}
