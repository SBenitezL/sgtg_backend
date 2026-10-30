package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageIntentosAccesoGatewayIntPort;
import com.sgtg.backend.domain.models.acceso.IntentosAcceso;
import com.sgtg.backend.infraestructure.output.percistence.entities.IntentoAccesoEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.IntentoAccesoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManageIntentosAccesoGatewayImplAdapter implements ManageIntentosAccesoGatewayIntPort {

    private final IntentoAccesoRepository intentoAccesoRepository;

    @Override
    public IntentosAcceso obtener(String correo) {
        return intentoAccesoRepository.findById(correo)
                .map(i -> new IntentosAcceso(i.getCorreo(), i.getFallos(), i.getUltimoFallo(), i.getBloqueadoHasta()))
                .orElseGet(() -> IntentosAcceso.sinFallos(correo));
    }

    @Override
    public void guardar(IntentosAcceso intentos) {
        intentoAccesoRepository.save(new IntentoAccesoEntity(intentos.getCorreo(), intentos.getFallos(),
                intentos.getUltimoFallo(), intentos.getBloqueadoHasta()));
    }

    @Override
    public void eliminar(String correo) {
        intentoAccesoRepository.deleteById(correo);
    }

    @Override
    @Transactional
    public int depurar(LocalDateTime antesDe) {
        return intentoAccesoRepository.depurar(antesDe);
    }
}
