package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Versión de un documento cargado al almacenamiento de archivos. El dominio
 * solo guarda la ruta relativa desde la que se recupera; el archivo lo maneja
 * la infraestructura.
 *
 * @param tipo       tipo de documento
 * @param ruta       ruta relativa del archivo en el almacenamiento
 * @param version    número de versión, inicia en 1
 * @param fechaCarga fecha en que se cargó esta versión
 */
public record Documento(TipoDocumento tipo, String ruta, int version, LocalDate fechaCarga) {

    public Documento {
        if (tipo == null || CheckString.isBlank(ruta) || fechaCarga == null)
            throw new ReglaNegocioException("error.dominio.documento.datos_incompletos");
        if (!esRutaValida(ruta))
            throw new ReglaNegocioException("error.dominio.documento.ruta_invalida");
        if (version < 1)
            throw new ReglaNegocioException("error.dominio.documento.version_invalida");
    }

    /** Crea la primera versión de un documento. */
    public static Documento inicial(TipoDocumento tipo, String ruta, LocalDate fechaCarga) {
        return new Documento(tipo, ruta, 1, fechaCarga);
    }

    /** Crea la siguiente versión de este documento (ej. tras correcciones). */
    public Documento nuevaVersion(String ruta, LocalDate fechaCarga) {
        return new Documento(this.tipo, ruta, this.version + 1, fechaCarga);
    }

    /** Copia de este documento ubicado en otra ruta (ej. al moverlo a su carpeta definitiva). */
    public Documento conRuta(String ruta) {
        return new Documento(this.tipo, ruta, this.version, this.fechaCarga);
    }

    /** Una ruta válida no sale de la carpeta del almacenamiento (sin segmentos "..") */
    private static boolean esRutaValida(String ruta) {
        for (String segmento : ruta.replace('\\', '/').split("/"))
            if (segmento.equals(".."))
                return false;
        return true;
    }
}
