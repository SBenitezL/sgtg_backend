package com.sgtg.backend.domain.models.trabajo_grado;

/** Modalidades de trabajo de grado aprobadas por la Facultad (Art. 3). */
public enum Modalidad {
    TRABAJO_DE_INVESTIGACION("TI"),
    PRACTICA_PROFESIONAL("PP");

    /** Prefijo de los formatos de la modalidad (ej. TI-A, PP-A) */
    private final String prefijoFormato;

    Modalidad(String prefijoFormato) {
        this.prefijoFormato = prefijoFormato;
    }

    public String getPrefijoFormato() {
        return prefijoFormato;
    }
}
