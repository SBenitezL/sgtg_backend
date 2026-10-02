package com.sgtg.backend.infraestructure.output.percistence.gateway;

import org.springframework.stereotype.Service;

import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.infraestructure.mapper.MapperPerfil;
import com.sgtg.backend.infraestructure.output.percistence.repositories.PerfilDocenteRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.PerfilEstudianteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagePerfilGatewayImplAdapter implements ManagePerfilGatewayIntPort {

    private final PerfilDocenteRepository perfilDocenteRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;

    @Override
    public PerfilDocente guardarPerfilDocente(PerfilDocente perfil) {
        return MapperPerfil.toDomain(perfilDocenteRepository.save(MapperPerfil.toEntity(perfil)));
    }

    @Override
    public PerfilDocente buscarPerfilDocente(Long usuarioId) {
        if (usuarioId == null)
            return null;
        return MapperPerfil.toDomain(perfilDocenteRepository.findById(usuarioId).orElse(null));
    }

    @Override
    public PerfilEstudiante guardarPerfilEstudiante(PerfilEstudiante perfil) {
        return MapperPerfil.toDomain(perfilEstudianteRepository.save(MapperPerfil.toEntity(perfil)));
    }

    @Override
    public PerfilEstudiante buscarPerfilEstudiante(Long usuarioId) {
        if (usuarioId == null)
            return null;
        return MapperPerfil.toDomain(perfilEstudianteRepository.findById(usuarioId).orElse(null));
    }
}
