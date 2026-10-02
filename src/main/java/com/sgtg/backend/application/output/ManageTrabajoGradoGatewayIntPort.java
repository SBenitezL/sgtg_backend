package com.sgtg.backend.application.output;

import java.util.List;

import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;

/** Puerto de salida para la persistencia del agregado trabajo de grado. */
public interface ManageTrabajoGradoGatewayIntPort {

    /**
     * Guarda el trabajo completo (crea o actualiza).
     *
     * @return el trabajo guardado, con su identificador
     */
    public TrabajoGrado guardar(TrabajoGrado trabajo);

    /** @return el trabajo, o null si no existe */
    public TrabajoGrado buscarPorId(Long trabajoId);

    /** @return los trabajos en los que el usuario es estudiante, director, codirector o asesor */
    public List<TrabajoGrado> listarPorParticipante(Long usuarioId);
}
