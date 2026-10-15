package com.sgtg.backend.domain.models.notificacion;

/**
 * Grupo de usuarios que recibe una notificación del trabajo de grado. Los
 * primeros se toman de los participantes del trabajo; los últimos son los
 * usuarios con el rol institucional.
 */
public enum Destinatario {
    /** Estudiantes del trabajo. */
    ESTUDIANTES,
    /** Director, codirector y asesor (si tiene usuario). */
    DIRECCION,
    /** Evaluadores asignados al anteproyecto. */
    EVALUADORES,
    /** Evaluadores que aún no emiten concepto sobre la versión actual del anteproyecto. */
    EVALUADORES_PENDIENTES,
    /** Jurados de la sesión de sustentación actual. */
    JURADOS,
    /** Miembros del Comité de Programa. */
    COMITE,
    /** Jefe del departamento al que está adscrito el director. */
    JEFE_DEP,
    /** Miembros del Consejo de Facultad. */
    CONSEJO,
    /** Usuarios de Decanatura (revisan el paz y salvo). */
    DECANATURA
}
