package com.sgtg.backend.application.input.seguridad;

/** Detección de hechos sospechosos y verificación de la integridad de la auditoría y los archivos. */
public interface ManageSeguridadCUIntPort {

    /**
     * Revisa la auditoría reciente y avisa a los administradores de los hechos
     * sospechosos (ver {@link com.sgtg.backend.domain.models.auditoria.TipoAlerta}).
     *
     * @return cantidad de alertas nuevas
     */
    int detectarAlertas();

    /**
     * Verifica la cadena de la auditoría y las huellas de los archivos; si algo
     * no coincide, lo avisa a los administradores.
     *
     * @return true si todo está íntegro
     */
    boolean verificarIntegridad();
}
