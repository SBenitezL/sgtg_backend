package com.sgtg.backend.application.output;

import java.util.Map;

import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;

/** Puerto de salida para la persistencia de los perfiles académicos. */
public interface ManagePerfilGatewayIntPort {

    /** Crea o reemplaza el perfil docente del usuario. */
    public PerfilDocente guardarPerfilDocente(PerfilDocente perfil);

    /** @return el perfil docente del usuario, o null si no tiene */
    public PerfilDocente buscarPerfilDocente(Long usuarioId);

    /** Crea o reemplaza el perfil de estudiante del usuario. */
    public PerfilEstudiante guardarPerfilEstudiante(PerfilEstudiante perfil);

    /** @return el perfil de estudiante del usuario, o null si no tiene */
    public PerfilEstudiante buscarPerfilEstudiante(Long usuarioId);

    /** @return los perfiles docentes de todos los usuarios, por identificador del usuario (una sola consulta) */
    public Map<Long, PerfilDocente> listarPerfilesDocentes();

    /** @return los perfiles de estudiante de todos los usuarios, por identificador del usuario (una sola consulta) */
    public Map<Long, PerfilEstudiante> listarPerfilesEstudiantes();
}
