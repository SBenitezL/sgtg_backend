package com.sgtg.backend.domain.models.trabajo_grado;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

/**
 * Versión de un documento enviado a revisión (propuesta o anteproyecto): el
 * documento radicado y el estado de su revisión. Queda EN_REVISION al
 * radicarla y cambia según el resultado (APROBADO, NO_APROBADO o
 * CON_CORRECCIONES); cada versión conserva el resultado de su revisión.
 *
 * @param documento documento radicado de la versión
 * @param estado    estado de la revisión de esta versión
 */
public record VersionDocumento(Documento documento, Estado estado) {

    public VersionDocumento {
        if (documento == null || estado == null)
            throw new ReglaNegocioException("error.dominio.version.datos_incompletos");
    }

    /** Versión recién radicada: queda en revisión. */
    static VersionDocumento enRevision(Documento documento) {
        return new VersionDocumento(documento, Estado.EN_REVISION);
    }

    /** Copia de esta versión con el resultado de la revisión. */
    VersionDocumento conEstado(Estado nuevoEstado) {
        return new VersionDocumento(this.documento, nuevoEstado);
    }

    public boolean enRevision() {
        return this.estado == Estado.EN_REVISION;
    }
}
