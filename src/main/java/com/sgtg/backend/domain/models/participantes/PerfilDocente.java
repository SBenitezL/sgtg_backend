package com.sgtg.backend.domain.models.participantes;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Datos académicos de un docente (o estudiante de posgrado que dirige o
 * evalúa). Se mantiene separado de {@link Usuario}, que solo contiene la
 * información de autenticación, y lo referencia por su identificador. Guarda
 * únicamente la vinculación vigente.
 */
public class PerfilDocente {
    /** Identificador del usuario al que pertenece el perfil */
    private Long usuarioId;
    /** Vinculación vigente con la universidad */
    private TipoVinculacion tipoVinculacion;
    /** Departamento al que está adscrito */
    private Departamento departamento;

    public PerfilDocente(Long usuarioId, TipoVinculacion tipoVinculacion, Departamento departamento) {
        if (usuarioId == null || tipoVinculacion == null || departamento == null)
            throw new ReglaNegocioException("error.dominio.perfil_docente.datos_incompletos");
        this.usuarioId = usuarioId;
        this.tipoVinculacion = tipoVinculacion;
        this.departamento = departamento;
    }

    /**
     * @return true si el perfil pertenece al usuario indicado
     */
    public boolean perteneceA(Usuario usuario) {
        return usuario != null && this.usuarioId.equals(usuario.getCusuari());
    }

    /**
     * Actualiza la vinculación vigente (ej. un docente ocasional que pasa a
     * planta). Los trabajos de grado ya asignados conservan la vinculación que
     * tenía el docente al momento de la asignación.
     */
    public void actualizarVinculacion(TipoVinculacion tipoVinculacion) {
        if (tipoVinculacion == null)
            throw new ReglaNegocioException("error.dominio.perfil_docente.datos_incompletos");
        this.tipoVinculacion = tipoVinculacion;
    }

    /** Cambia el departamento al que está adscrito el docente. */
    public void cambiarDepartamento(Departamento departamento) {
        if (departamento == null)
            throw new ReglaNegocioException("error.dominio.perfil_docente.datos_incompletos");
        this.departamento = departamento;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public TipoVinculacion getTipoVinculacion() {
        return tipoVinculacion;
    }

    public Departamento getDepartamento() {
        return departamento;
    }
}
