package com.sgtg.backend.application.input.usuario;

import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

/**
 * Perfil académico que se registra junto con el usuario. Todos los campos son
 * opcionales: el perfil docente exige vinculación y departamento juntos, y el
 * de estudiante exige el programa.
 *
 * @param tipoVinculacion vinculación del docente, null si no tiene perfil docente
 * @param departamentoId  departamento del docente, null si no tiene perfil docente
 * @param programaId      programa del estudiante, null si no tiene perfil de estudiante
 */
public record DatosPerfil(TipoVinculacion tipoVinculacion, Long departamentoId, Long programaId) {

    /** Perfil vacío: el usuario no tiene perfil académico. */
    public static final DatosPerfil NINGUNO = new DatosPerfil(null, null, null);

    /** @return true si se indicó algún dato del perfil docente */
    public boolean tienePerfilDocente() {
        return this.tipoVinculacion != null || this.departamentoId != null;
    }

    /** @return true si se indicó el programa del perfil de estudiante */
    public boolean tienePerfilEstudiante() {
        return this.programaId != null;
    }
}
