package com.sgtg.backend.application.output;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;

/**
 * Puerto de salida para el almacenamiento de los archivos de los trabajos de
 * grado. Los archivos se organizan por trabajo, tipo de documento e iteración
 * ({@code <trabajoId>/<carpeta>/v<n>/<nombre>}); el dominio solo guarda la
 * ruta relativa que devuelve este puerto.
 */
public interface ManageArchivoGatewayIntPort {

    /**
     * Guarda el archivo en la siguiente iteración libre de la carpeta del tipo
     * de documento dentro del trabajo. Nunca sobrescribe un archivo existente.
     *
     * @param trabajoId identificador del trabajo de grado
     * @param carpeta   carpeta del tipo de documento (ej. "PP-A", "ANTEPROYECTO")
     * @param archivo   archivo a guardar
     * @return ruta relativa del archivo guardado
     */
    public String guardar(Long trabajoId, String carpeta, ArchivoAdjunto archivo);

    /**
     * Guarda el archivo en una ubicación temporal, para los documentos que se
     * cargan antes de que el trabajo tenga identificador (carta de aceptación).
     *
     * @param archivo archivo a guardar
     * @return ruta relativa temporal
     */
    public String guardarTemporal(ArchivoAdjunto archivo);

    /**
     * Mueve un archivo temporal a la siguiente iteración libre de la carpeta
     * del tipo de documento dentro del trabajo.
     *
     * @param rutaTemporal ruta devuelta por {@link #guardarTemporal}
     * @param trabajoId    identificador del trabajo de grado
     * @param carpeta      carpeta del tipo de documento
     * @return nueva ruta relativa del archivo
     */
    public String reubicar(String rutaTemporal, Long trabajoId, String carpeta);

    /**
     * @param ruta ruta relativa del archivo
     * @return el archivo con su nombre, tipo y contenido; null si no existe
     */
    public ArchivoAdjunto obtener(String ruta);

    /**
     * Elimina un archivo y su carpeta de iteración. No falla si no existe.
     *
     * @param ruta ruta relativa del archivo
     */
    public void eliminar(String ruta);
}
