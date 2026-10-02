package com.sgtg.backend.infraestructure.mapper;

import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.infraestructure.output.percistence.entities.PerfilDocenteEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.PerfilEstudianteEntity;

/** Conversión entre los perfiles académicos de dominio y sus entidades. */
public abstract class MapperPerfil {

    public static PerfilDocente toDomain(PerfilDocenteEntity entity) {
        if (entity == null)
            return null;
        return new PerfilDocente(entity.getUsuarioId(), entity.getTipoVinculacion(),
                MapperCatalogo.toDomain(entity.getDepartamento()));
    }

    public static PerfilDocenteEntity toEntity(PerfilDocente perfil) {
        return new PerfilDocenteEntity(perfil.getUsuarioId(), perfil.getTipoVinculacion(),
                MapperCatalogo.toEntity(perfil.getDepartamento()));
    }

    public static PerfilEstudiante toDomain(PerfilEstudianteEntity entity) {
        if (entity == null)
            return null;
        return new PerfilEstudiante(entity.getUsuarioId(), MapperCatalogo.toDomain(entity.getPrograma()));
    }

    public static PerfilEstudianteEntity toEntity(PerfilEstudiante perfil) {
        return new PerfilEstudianteEntity(perfil.getUsuarioId(), MapperCatalogo.toEntity(perfil.getPrograma()));
    }
}
