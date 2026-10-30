package com.sgtg.backend.infraestructure.output.percistence.gateway;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageAuditoriaGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.RegistroAuditoria;
import com.sgtg.backend.infraestructure.output.percistence.entities.AuditoriaEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.AuditoriaRepository;

import lombok.RequiredArgsConstructor;

/** Guarda cada registro de auditoría en su propia transacción. */
@Service
@RequiredArgsConstructor
public class ManageAuditoriaGatewayImplAdapter implements ManageAuditoriaGatewayIntPort {

    private final AuditoriaRepository auditoriaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(RegistroAuditoria r) {
        auditoriaRepository.save(new AuditoriaEntity(null, r.fecha(), r.usuarioId(), r.usuario(), r.roles(), r.sesion(), r.ip(),
                r.agenteUsuario(), r.metodo(), r.ruta(), r.operacion(), r.trabajoId(), r.parametros(), r.estadoHttp(),
                r.resultado()));
    }
}
