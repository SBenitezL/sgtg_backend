package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Prórroga del plazo de ejecución (Arts. 16, 41): la solicita el director, una
 * sola vez, por 1 a 3 meses, con el documento de la solicitud adjunto, y la
 * decide el Consejo de Facultad con la resolución adjunta. Si se aprueba, el
 * plazo se extiende por los meses solicitados.
 *
 * @param meses          meses solicitados, de 1 a 3
 * @param rutaSolicitud  ruta del documento de la solicitud
 * @param fechaSolicitud fecha de la solicitud
 * @param decision       decisión del Consejo, null mientras esté pendiente
 */
public record Prorroga(int meses, String rutaSolicitud, LocalDate fechaSolicitud, Decision decision) {

    /** Meses mínimos de una prórroga */
    public static final int MESES_MINIMOS = 1;
    /** Meses máximos de una prórroga (Art. 16) */
    public static final int MESES_MAXIMOS = 3;

    public Prorroga {
        if (meses < MESES_MINIMOS || meses > MESES_MAXIMOS)
            throw new ReglaNegocioException("error.dominio.prorroga.meses_invalidos");
        if (CheckString.isBlank(rutaSolicitud))
            throw new ReglaNegocioException("error.dominio.prorroga.documento_obligatorio");
        if (fechaSolicitud == null)
            throw new ReglaNegocioException("error.dominio.prorroga.datos_incompletos");
        rutaSolicitud = rutaSolicitud.trim();
        if (decision != null)
            validarDecision(decision, fechaSolicitud);
    }

    /** Crea una solicitud pendiente de decisión. */
    public static Prorroga solicitar(int meses, String rutaSolicitud, LocalDate fecha) {
        return new Prorroga(meses, rutaSolicitud, fecha, null);
    }

    private static void validarDecision(Decision decision, LocalDate fechaSolicitud) {
        if (!decision.emitidaPor(Organo.CONSEJO_FACULTAD))
            throw new ReglaNegocioException("error.dominio.prorroga.organo_invalido");
        if (decision.rutaArchivo() == null)
            throw new ReglaNegocioException("error.dominio.prorroga.resolucion_obligatoria");
        if (decision.resultado() != ResultadoConcepto.APROBADO
                && decision.resultado() != ResultadoConcepto.NO_APROBADO)
            throw new ReglaNegocioException("error.dominio.prorroga.resultado_invalido");
        if (decision.fecha().isBefore(fechaSolicitud))
            throw new ReglaNegocioException("error.dominio.prorroga.fecha_invalida");
    }

    /**
     * @param decision decisión del Consejo de Facultad con la resolución adjunta
     * @return la prórroga resuelta
     */
    public Prorroga resolver(Decision decision) {
        if (!estaPendiente())
            throw new ReglaNegocioException("error.dominio.prorroga.sin_solicitud_pendiente");
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.prorroga.datos_incompletos");
        return new Prorroga(meses, rutaSolicitud, fechaSolicitud, decision);
    }

    public boolean estaPendiente() {
        return this.decision == null;
    }

    public boolean fueAprobada() {
        return this.decision != null && this.decision.esAprobatoria();
    }
}
