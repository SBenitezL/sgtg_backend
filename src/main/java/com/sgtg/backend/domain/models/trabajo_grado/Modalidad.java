package com.sgtg.backend.domain.models.trabajo_grado;

/** Modalidades de trabajo de grado aprobadas por la Facultad (Art. 3). */
public enum Modalidad {
    /** Nueve meses de ejecución; la sustentación se solicita desde el tercer mes. */
    TRABAJO_DE_INVESTIGACION("TI", 3, 9),
    /** Mínimo seis meses y máximo un año de ejecución. */
    PRACTICA_PROFESIONAL("PP", 6, 12);

    /** Prefijo de los formatos de la modalidad (ej. TI-A, PP-A) */
    private final String prefijoFormato;
    /** Meses mínimos de ejecución antes de solicitar la sustentación */
    private final int mesesMinimosEjecucion;
    /** Meses máximos de ejecución, contados desde la notificación de la aprobación */
    private final int mesesMaximosEjecucion;

    Modalidad(String prefijoFormato, int mesesMinimosEjecucion, int mesesMaximosEjecucion) {
        this.prefijoFormato = prefijoFormato;
        this.mesesMinimosEjecucion = mesesMinimosEjecucion;
        this.mesesMaximosEjecucion = mesesMaximosEjecucion;
    }

    public int getMesesMinimosEjecucion() {
        return mesesMinimosEjecucion;
    }

    public int getMesesMaximosEjecucion() {
        return mesesMaximosEjecucion;
    }

    public String getPrefijoFormato() {
        return prefijoFormato;
    }
}
