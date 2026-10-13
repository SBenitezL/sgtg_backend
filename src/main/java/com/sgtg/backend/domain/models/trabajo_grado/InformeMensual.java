package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Informe mensual de avance del trabajo de grado (Art. 45), en ambas
 * modalidades. Lo radica un estudiante (en práctica profesional también el
 * director) con el documento adjunto y, opcionalmente, sus observaciones. El
 * director, el codirector y el asesor pueden registrar una revisión cada uno;
 * las revisiones son una formalidad y no cambian el estado del trabajo.
 *
 * @param numero        consecutivo del informe, inicia en 1
 * @param rutaArchivo   ruta del documento del informe
 * @param fecha         fecha de radicación
 * @param radicadoPor   código de quien radicó el informe, null en datos anteriores
 * @param observaciones observaciones de quien lo radicó, null si no las hay
 * @param revisiones    revisiones del director, codirector o asesor, en orden
 */
public record InformeMensual(int numero, String rutaArchivo, LocalDate fecha, String radicadoPor,
        String observaciones, List<RevisionInforme> revisiones) {

    public InformeMensual {
        if (CheckString.isBlank(rutaArchivo))
            throw new ReglaNegocioException("error.dominio.informe_mensual.documento_obligatorio");
        if (numero < 1 || fecha == null)
            throw new ReglaNegocioException("error.dominio.informe_mensual.datos_incompletos");
        rutaArchivo = rutaArchivo.trim();
        observaciones = CheckString.isBlank(observaciones) ? null : observaciones.trim();
        revisiones = revisiones == null ? List.of() : List.copyOf(revisiones);
    }

    /** Informe sin observaciones ni revisiones (datos anteriores). */
    public InformeMensual(int numero, String rutaArchivo, LocalDate fecha) {
        this(numero, rutaArchivo, fecha, null, null, List.of());
    }

    /**
     * @param usuarioId usuario a verificar
     * @return true si el usuario ya revisó este informe
     */
    public boolean revisadoPor(Long usuarioId) {
        return this.revisiones.stream().anyMatch(revision -> revision.revisorUsuarioId().equals(usuarioId));
    }

    /** Copia del informe con una revisión más. */
    InformeMensual conRevision(RevisionInforme revision) {
        List<RevisionInforme> nuevas = new ArrayList<>(this.revisiones);
        nuevas.add(revision);
        return new InformeMensual(this.numero, this.rutaArchivo, this.fecha, this.radicadoPor, this.observaciones,
                nuevas);
    }
}
