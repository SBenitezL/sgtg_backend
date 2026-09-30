package com.sgtg.backend.domain.models.trabajo_grado;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Versión de un documento cargado al servidor de archivos. El dominio solo
 * guarda la URL desde la que se recupera.
 *
 * @param tipo       tipo de documento
 * @param url        URL http(s) del archivo en el servidor
 * @param version    número de versión, inicia en 1
 * @param fechaCarga fecha en que se cargó esta versión
 */
public record Documento(TipoDocumento tipo, String url, int version, LocalDate fechaCarga) {

    public Documento {
        if (tipo == null || CheckString.isBlank(url) || fechaCarga == null)
            throw new ReglaNegocioException("error.dominio.documento.datos_incompletos");
        if (!esUrlValida(url))
            throw new ReglaNegocioException("error.dominio.documento.url_invalida");
        if (version < 1)
            throw new ReglaNegocioException("error.dominio.documento.version_invalida");
    }

    /** Crea la primera versión de un documento. */
    public static Documento inicial(TipoDocumento tipo, String url, LocalDate fechaCarga) {
        return new Documento(tipo, url, 1, fechaCarga);
    }

    /** Crea la siguiente versión de este documento (ej. tras correcciones). */
    public Documento nuevaVersion(String url, LocalDate fechaCarga) {
        return new Documento(this.tipo, url, this.version + 1, fechaCarga);
    }

    private static boolean esUrlValida(String url) {
        try {
            URI uri = new URI(url);
            String esquema = uri.getScheme();
            return ("http".equalsIgnoreCase(esquema) || "https".equalsIgnoreCase(esquema))
                    && !CheckString.isBlank(uri.getHost());
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
