package com.sgtg.backend.domain.models.comun;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;

/**
 * Período académico de la universidad. Por fines prácticos se toman dos
 * períodos por año: el primero de enero a junio y el segundo de julio a
 * diciembre.
 *
 * @param anio     año del período
 * @param semestre 1 (enero a junio) o 2 (julio a diciembre)
 */
public record PeriodoAcademico(int anio, int semestre) {

    public PeriodoAcademico {
        if (semestre != 1 && semestre != 2)
            throw new ReglaNegocioException("error.dominio.periodo_academico.semestre_invalido");
    }

    /**
     * @return el período académico al que pertenece la fecha
     */
    public static PeriodoAcademico de(LocalDate fecha) {
        if (fecha == null)
            throw new ReglaNegocioException("error.dominio.periodo.fechas_obligatorias");
        return new PeriodoAcademico(fecha.getYear(), fecha.getMonthValue() <= 6 ? 1 : 2);
    }

    /**
     * @return el período académico que sigue a este
     */
    public PeriodoAcademico siguiente() {
        return this.semestre == 1 ? new PeriodoAcademico(this.anio, 2) : new PeriodoAcademico(this.anio + 1, 1);
    }

    /**
     * @return primer día del período
     */
    public LocalDate inicio() {
        return LocalDate.of(this.anio, this.semestre == 1 ? 1 : 7, 1);
    }

    /**
     * @return último día del período
     */
    public LocalDate fin() {
        return this.semestre == 1 ? LocalDate.of(this.anio, 6, 30) : LocalDate.of(this.anio, 12, 31);
    }

    @Override
    public String toString() {
        return this.anio + "-" + this.semestre;
    }
}
