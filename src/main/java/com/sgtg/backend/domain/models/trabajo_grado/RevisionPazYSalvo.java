package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Revisión del paz y salvo (formato F) que registra Decanatura después de la
 * solicitud de sustentación. El documento es obligatorio tanto si lo aprueba
 * como si no; si no lo aprueba, las observaciones también lo son y puede
 * registrar otra revisión más adelante.
 *
 * @param rutaDocumento ruta del formato F
 * @param aprobado      true si el estudiante está a paz y salvo
 * @param observaciones motivo de la no aprobación (opcional si se aprueba)
 * @param fecha         fecha de la revisión
 */
public record RevisionPazYSalvo(String rutaDocumento, boolean aprobado, String observaciones, LocalDate fecha) {

    public RevisionPazYSalvo {
        if (CheckString.isBlank(rutaDocumento))
            throw new ReglaNegocioException("error.dominio.sustentacion.paz_y_salvo_obligatorio");
        if (fecha == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.solicitud_incompleta");
        if (!aprobado && CheckString.isBlank(observaciones))
            throw new ReglaNegocioException("error.dominio.sustentacion.paz_y_salvo_observaciones_obligatorias");
        rutaDocumento = rutaDocumento.trim();
        observaciones = CheckString.isBlank(observaciones) ? null : observaciones.trim();
    }
}
