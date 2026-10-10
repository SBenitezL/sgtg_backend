package com.sgtg.backend.application.input.usuario;

import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;

/**
 * Usuario con sus perfiles académicos, tal como lo consulta la administración
 * de usuarios.
 *
 * @param usuario          datos del usuario
 * @param perfilDocente    perfil docente, null si no tiene
 * @param perfilEstudiante perfil de estudiante, null si no tiene
 */
public record FichaUsuario(Usuario usuario, PerfilDocente perfilDocente, PerfilEstudiante perfilEstudiante) {
}
