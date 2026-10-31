package com.sgtg.backend.application.input.auth;

import java.time.LocalDateTime;

/**
 * Tokens de una sesión al iniciarla o renovarla.
 *
 * @param tokenAcceso      token corto que va en {@code Authorization: Bearer}
 * @param tokenRenovacion  token para pedir uno de acceso nuevo; va en una cookie que el navegador no deja leer
 * @param expiraRenovacion cuándo vence la sesión por su duración máxima (vida de la cookie)
 * @param sesion           identificador de la sesión (el de la auditoría)
 */
public record TokensSesion(String tokenAcceso, String tokenRenovacion, LocalDateTime expiraRenovacion,
        String sesion) {
}
