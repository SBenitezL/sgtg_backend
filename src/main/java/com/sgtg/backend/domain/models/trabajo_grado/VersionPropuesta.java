package com.sgtg.backend.domain.models.trabajo_grado;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

/**
 * Versión de la propuesta enviada a revisión del Comité de Programa: el
 * documento (formato A) y su estado. Queda EN_REVISION al radicarla y pasa a
 * APROBADO o NO_APROBADO con el concepto del Comité; cada versión conserva el
 * resultado de su revisión.
 *
 * @param documento documento radicado de la versión
 * @param estado    estado de la revisión de esta versión
 */
public record VersionPropuesta(Documento documento, Estado estado) {

    public VersionPropuesta {
        if (documento == null || estado == null)
            throw new ReglaNegocioException("error.dominio.propuesta.datos_incompletos");
    }

    /** Versión recién radicada: queda en revisión del Comité. */
    static VersionPropuesta enRevision(Documento documento) {
        return new VersionPropuesta(documento, Estado.EN_REVISION);
    }

    /** Copia de esta versión con el resultado de la revisión. */
    VersionPropuesta conEstado(Estado nuevoEstado) {
        return new VersionPropuesta(this.documento, nuevoEstado);
    }

    public boolean enRevision() {
        return this.estado == Estado.EN_REVISION;
    }
}
