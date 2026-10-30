package com.sgtg.backend.application.output;

import com.sgtg.backend.domain.models.auditoria.RegistroAuditoria;

/**
 * Almacén de la auditoría. Solo agrega registros: la aplicación no los
 * consulta, modifica ni borra.
 */
public interface ManageAuditoriaGatewayIntPort {

    void registrar(RegistroAuditoria registro);
}
