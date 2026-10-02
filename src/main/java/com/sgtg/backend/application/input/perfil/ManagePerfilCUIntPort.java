package com.sgtg.backend.application.input.perfil;

import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

/** Puerto de entrada para administrar los perfiles académicos de los usuarios. */
public interface ManagePerfilCUIntPort {

    /**
     * Registra o actualiza el perfil docente de un usuario.
     *
     * @param usuarioId       usuario con un rol docente (DOCENTE, DIRECTOR,
     *                        CODIRECTOR, EVALUADOR o JURADO)
     * @param tipoVinculacion vinculación vigente con la universidad
     * @param departamentoId  departamento al que está adscrito
     * @return el perfil guardado
     */
    public PerfilDocente registrarPerfilDocente(Long usuarioId, TipoVinculacion tipoVinculacion, Long departamentoId);

    /**
     * @param usuarioId identificador del usuario
     * @return su perfil docente
     */
    public PerfilDocente consultarPerfilDocente(Long usuarioId);

    /**
     * Registra o actualiza el perfil de estudiante de un usuario.
     *
     * @param usuarioId  usuario con rol ESTUDIANTE
     * @param programaId programa en el que está inscrito
     * @return el perfil guardado
     */
    public PerfilEstudiante registrarPerfilEstudiante(Long usuarioId, Long programaId);

    /**
     * @param usuarioId identificador del usuario
     * @return su perfil de estudiante
     */
    public PerfilEstudiante consultarPerfilEstudiante(Long usuarioId);
}
