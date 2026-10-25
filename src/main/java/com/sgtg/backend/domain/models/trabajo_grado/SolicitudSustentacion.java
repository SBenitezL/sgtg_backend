package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

/**
 * Solicitud de sustentación anterior que no siguió: Decanatura no aprobó su
 * paz y salvo y el director radicó una nueva. Conserva los documentos con los
 * que se solicitó.
 *
 * @param rutaMonografia     monografía
 * @param rutaDocumentoFinal documento final (formato E)
 * @param rutaConceptoAsesor concepto del asesor (práctica profesional), null si no aplica
 * @param rutaAnexos         anexos, null si no se adjuntaron
 * @param fechaSolicitud     fecha de la solicitud
 */
public record SolicitudSustentacion(String rutaMonografia, String rutaDocumentoFinal, String rutaConceptoAsesor,
        String rutaAnexos, LocalDate fechaSolicitud) {

    public SolicitudSustentacion {
        if (rutaDocumentoFinal == null || fechaSolicitud == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.solicitud_incompleta");
    }
}
