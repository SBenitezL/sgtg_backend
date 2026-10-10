package com.sgtg.backend.application.input.trabajo_grado;

/**
 * Archivo que se recibe o se entrega para un trabajo de grado, independiente
 * del framework web. El dominio no lo conoce: solo guarda la ruta en la que
 * queda almacenado.
 *
 * @param nombre        nombre original del archivo
 * @param tipoContenido tipo MIME (ej. application/pdf)
 * @param contenido     bytes del archivo
 */
public record ArchivoAdjunto(String nombre, String tipoContenido, byte[] contenido) {

    /**
     * @return true si no se recibió archivo o no tiene contenido
     */
    public boolean estaVacio() {
        return this.contenido == null || this.contenido.length == 0;
    }
}
