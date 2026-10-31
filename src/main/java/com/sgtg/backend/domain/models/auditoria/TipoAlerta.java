package com.sgtg.backend.domain.models.auditoria;

/** Hechos sospechosos que se avisan a los administradores. */
public enum TipoAlerta {
    /** Muchos inicios de sesión fallidos desde una misma IP: alguien prueba correos o contraseñas. */
    FUERZA_BRUTA_IP("Muchos inicios de sesión fallidos desde una misma IP"),
    /** Un correo llegó al bloqueo por intentos fallidos. */
    CORREO_BLOQUEADO("Correo bloqueado por intentos fallidos"),
    /** Un usuario intentó varias veces operaciones para las que no tiene permiso. */
    ACCESOS_DENEGADOS("Accesos denegados repetidos"),
    /** Se usó el token de una sesión ya cerrada o anterior a un cambio de contraseña (posible token robado). */
    SESION_CERRADA_USADA("Uso de una sesión cerrada"),
    /** Se presentó un token de renovación ya usado: alguien más lo copió; la sesión se cierra. */
    TOKEN_RENOVACION_REUTILIZADO("Reutilización de un token de renovación"),
    /** La cadena de la auditoría no coincide: se modificaron o borraron registros fuera de la aplicación. */
    AUDITORIA_ALTERADA("La auditoría fue alterada"),
    /** Un archivo guardado ya no coincide con su huella SHA-256 o desapareció. */
    ARCHIVO_ALTERADO("Un archivo fue alterado");

    private final String titulo;

    TipoAlerta(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }
}
