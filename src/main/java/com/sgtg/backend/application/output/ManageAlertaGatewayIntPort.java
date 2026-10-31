package com.sgtg.backend.application.output;

import com.sgtg.backend.domain.models.auditoria.AlertaSeguridad;

/** Registro y aviso de las alertas de seguridad. */
public interface ManageAlertaGatewayIntPort {

    /**
     * Guarda la alerta y la avisa a los administradores activos (notificación y
     * log), salvo que ya se haya avisado una del mismo tipo y clave en las
     * últimas 24 horas.
     *
     * @return true si se avisó; false si era repetida
     */
    boolean alertar(AlertaSeguridad alerta);
}
