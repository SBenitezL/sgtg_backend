package com.sgtg.backend.domain.models.comun;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

/**
 * Rango de fechas inmutable, usado para el tiempo estipulado del trabajo de
 * grado y sus extensiones.
 *
 * @param inicio fecha de inicio, obligatoria
 * @param fin    fecha de fin, obligatoria y posterior al inicio
 */
public record Periodo(LocalDate inicio, LocalDate fin) {

    public Periodo {
        if (inicio == null || fin == null)
            throw new ReglaNegocioException("error.dominio.periodo.fechas_obligatorias");
        if (!fin.isAfter(inicio))
            throw new ReglaNegocioException("error.dominio.periodo.fin_antes_de_inicio");
    }

    /**
     * Crea un periodo que inicia en la fecha dada y dura la cantidad de meses
     * indicada.
     */
    public static Periodo desde(LocalDate inicio, int meses) {
        if (inicio == null)
            throw new ReglaNegocioException("error.dominio.periodo.fechas_obligatorias");
        if (meses <= 0)
            throw new ReglaNegocioException("error.dominio.periodo.meses_invalidos");
        return new Periodo(inicio, inicio.plusMonths(meses));
    }

    /**
     * @param meses cantidad de meses a extender, debe ser mayor a cero
     * @return un nuevo periodo con la misma fecha de inicio y el fin extendido
     */
    public Periodo extenderMeses(int meses) {
        if (meses <= 0)
            throw new ReglaNegocioException("error.dominio.periodo.meses_invalidos");
        return new Periodo(this.inicio, this.fin.plusMonths(meses));
    }

    /**
     * Días calendario que faltan desde la fecha dada hasta el fin del periodo.
     * Se usa para validar el plazo de la prórroga y para guardar el tiempo
     * restante al suspender.
     *
     * @return días restantes, 0 si el periodo ya terminó
     */
    public long diasRestantes(LocalDate fecha) {
        return Math.max(0, ChronoUnit.DAYS.between(fecha, this.fin));
    }

    /**
     * Recalcula el fin al reanudar un periodo suspendido: el nuevo fin es la
     * fecha de reanudación más los días que faltaban al suspender.
     *
     * @param fecha fecha de reanudación
     * @param dias  días restantes guardados al suspender, mayor a cero
     * @return un nuevo periodo con la misma fecha de inicio
     */
    public Periodo reanudarDesde(LocalDate fecha, long dias) {
        if (fecha == null)
            throw new ReglaNegocioException("error.dominio.periodo.fechas_obligatorias");
        if (dias <= 0)
            throw new ReglaNegocioException("error.dominio.periodo.dias_invalidos");
        return new Periodo(this.inicio, fecha.plusDays(dias));
    }

    /**
     * @return true si desde el inicio hasta la fecha dada han transcurrido al
     *         menos los meses indicados (ej. 3 meses mínimos para sustentar)
     */
    public boolean haTranscurrido(int meses, LocalDate fecha) {
        return !fecha.isBefore(this.inicio.plusMonths(meses));
    }

    /**
     * @return true si la fecha es posterior al fin del periodo
     */
    public boolean vencido(LocalDate fecha) {
        return fecha.isAfter(this.fin);
    }

    /**
     * @return true si la fecha está dentro del periodo (ambos extremos incluidos)
     */
    public boolean contiene(LocalDate fecha) {
        return !fecha.isBefore(this.inicio) && !fecha.isAfter(this.fin);
    }
}
