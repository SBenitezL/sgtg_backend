package com.sgtg.backend.domain.models.participantes;

import java.util.Set;

import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Profesor o estudiante de posgrado asignado para evaluar el anteproyecto o
 * como jurado de la sustentación de un trabajo de grado.
 */
public class Evaluador extends Participante {
    /** Roles del sistema que permiten evaluar un anteproyecto o ser jurado */
    private static final Set<RoleAdmited> ROLES_PERMITIDOS = Set.of(RoleAdmited.DOCENTE, RoleAdmited.EVALUADOR,
            RoleAdmited.JURADO);

    /** Crea el evaluador a partir de un usuario con rol DOCENTE, EVALUADOR o JURADO. */
    public Evaluador(Usuario usuario) {
        super(usuario, ROLES_PERMITIDOS);
    }

    /** Reconstruye el evaluador con datos ya persistidos. */
    public Evaluador(Long usuarioId, String codigo, String nombres, String apellidos, String email) {
        super(usuarioId, codigo, nombres, apellidos, email, true);
    }
}
