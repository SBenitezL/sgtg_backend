package com.sgtg.backend.domain.exceptions;

/**
 * Excepción lanzada por el dominio cuando se incumple una regla de negocio.
 * El mensaje corresponde a una clave definida en message.properties
 * (ej. "error.dominio.proceso.transicion_invalida").
 */
public class ReglaNegocioException extends RuntimeException {

    /**
     * @param claveMensaje clave del mensaje que describe la regla incumplida
     */
    public ReglaNegocioException(String claveMensaje) {
        super(claveMensaje);
    }
}
