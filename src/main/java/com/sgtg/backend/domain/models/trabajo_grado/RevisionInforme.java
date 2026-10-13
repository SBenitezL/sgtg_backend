package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Revisión de un informe mensual por parte del director, el codirector o el
 * asesor: observaciones y, opcionalmente, un PDF con sus notas. Es una
 * formalidad: no cambia el estado ni el flujo del trabajo de grado.
 *
 * @param revisorUsuarioId usuario que revisa
 * @param revisor          nombre completo de quien revisa
 * @param observaciones    observaciones sobre el informe
 * @param rutaArchivo      ruta del PDF con las notas, null si no se adjuntó
 * @param fecha            fecha de la revisión
 */
public record RevisionInforme(Long revisorUsuarioId, String revisor, String observaciones, String rutaArchivo,
        LocalDate fecha) {

    public RevisionInforme {
        if (revisorUsuarioId == null || CheckString.isBlank(revisor) || fecha == null)
            throw new ReglaNegocioException("error.dominio.informe_mensual.revision_incompleta");
        if (CheckString.isBlank(observaciones) && CheckString.isBlank(rutaArchivo))
            throw new ReglaNegocioException("error.dominio.informe_mensual.revision_vacia");
        observaciones = CheckString.isBlank(observaciones) ? null : observaciones.trim();
        rutaArchivo = CheckString.isBlank(rutaArchivo) ? null : rutaArchivo.trim();
    }
}
