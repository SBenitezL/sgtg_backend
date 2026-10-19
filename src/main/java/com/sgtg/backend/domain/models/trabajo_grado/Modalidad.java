package com.sgtg.backend.domain.models.trabajo_grado;

import java.util.List;

/** Modalidades de trabajo de grado aprobadas por la Facultad (Art. 3). */
public enum Modalidad {
    /** Plazo de 6 o 9 meses que fija el Consejo; la sustentación se solicita desde el tercer mes. */
    TRABAJO_DE_INVESTIGACION("TI", 3, 6, 9),
    /** Plazo de 6, 9 o 12 meses que fija el Consejo; la sustentación se solicita desde el tercer mes. */
    PRACTICA_PROFESIONAL("PP", 3, 6, 9, 12);

    /** Prefijo de los formatos de la modalidad (ej. TI-A, PP-A) */
    private final String prefijoFormato;
    /** Meses mínimos de ejecución antes de solicitar la sustentación */
    private final int mesesMinimosEjecucion;
    /** Plazos de ejecución (meses) entre los que elige el Consejo al aprobar el anteproyecto */
    private final List<Integer> opcionesMesesEjecucion;

    Modalidad(String prefijoFormato, int mesesMinimosEjecucion, Integer... opcionesMesesEjecucion) {
        this.prefijoFormato = prefijoFormato;
        this.mesesMinimosEjecucion = mesesMinimosEjecucion;
        this.opcionesMesesEjecucion = List.of(opcionesMesesEjecucion);
    }

    public int getMesesMinimosEjecucion() {
        return mesesMinimosEjecucion;
    }

    /** @return el plazo de ejecución más largo de la modalidad */
    public int getMesesMaximosEjecucion() {
        return opcionesMesesEjecucion.get(opcionesMesesEjecucion.size() - 1);
    }

    /** @return los plazos de ejecución (meses) que puede fijar el Consejo */
    public List<Integer> getOpcionesMesesEjecucion() {
        return opcionesMesesEjecucion;
    }

    /** @return true si el plazo de ejecución es una de las opciones de la modalidad */
    public boolean admitePlazo(Integer meses) {
        return meses != null && opcionesMesesEjecucion.contains(meses);
    }

    public String getPrefijoFormato() {
        return prefijoFormato;
    }
}
