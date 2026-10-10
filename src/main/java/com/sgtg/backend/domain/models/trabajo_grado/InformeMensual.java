package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Informe mensual de avance de una práctica profesional, radicado por el
 * director con el documento adjunto (Art. 45). Del documento solo se guarda
 * la ruta en el servidor de archivos.
 *
 * @param numero      consecutivo del informe, inicia en 1
 * @param rutaArchivo ruta del documento del informe
 * @param fecha       fecha de radicación
 */
public record InformeMensual(int numero, String rutaArchivo, LocalDate fecha) {

    public InformeMensual {
        if (CheckString.isBlank(rutaArchivo))
            throw new ReglaNegocioException("error.dominio.informe_mensual.documento_obligatorio");
        if (numero < 1 || fecha == null)
            throw new ReglaNegocioException("error.dominio.informe_mensual.datos_incompletos");
        rutaArchivo = rutaArchivo.trim();
    }
}
