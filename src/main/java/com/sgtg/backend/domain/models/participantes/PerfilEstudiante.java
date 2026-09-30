package com.sgtg.backend.domain.models.participantes;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Datos académicos de un estudiante de pregrado. Se mantiene separado de
 * {@link Usuario}, que solo contiene la información de autenticación, y lo
 * referencia por su identificador. En una segunda versión incluirá los datos
 * de elegibilidad (porcentaje de créditos, matrícula condicional).
 */
public class PerfilEstudiante {
    /** Identificador del usuario al que pertenece el perfil */
    private Long usuarioId;
    /** Programa académico en el que está inscrito */
    private Programa programa;

    public PerfilEstudiante(Long usuarioId, Programa programa) {
        if (usuarioId == null || programa == null)
            throw new ReglaNegocioException("error.dominio.perfil_estudiante.datos_incompletos");
        this.usuarioId = usuarioId;
        this.programa = programa;
    }

    /**
     * @return true si el perfil pertenece al usuario indicado
     */
    public boolean perteneceA(Usuario usuario) {
        return usuario != null && this.usuarioId.equals(usuario.getCusuari());
    }

    /** Cambia el programa académico del estudiante. */
    public void cambiarPrograma(Programa programa) {
        if (programa == null)
            throw new ReglaNegocioException("error.dominio.perfil_estudiante.datos_incompletos");
        this.programa = programa;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Programa getPrograma() {
        return programa;
    }
}
