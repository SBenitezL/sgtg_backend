package com.sgtg.backend.application.input.calendario;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.sgtg.backend.domain.models.comun.DiaNoHabil;

/**
 * Puerto de entrada para administrar los días no hábiles institucionales y
 * consultar los festivos de ley.
 */
public interface ManageCalendarioCUIntPort {

    /**
     * Registra un día no hábil institucional (receso, cierre, etc.).
     *
     * @return el día registrado
     */
    public DiaNoHabil registrarDiaNoHabil(LocalDate fecha, String descripcion);

    /** Elimina un día no hábil institucional registrado. */
    public void eliminarDiaNoHabil(LocalDate fecha);

    /** @return los días no hábiles institucionales del rango, ambos extremos incluidos */
    public List<DiaNoHabil> listarDiasNoHabiles(LocalDate desde, LocalDate hasta);

    /** @return los festivos de ley del año */
    public Set<LocalDate> festivos(int anio);
}
