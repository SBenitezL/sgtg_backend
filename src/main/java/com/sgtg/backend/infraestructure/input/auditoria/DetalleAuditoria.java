package com.sgtg.backend.infraestructure.input.auditoria;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Detalle que la auditoría guarda con la petición en curso (ej. el archivo
 * descargado con su huella, o el usuario creado). Fuera de una petición (tareas
 * programadas, pruebas unitarias) no hace nada.
 */
public final class DetalleAuditoria {

    private static final String ATRIBUTO = "sgtg.auditoria.detalle";

    private DetalleAuditoria() {
    }

    /** Agrega una línea al detalle de la petición en curso. */
    public static void agregar(String linea) {
        RequestAttributes peticion = RequestContextHolder.getRequestAttributes();
        if (peticion == null || linea == null || linea.isBlank())
            return;
        lineas(peticion).add(linea);
    }

    /** Líneas agregadas en la petición, en orden; vacía si no hay. */
    @SuppressWarnings("unchecked")
    static List<String> de(HttpServletRequest peticion) {
        return peticion.getAttribute(ATRIBUTO) instanceof List<?> lineas ? List.copyOf((List<String>) lineas)
                : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> lineas(RequestAttributes peticion) {
        Object lineas = peticion.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
        if (lineas instanceof List<?> lista)
            return (List<String>) lista;
        List<String> nuevas = new ArrayList<>();
        peticion.setAttribute(ATRIBUTO, nuevas, RequestAttributes.SCOPE_REQUEST);
        return nuevas;
    }
}
