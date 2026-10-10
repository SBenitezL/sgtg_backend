package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Sesión de sustentación programada por el Consejo de Facultad: dos jurados,
 * fecha, hora y salón. Hay máximo dos sesiones (la segunda solo si la primera
 * se aplaza).
 *
 * @param numero            1 o 2
 * @param jurados           los dos jurados
 * @param fechaHora         fecha y hora de la sesión
 * @param salon             salón
 * @param fechaProgramacion fecha en que se programó
 */
public record SesionSustentacion(int numero, List<Evaluador> jurados, LocalDateTime fechaHora, String salon,
        LocalDate fechaProgramacion) {

    public SesionSustentacion {
        if (numero < 1 || numero > Sustentacion.MAXIMO_SESIONES || fechaHora == null || CheckString.isBlank(salon)
                || fechaProgramacion == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.programacion_incompleta");
        if (jurados == null || jurados.size() != 2 || jurados.stream().anyMatch(Objects::isNull))
            throw new ReglaNegocioException("error.dominio.sustentacion.jurados_obligatorios");
        if (jurados.get(0).esMismaPersona(jurados.get(1)))
            throw new ReglaNegocioException("error.dominio.sustentacion.jurados_repetidos");
        if (fechaHora.toLocalDate().isBefore(fechaProgramacion))
            throw new ReglaNegocioException("error.dominio.sustentacion.fecha_sesion_invalida");
        jurados = List.copyOf(jurados);
        salon = salon.trim();
    }

    /**
     * @return true si el usuario es uno de los jurados de la sesión
     */
    public boolean esJurado(Long usuarioId) {
        return usuarioId != null && this.jurados.stream().anyMatch(j -> usuarioId.equals(j.getUsuarioId()));
    }

    /**
     * @return la fecha de la sesión
     */
    public LocalDate fecha() {
        return this.fechaHora.toLocalDate();
    }
}
