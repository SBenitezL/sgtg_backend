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
 * fecha, hora, salón y el formato E. El primer jurado es el **coordinador**:
 * califica la sesión, registra la inasistencia, verifica los ajustes y carga
 * la versión definitiva; el otro solo consulta. Hay máximo dos sesiones (la
 * segunda solo si la primera se aplaza).
 *
 * @param numero            1 o 2
 * @param jurados           los dos jurados; el primero es el coordinador
 * @param fechaHora         fecha y hora de la sesión
 * @param salon             salón
 * @param fechaProgramacion fecha en que se programó
 * @param rutaFormatoE      ruta del formato E que adjunta el Consejo al programar
 */
public record SesionSustentacion(int numero, List<Evaluador> jurados, LocalDateTime fechaHora, String salon,
        LocalDate fechaProgramacion, String rutaFormatoE) {

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
        if (CheckString.isBlank(rutaFormatoE))
            throw new ReglaNegocioException("error.dominio.sustentacion.formato_e_obligatorio");
        jurados = List.copyOf(jurados);
        salon = salon.trim();
        rutaFormatoE = rutaFormatoE.trim();
    }

    /**
     * @return true si el usuario es uno de los jurados de la sesión
     */
    public boolean esJurado(Long usuarioId) {
        return usuarioId != null && this.jurados.stream().anyMatch(j -> usuarioId.equals(j.getUsuarioId()));
    }

    /**
     * @return el jurado coordinador (el primero)
     */
    public Evaluador coordinador() {
        return this.jurados.get(0);
    }

    /**
     * @return true si el usuario es el jurado coordinador de la sesión
     */
    public boolean esCoordinador(Long usuarioId) {
        return usuarioId != null && usuarioId.equals(coordinador().getUsuarioId());
    }

    /**
     * @return la fecha de la sesión
     */
    public LocalDate fecha() {
        return this.fechaHora.toLocalDate();
    }

    /**
     * @param nuevaFechaHora nueva fecha y hora aprobada por el Consejo
     * @param nuevoSalon     nuevo salón; si está vacío se conserva el actual
     * @param fechaDecision  fecha de la decisión (la sesión no puede quedar antes)
     * @return la misma sesión con la nueva fecha (y salón)
     */
    public SesionSustentacion reprogramada(LocalDateTime nuevaFechaHora, String nuevoSalon, LocalDate fechaDecision) {
        if (nuevaFechaHora == null || fechaDecision == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.nueva_fecha_obligatoria");
        if (nuevaFechaHora.toLocalDate().isBefore(fechaDecision))
            throw new ReglaNegocioException("error.dominio.sustentacion.fecha_sesion_invalida");
        return new SesionSustentacion(numero, jurados, nuevaFechaHora,
                CheckString.isBlank(nuevoSalon) ? salon : nuevoSalon, fechaProgramacion, rutaFormatoE);
    }
}
