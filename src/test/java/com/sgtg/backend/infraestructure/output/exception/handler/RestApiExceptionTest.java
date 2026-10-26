package com.sgtg.backend.infraestructure.output.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.sgtg.backend.infraestructure.output.exception.structure.Error;

/** Respuestas de los errores que no son reglas de negocio. */
class RestApiExceptionTest {

    private final StaticMessageSource mensajes = new StaticMessageSource();
    private final RestApiException manejador = new RestApiException(mensajes);
    private final MockHttpServletRequest peticion = new MockHttpServletRequest("POST",
            "/api/v1/sgtg/trabajos-grado/2/informes-mensuales");

    RestApiExceptionTest() {
        mensajes.addMessage("error.general.conflicto", Locale.ROOT, "Ya se registró");
        mensajes.addMessage("error.general.inesperado", Locale.ROOT, "Error inesperado");
    }

    @Test
    void unDobleEnvioQueChocaAlGuardarRespondeConflictoConSuMensaje() {
        ResponseEntity<Error> respuesta = manejador.handleConflictException(peticion,
                new DataIntegrityViolationException("Duplicate entry '2-1'"), Locale.ROOT);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("GC-0011", respuesta.getBody().getErrorCode());
        assertEquals("Ya se registró", respuesta.getBody().getMessage());
    }

    @Test
    void unErrorNoControladoRespondeConUnMensajeLegible() {
        ResponseEntity<Error> respuesta = manejador.handleException(peticion, new IllegalStateException("x"),
                Locale.ROOT);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("GC-0001", respuesta.getBody().getErrorCode());
        assertEquals("Error inesperado", respuesta.getBody().getMessage());
    }
}
