package com.sgtg.backend.domain.models.participantes;

import java.util.Set;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.comun.NombrePersona;

/**
 * Persona natural que se encuentra inscrita en la Universidad y está en la
 * potestad de realizar un trabajo de grado, es decir, es un estudiante de
 * pregrado.
 */
public class Estudiante extends Participante {
    /** Roles del sistema que permiten ser estudiante de un trabajo de grado */
    private static final Set<RoleAdmited> ROLES_PERMITIDOS = Set.of(RoleAdmited.ESTUDIANTE);

    /** Programa académico en el que estaba inscrito al momento de la asignación */
    private Programa programa;

    /** Crea el estudiante a partir de un usuario con rol ESTUDIANTE y su perfil. */
    public Estudiante(Usuario usuario, PerfilEstudiante perfil) {
        super(usuario, ROLES_PERMITIDOS);
        if (perfil == null || !perfil.perteneceA(usuario))
            throw new ReglaNegocioException("error.dominio.perfil.no_corresponde_al_usuario");
        this.programa = perfil.getPrograma();
    }

    /** Reconstruye el estudiante con datos ya persistidos. */
    public Estudiante(Long usuarioId, String codigo, NombrePersona nombre, String email,
            Programa programa) {
        super(usuarioId, codigo, nombre, email, true);
        if (programa == null)
            throw new ReglaNegocioException("error.dominio.estudiante.programa_obligatorio");
        this.programa = programa;
    }

    public Programa getPrograma() {
        return programa;
    }
}
