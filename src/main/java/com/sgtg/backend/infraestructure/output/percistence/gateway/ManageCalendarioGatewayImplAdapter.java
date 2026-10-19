package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sgtg.backend.application.output.ManageCalendarioGatewayIntPort;
import com.sgtg.backend.domain.models.comun.DiaNoHabil;
import com.sgtg.backend.infraestructure.output.calendario.CalendarioHabilImplAdapter;
import com.sgtg.backend.infraestructure.output.percistence.entities.DiaNoHabilEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.DiaNoHabilRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageCalendarioGatewayImplAdapter implements ManageCalendarioGatewayIntPort {

    private final DiaNoHabilRepository diaNoHabilRepository;
    /** Se le avisa cada cambio para que recargue los días no hábiles que tiene en memoria. */
    private final CalendarioHabilImplAdapter calendarioHabil;

    @Override
    public DiaNoHabil guardar(DiaNoHabil dia) {
        DiaNoHabilEntity guardado = diaNoHabilRepository.save(new DiaNoHabilEntity(dia.fecha(), dia.descripcion()));
        calendarioHabil.invalidar();
        return new DiaNoHabil(guardado.getFecha(), guardado.getDescripcion());
    }

    @Override
    public boolean existe(LocalDate fecha) {
        return fecha != null && diaNoHabilRepository.existsById(fecha);
    }

    @Override
    public void eliminar(LocalDate fecha) {
        diaNoHabilRepository.deleteById(fecha);
        calendarioHabil.invalidar();
    }

    @Override
    public List<DiaNoHabil> listar(LocalDate desde, LocalDate hasta) {
        return diaNoHabilRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta).stream()
                .map(entity -> new DiaNoHabil(entity.getFecha(), entity.getDescripcion()))
                .toList();
    }
}
