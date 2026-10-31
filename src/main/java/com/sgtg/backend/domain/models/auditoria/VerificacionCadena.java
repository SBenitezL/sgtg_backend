package com.sgtg.backend.domain.models.auditoria;

/**
 * Resultado de recorrer la cadena de la auditoría recalculando los sellos.
 *
 * @param registros   registros sellados revisados
 * @param ultimoSello sello del último registro revisado; si se guarda fuera de la base (el log), permite
 *                    detectar también que se borraron los últimos registros
 * @param falla       primer problema encontrado; null si la cadena está íntegra
 */
public record VerificacionCadena(long registros, String ultimoSello, String falla) {

    public boolean integra() {
        return falla == null;
    }
}
