package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

/**
 * Modalidades de trabajo de grado aprobadas por la Facultad (Art. 3). En las
 * dos, el Consejo fija la fecha máxima de entrega entre {@value #MESES_MINIMOS_PLAZO}
 * y {@value #MESES_MAXIMOS_PLAZO} meses después de la fecha de inicio (la de la
 * reunión en que aprobó el anteproyecto), y la sustentación se solicita desde
 * el tercer mes (2026-11-09).
 */
public enum Modalidad {
    TRABAJO_DE_INVESTIGACION("TI", 3),
    PRACTICA_PROFESIONAL("PP", 3);

    /** Plazo de ejecución más corto que puede fijar el Consejo, en meses */
    public static final int MESES_MINIMOS_PLAZO = 3;
    /** Plazo de ejecución más largo que puede fijar el Consejo, en meses */
    public static final int MESES_MAXIMOS_PLAZO = 9;

    /** Prefijo de los formatos de la modalidad (ej. TI-A, PP-A) */
    private final String prefijoFormato;
    /** Meses mínimos de ejecución antes de solicitar la sustentación */
    private final int mesesMinimosEjecucion;

    Modalidad(String prefijoFormato, int mesesMinimosEjecucion) {
        this.prefijoFormato = prefijoFormato;
        this.mesesMinimosEjecucion = mesesMinimosEjecucion;
    }

    public int getMesesMinimosEjecucion() {
        return mesesMinimosEjecucion;
    }

    /** @return el plazo de ejecución más largo que puede fijar el Consejo */
    public int getMesesMaximosEjecucion() {
        return MESES_MAXIMOS_PLAZO;
    }

    /**
     * @param inicio fecha de inicio de la ejecución
     * @param fin    fecha máxima de entrega
     * @return true si el fin está entre 3 y 9 meses después del inicio (ambos incluidos)
     */
    public boolean admitePlazo(LocalDate inicio, LocalDate fin) {
        return inicio != null && fin != null && !fin.isBefore(inicio.plusMonths(MESES_MINIMOS_PLAZO))
                && !fin.isAfter(inicio.plusMonths(MESES_MAXIMOS_PLAZO));
    }

    public String getPrefijoFormato() {
        return prefijoFormato;
    }
}
