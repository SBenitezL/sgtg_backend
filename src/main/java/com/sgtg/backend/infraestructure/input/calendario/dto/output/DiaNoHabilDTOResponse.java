package com.sgtg.backend.infraestructure.input.calendario.dto.output;

import java.time.LocalDate;

import com.sgtg.backend.domain.models.comun.DiaNoHabil;

public record DiaNoHabilDTOResponse(LocalDate fecha, String descripcion) {

    public static DiaNoHabilDTOResponse desde(DiaNoHabil dia) {
        return new DiaNoHabilDTOResponse(dia.fecha(), dia.descripcion());
    }
}
