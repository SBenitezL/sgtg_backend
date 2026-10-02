package com.sgtg.backend.application.output;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.comun.DiaNoHabil;

/** Puerto de salida para la persistencia de los días no hábiles institucionales. */
public interface ManageCalendarioGatewayIntPort {

    public DiaNoHabil guardar(DiaNoHabil dia);

    public boolean existe(LocalDate fecha);

    public void eliminar(LocalDate fecha);

    /** @return los días del rango, ambos extremos incluidos, ordenados por fecha */
    public List<DiaNoHabil> listar(LocalDate desde, LocalDate hasta);
}
