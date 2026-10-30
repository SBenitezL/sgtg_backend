package com.sgtg.backend.domain.models.auditoria;

/** Resultado de una operación auditada, según el código de respuesta. */
public enum ResultadoAuditoria {
    /** 2xx/3xx: la operación se realizó. */
    EXITOSA,
    /** 401: sin sesión válida (token ausente, vencido o inválido) o credenciales incorrectas al iniciar sesión. */
    NO_AUTENTICADO,
    /** 403: el usuario no tiene permiso para la operación. */
    SIN_PERMISO,
    /** Otro 4xx: la operación no cumplió una regla de negocio o los datos no eran válidos. */
    RECHAZADA,
    /** 5xx: error del servidor. */
    ERROR;

    public static ResultadoAuditoria desde(int estadoHttp) {
        if (estadoHttp == 401)
            return NO_AUTENTICADO;
        if (estadoHttp == 403)
            return SIN_PERMISO;
        if (estadoHttp >= 500)
            return ERROR;
        return estadoHttp >= 400 ? RECHAZADA : EXITOSA;
    }
}
