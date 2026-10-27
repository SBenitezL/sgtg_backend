package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageTrabajoGradoGatewayIntPort;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.infraestructure.mapper.MapperTrabajoGrado;
import com.sgtg.backend.infraestructure.output.percistence.repositories.TrabajoGradoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Persistencia del agregado trabajo de grado. Las colecciones se cargan de
 * forma perezosa, por eso la conversión a dominio ocurre dentro de la
 * transacción (también fuera de una petición web, como en la tarea programada).
 * El guardado verifica la versión con la que se leyó el trabajo: si otra
 * operación lo modificó entre tanto, falla con
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Service
@RequiredArgsConstructor
public class ManageTrabajoGradoGatewayImplAdapter implements ManageTrabajoGradoGatewayIntPort {

    private final TrabajoGradoRepository trabajoGradoRepository;

    @Override
    @Transactional
    public TrabajoGrado guardar(TrabajoGrado trabajo) {
        // Se sincroniza de inmediato para devolver la versión nueva y detectar aquí el conflicto
        return MapperTrabajoGrado.toDomain(trabajoGradoRepository.saveAndFlush(MapperTrabajoGrado.toEntity(trabajo)));
    }

    @Override
    @Transactional(readOnly = true)
    public TrabajoGrado buscarPorId(Long trabajoId) {
        if (trabajoId == null)
            return null;
        return MapperTrabajoGrado.toDomain(trabajoGradoRepository.findById(trabajoId).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajoGrado> listarPorParticipante(Long usuarioId) {
        return trabajoGradoRepository.findByParticipante(usuarioId).stream().map(MapperTrabajoGrado::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajoGrado> listarPorEtapa(Proceso etapa) {
        return trabajoGradoRepository.findByEtapaOrderByIdDesc(etapa).stream().map(MapperTrabajoGrado::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajoGrado> listarTodos() {
        return trabajoGradoRepository.findAllByOrderByIdDesc().stream().map(MapperTrabajoGrado::toDomain).toList();
    }
}
