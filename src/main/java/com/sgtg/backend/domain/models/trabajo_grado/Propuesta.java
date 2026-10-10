package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.services.CalendarioHabil;

/**
 * Etapa de propuesta del trabajo de grado (Arts. 10-11, 32-33): la idea se
 * radica en el formato A ante el Comité de Programa, que dispone de 10 días
 * hábiles para emitir su concepto: APROBADO o NO_APROBADO. Si no es aprobada se
 * radica una nueva versión hasta que lo sea.
 *
 * Sus operaciones que modifican el estado solo son accesibles desde el
 * paquete, para que únicamente {@link TrabajoGrado} las invoque.
 */
public class Propuesta {
    /** Días hábiles que tiene el Comité de Programa para emitir concepto (Art. 11) */
    public static final int DIAS_HABILES_CONCEPTO_COMITE = 10;

    /** Versiones radicadas del documento de propuesta (formato A) */
    private final List<Documento> versiones;
    /** Decisiones del Comité de Programa sobre cada versión */
    private final List<Decision> decisiones;
    /** Estado de la versión actual, null mientras no se haya radicado */
    private Estado estado;
    /** Fecha límite para que el Comité emita concepto sobre la versión actual */
    private LocalDate fechaLimiteConcepto;

    /** Crea una propuesta sin radicar. */
    Propuesta() {
        this(DatosPropuesta.vacia());
    }

    /** Reconstruye la propuesta con su estado persistido. */
    Propuesta(DatosPropuesta datos) {
        this.versiones = new ArrayList<>(datos.versiones());
        this.decisiones = new ArrayList<>(datos.decisiones());
        this.estado = datos.estado();
        this.fechaLimiteConcepto = datos.fechaLimiteConcepto();
    }

    /**
     * Radica una nueva versión de la propuesta. Se permite la primera vez o
     * después de que el Comité no la apruebe.
     *
     * @param ruta       ruta del documento en el almacenamiento de archivos
     * @param hoy        fecha de radicación
     * @param calendario calendario para calcular la fecha límite del concepto
     */
    void radicar(String ruta, LocalDate hoy, CalendarioHabil calendario) {
        if (hoy == null || calendario == null)
            throw new ReglaNegocioException("error.dominio.propuesta.datos_incompletos");
        if (!puedeRadicar())
            throw new ReglaNegocioException("error.dominio.propuesta.radicacion_no_permitida");
        Documento documento = this.versiones.isEmpty()
                ? Documento.inicial(TipoDocumento.PROPUESTA, ruta, hoy)
                : this.versiones.get(this.versiones.size() - 1).nuevaVersion(ruta, hoy);
        this.versiones.add(documento);
        this.estado = Estado.RADICADO;
        this.fechaLimiteConcepto = calendario.sumarDiasHabiles(hoy, DIAS_HABILES_CONCEPTO_COMITE);
    }

    /**
     * Registra el concepto del Comité de Programa sobre la versión actual.
     *
     * @param decision decisión del Comité: APROBADO o NO_APROBADO
     */
    void registrarDecision(Decision decision) {
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.propuesta.datos_incompletos");
        if (!decision.emitidaPor(Organo.COMITE_PROGRAMA))
            throw new ReglaNegocioException("error.dominio.propuesta.organo_invalido");
        if (this.estado != Estado.RADICADO && this.estado != Estado.EN_REVISION)
            throw new ReglaNegocioException("error.dominio.propuesta.decision_no_permitida");
        if (decision.fecha().isBefore(this.versionActual().get().fechaCarga()))
            throw new ReglaNegocioException("error.dominio.propuesta.fecha_decision_invalida");
        Estado resultado = switch (decision.resultado()) {
            case APROBADO -> Estado.APROBADO;
            case NO_APROBADO -> Estado.NO_APROBADO;
            default -> throw new ReglaNegocioException("error.dominio.propuesta.resultado_invalido");
        };
        this.decisiones.add(decision);
        this.estado = resultado;
    }

    /**
     * Una propuesta no aprobada es una entrega cerrada; la nueva versión inicia
     * una entrega nueva (regla del negocio: se presenta una nueva propuesta).
     */
    boolean puedeRadicar() {
        return this.estado == null || this.estado == Estado.NO_APROBADO;
    }

    /**
     * @return true si el Comité aprobó la versión actual
     */
    public boolean estaAprobada() {
        return this.estado == Estado.APROBADO;
    }

    /**
     * Indica si el Comité superó el plazo para emitir concepto. Es informativo
     * (alertas): el vencimiento no cambia la etapa del trabajo.
     */
    public boolean conceptoVencido(LocalDate hoy) {
        boolean pendiente = this.estado == Estado.RADICADO || this.estado == Estado.EN_REVISION;
        return pendiente && hoy.isAfter(this.fechaLimiteConcepto);
    }

    /**
     * @return la última versión radicada, vacía si no se ha radicado ninguna
     */
    public Optional<Documento> versionActual() {
        if (this.versiones.isEmpty())
            return Optional.empty();
        return Optional.of(this.versiones.get(this.versiones.size() - 1));
    }

    public List<Documento> getVersiones() {
        return Collections.unmodifiableList(versiones);
    }

    public List<Decision> getDecisiones() {
        return Collections.unmodifiableList(decisiones);
    }

    public Estado getEstado() {
        return estado;
    }

    public LocalDate getFechaLimiteConcepto() {
        return fechaLimiteConcepto;
    }
}
