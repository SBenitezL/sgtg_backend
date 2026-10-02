package com.sgtg.backend.infraestructure.input.usuario.dto.input;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Roles que el administrador asigna a un usuario; reemplazan los actuales.
 *
 * @param roles nombres de los roles (ej. COMITE, CONSEJO, JEFE_DEP)
 */
public record AsignarRolesDTORequest(@NotEmpty(message = "Se debe indicar al menos un rol") List<String> roles) {
}
