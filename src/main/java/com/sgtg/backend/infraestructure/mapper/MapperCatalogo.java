package com.sgtg.backend.infraestructure.mapper;

import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.infraestructure.output.percistence.entities.DepartamentoEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.ProgramaEntity;

/** Conversión entre los catálogos de dominio y sus entidades. */
public abstract class MapperCatalogo {

    public static Departamento toDomain(DepartamentoEntity entity) {
        if (entity == null)
            return null;
        return new Departamento(entity.getId(), entity.getNombre());
    }

    public static DepartamentoEntity toEntity(Departamento departamento) {
        if (departamento == null)
            return null;
        return new DepartamentoEntity(departamento.id(), departamento.nombre());
    }

    public static Programa toDomain(ProgramaEntity entity) {
        if (entity == null)
            return null;
        return new Programa(entity.getId(), entity.getNombre(), toDomain(entity.getDepartamento()));
    }

    public static ProgramaEntity toEntity(Programa programa) {
        if (programa == null)
            return null;
        return new ProgramaEntity(programa.id(), programa.nombre(), toEntity(programa.departamento()));
    }
}
