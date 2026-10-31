package com.sgtg.backend.application.output;

import com.sgtg.backend.domain.models.auditoria.Instantanea;

/**
 * Datos de usuarios y trabajos de grado que registra la auditoría antes y
 * después de una operación, para guardar qué cambió.
 */
public interface ManageInstantaneaGatewayIntPort {

    /**
     * Datos de un usuario: identificación, roles, estado, perfil académico y la
     * versión de sus credenciales (nunca el hash de la contraseña).
     *
     * @return null si el usuario no existe
     */
    Instantanea usuario(Long usuarioId);

    /**
     * Datos de un trabajo de grado: etapa, cierre, título, participantes,
     * decisiones, conceptos, calificaciones y fechas límite.
     *
     * @return null si el trabajo no existe
     */
    Instantanea trabajo(Long trabajoId);
}
