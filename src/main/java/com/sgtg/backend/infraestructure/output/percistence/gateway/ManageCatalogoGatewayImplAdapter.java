package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.infraestructure.mapper.MapperCatalogo;
import com.sgtg.backend.infraestructure.output.percistence.entities.DepartamentoEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.ProgramaEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.DepartamentoRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.ProgramaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageCatalogoGatewayImplAdapter implements ManageCatalogoGatewayIntPort {

    private final DepartamentoRepository departamentoRepository;
    private final ProgramaRepository programaRepository;

    @Override
    public Departamento crearDepartamento(String nombre) {
        DepartamentoEntity entity = DepartamentoEntity.builder().nombre(nombre).build();
        return MapperCatalogo.toDomain(departamentoRepository.save(entity));
    }

    @Override
    public boolean existeDepartamento(String nombre) {
        return departamentoRepository.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public Departamento buscarDepartamento(Long id) {
        if (id == null)
            return null;
        return MapperCatalogo.toDomain(departamentoRepository.findById(id).orElse(null));
    }

    @Override
    public List<Departamento> listarDepartamentos() {
        return departamentoRepository.findAllByOrderByNombreAsc().stream().map(MapperCatalogo::toDomain).toList();
    }

    @Override
    public Programa crearPrograma(String nombre) {
        ProgramaEntity entity = ProgramaEntity.builder().nombre(nombre).build();
        return MapperCatalogo.toDomain(programaRepository.save(entity));
    }

    @Override
    public boolean existePrograma(String nombre) {
        return programaRepository.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public Programa buscarPrograma(Long id) {
        if (id == null)
            return null;
        return MapperCatalogo.toDomain(programaRepository.findById(id).orElse(null));
    }

    @Override
    public List<Programa> listarProgramas() {
        return programaRepository.findAllByOrderByNombreAsc().stream().map(MapperCatalogo::toDomain).toList();
    }
}
