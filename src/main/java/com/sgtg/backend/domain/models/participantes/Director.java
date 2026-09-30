package com.sgtg.backend.domain.models.participantes;

import java.util.Set;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Docente (o estudiante de posgrado) que dirige o codirige un trabajo de
 * grado. Guarda una copia de la vinculación y el departamento que tenía al
 * momento de la asignación, aunque su perfil cambie después.
 */
public class Director extends Participante {
    /** Roles del sistema que permiten dirigir o codirigir un trabajo de grado */
    private static final Set<RoleAdmited> ROLES_PERMITIDOS = Set.of(RoleAdmited.DOCENTE, RoleAdmited.DIRECTOR,
            RoleAdmited.CODIRECTOR);

    /** Vinculación con la universidad al momento de la asignación */
    private TipoVinculacion tipoVinculacion;
    /** Departamento al que estaba adscrito al momento de la asignación */
    private Departamento departamento;

    /**
     * Crea el director a partir de un usuario con rol DOCENTE, DIRECTOR o
     * CODIRECTOR y de su perfil docente.
     */
    public Director(Usuario usuario, PerfilDocente perfil) {
        super(usuario, ROLES_PERMITIDOS);
        if (perfil == null || !perfil.perteneceA(usuario))
            throw new ReglaNegocioException("error.dominio.perfil.no_corresponde_al_usuario");
        this.tipoVinculacion = perfil.getTipoVinculacion();
        this.departamento = perfil.getDepartamento();
    }

    /** Reconstruye el director con datos ya persistidos. */
    public Director(Long usuarioId, String codigo, String nombres, String apellidos, String email,
            TipoVinculacion tipoVinculacion, Departamento departamento) {
        super(usuarioId, codigo, nombres, apellidos, email, true);
        if (tipoVinculacion == null || departamento == null)
            throw new ReglaNegocioException("error.dominio.director.vinculacion_obligatoria");
        this.tipoVinculacion = tipoVinculacion;
        this.departamento = departamento;
    }

    /**
     * @return true si por su vinculación debe tener un codirector de planta
     */
    public boolean requiereCodirector() {
        return this.tipoVinculacion.requiereCodirector();
    }

    /**
     * Un codirector exigido por el Art. 6 debe ser profesor de planta del
     * departamento respectivo.
     *
     * @return true si es de planta y está adscrito al departamento indicado
     */
    public boolean esDePlantaDe(Departamento departamento) {
        return this.tipoVinculacion == TipoVinculacion.PLANTA && this.departamento.esMismo(departamento);
    }

    public TipoVinculacion getTipoVinculacion() {
        return tipoVinculacion;
    }

    public Departamento getDepartamento() {
        return departamento;
    }
}
