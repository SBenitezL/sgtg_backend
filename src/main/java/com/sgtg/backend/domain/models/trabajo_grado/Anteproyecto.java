package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Etapa de anteproyecto y evaluación del trabajo de grado (Arts. 12-13,
 * 37-38). El director radica el anteproyecto ante el Departamento, que asigna
 * dos evaluadores con 10 días hábiles para emitir concepto. El anteproyecto no
 * se rechaza: cada evaluador lo aprueba o lo devuelve con correcciones. Si
 * alguno pide correcciones, el director tiene 10 días hábiles para radicar una
 * nueva versión, que revisan los mismos evaluadores.
 *
 * Aprobado por ambos evaluadores, el Consejo de Facultad emite su concepto
 * (Art. 14) con la resolución adjunta: APROBADO o NO_APROBADO. Si no lo
 * aprueba, el director radica una nueva versión que vuelve a los evaluadores.
 *
 * Sus operaciones que modifican el estado solo son accesibles desde el
 * paquete, para que únicamente {@link TrabajoGrado} las invoque.
 */
public class Anteproyecto {
    /** Días hábiles que tienen los evaluadores para emitir concepto (Art. 13) */
    public static final int DIAS_HABILES_EVALUACION = 10;
    /** Días hábiles que tiene el director para reenviar el anteproyecto corregido (Art. 13) */
    public static final int DIAS_HABILES_CORRECCIONES = 10;
    /** Cantidad de evaluadores que asigna el Departamento (Art. 13) */
    public static final int CANTIDAD_EVALUADORES = 2;

    /** Versiones radicadas del anteproyecto */
    private final List<Documento> versiones;
    /** Evaluadores asignados por el Departamento */
    private final List<Evaluador> evaluadores;
    /** Conceptos de los evaluadores sobre cada versión */
    private final List<ConceptoEvaluacion> conceptos;
    /** Decisiones del Consejo de Facultad sobre las versiones aprobadas por los evaluadores */
    private final List<Decision> decisionesConsejo;
    /** Estado de la versión actual, null mientras no se haya radicado */
    private Estado estado;
    /** Fecha límite para que los evaluadores emitan concepto sobre la versión actual */
    private LocalDate fechaLimiteEvaluacion;
    /** Fecha límite para que el director reenvíe el anteproyecto corregido */
    private LocalDate fechaLimiteCorrecciones;

    /** Crea un anteproyecto sin radicar. */
    Anteproyecto() {
        this(DatosAnteproyecto.vacio());
    }

    /** Reconstruye el anteproyecto con su estado persistido. */
    Anteproyecto(DatosAnteproyecto datos) {
        this.versiones = new ArrayList<>(datos.versiones());
        this.evaluadores = new ArrayList<>(datos.evaluadores());
        this.conceptos = new ArrayList<>(datos.conceptos());
        this.decisionesConsejo = new ArrayList<>(datos.decisionesConsejo());
        this.estado = datos.estado();
        this.fechaLimiteEvaluacion = datos.fechaLimiteEvaluacion();
        this.fechaLimiteCorrecciones = datos.fechaLimiteCorrecciones();
    }

    /**
     * Radica una versión del anteproyecto. Se permite la primera vez, después
     * de que algún evaluador solicite correcciones o después de que el Consejo
     * de Facultad no lo apruebe. Si los evaluadores ya
     * fueron asignados, la nueva versión queda de inmediato en revisión.
     *
     * @param ruta       ruta del documento en el almacenamiento de archivos
     * @param hoy        fecha de radicación
     * @param calendario calendario para calcular el plazo de los evaluadores
     */
    void radicar(String ruta, LocalDate hoy, CalendarioHabil calendario) {
        if (hoy == null || calendario == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        if (!puedeRadicar())
            throw new ReglaNegocioException("error.dominio.anteproyecto.radicacion_no_permitida");
        Documento documento = this.versiones.isEmpty()
                ? Documento.inicial(TipoDocumento.ANTEPROYECTO, ruta, hoy)
                : this.versiones.get(this.versiones.size() - 1).nuevaVersion(ruta, hoy);
        this.versiones.add(documento);
        this.fechaLimiteCorrecciones = null;
        if (tieneEvaluadores()) {
            this.estado = Estado.EN_REVISION;
            this.fechaLimiteEvaluacion = calendario.sumarDiasHabiles(hoy, DIAS_HABILES_EVALUACION);
        } else {
            this.estado = Estado.RADICADO;
        }
    }

    /**
     * Valida y registra los dos evaluadores asignados por el Departamento. Las
     * restricciones que dependen de los participantes del trabajo (que no sean
     * director, codirector, asesor ni estudiantes) las valida el agregado.
     *
     * @param evaluadores los dos evaluadores
     * @param hoy         fecha de asignación
     * @param calendario  calendario para calcular el plazo de los evaluadores
     */
    void asignarEvaluadores(List<Evaluador> evaluadores, LocalDate hoy, CalendarioHabil calendario) {
        validarAsignacion(evaluadores, hoy, calendario);
        this.evaluadores.addAll(evaluadores);
        this.estado = Estado.EN_REVISION;
        this.fechaLimiteEvaluacion = calendario.sumarDiasHabiles(hoy, DIAS_HABILES_EVALUACION);
    }

    /**
     * Verifica que se puedan asignar los evaluadores, sin modificar el estado.
     */
    void validarAsignacion(List<Evaluador> evaluadores, LocalDate hoy, CalendarioHabil calendario) {
        if (hoy == null || calendario == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        if (this.estado != Estado.RADICADO || tieneEvaluadores())
            throw new ReglaNegocioException("error.dominio.anteproyecto.asignacion_no_permitida");
        if (evaluadores == null || evaluadores.size() != CANTIDAD_EVALUADORES
                || evaluadores.stream().anyMatch(evaluador -> evaluador == null))
            throw new ReglaNegocioException("error.dominio.anteproyecto.evaluadores_obligatorios");
        if (evaluadores.get(0).esMismaPersona(evaluadores.get(1)))
            throw new ReglaNegocioException("error.dominio.anteproyecto.evaluadores_repetidos");
        if (hoy.isBefore(this.versionActual().get().fechaCarga()))
            throw new ReglaNegocioException("error.dominio.anteproyecto.fecha_invalida");
    }

    /**
     * Calcula el estado que tendrá la versión actual al registrar el concepto,
     * validando el concepto sin modificar el anteproyecto.
     *
     * @param concepto concepto de uno de los evaluadores asignados
     * @return EN_REVISION si falta el concepto del otro evaluador, APROBADO si
     *         ambos aprueban o CON_CORRECCIONES si alguno pide correcciones
     */
    Estado estadoTrasConcepto(Concepto concepto) {
        if (concepto == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        if (this.estado != Estado.EN_REVISION)
            throw new ReglaNegocioException("error.dominio.anteproyecto.concepto_no_permitido");
        if (this.evaluadores.stream().noneMatch(evaluador -> evaluador.esMismaPersona(concepto.emisor())))
            throw new ReglaNegocioException("error.dominio.anteproyecto.evaluador_no_asignado");
        if (concepto.resultado() != ResultadoConcepto.APROBADO
                && concepto.resultado() != ResultadoConcepto.CON_CORRECCIONES)
            throw new ReglaNegocioException("error.dominio.anteproyecto.resultado_invalido");
        List<ConceptoEvaluacion> ronda = conceptosVersionActual();
        if (ronda.stream().anyMatch(previo -> previo.concepto().emisor().esMismaPersona(concepto.emisor())))
            throw new ReglaNegocioException("error.dominio.anteproyecto.concepto_repetido");
        if (concepto.fecha().isBefore(this.versionActual().get().fechaCarga()))
            throw new ReglaNegocioException("error.dominio.anteproyecto.fecha_invalida");

        if (ronda.size() + 1 < CANTIDAD_EVALUADORES)
            return Estado.EN_REVISION;
        boolean ambosAprueban = concepto.esAprobado()
                && ronda.stream().allMatch(previo -> previo.concepto().esAprobado());
        return ambosAprueban ? Estado.APROBADO : Estado.CON_CORRECCIONES;
    }

    /**
     * Registra el concepto de un evaluador sobre la versión actual. Cuando ambos
     * evaluadores han emitido concepto, la versión queda aprobada o con
     * correcciones; en este último caso se calcula el plazo del director.
     *
     * @param concepto   concepto del evaluador
     * @param rutaFormato ruta del formato B, null si no se cargó
     * @param calendario calendario para calcular el plazo de las correcciones
     */
    void registrarConcepto(Concepto concepto, String rutaFormato, CalendarioHabil calendario) {
        if (calendario == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        Estado resultado = estadoTrasConcepto(concepto);
        int version = this.versionActual().get().version();
        Documento formato = CheckString.isBlank(rutaFormato) ? null
                : new Documento(TipoDocumento.EVALUACION_ANTEPROYECTO, rutaFormato.trim(), version, concepto.fecha());
        this.conceptos.add(new ConceptoEvaluacion(version, concepto, formato));
        this.estado = resultado;
        if (resultado == Estado.CON_CORRECCIONES)
            this.fechaLimiteCorrecciones = calendario.sumarDiasHabiles(concepto.fecha(), DIAS_HABILES_CORRECCIONES);
    }

    /**
     * Valida la decisión del Consejo de Facultad sobre la versión aprobada por
     * los evaluadores, sin modificar el anteproyecto.
     *
     * @param decision decisión del Consejo con la ruta de la resolución
     * @return APROBADO o NO_APROBADO
     */
    Estado estadoTrasDecisionConsejo(Decision decision) {
        if (decision == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        if (!decision.emitidaPor(Organo.CONSEJO_FACULTAD))
            throw new ReglaNegocioException("error.dominio.anteproyecto.organo_invalido");
        if (this.estado != Estado.APROBADO)
            throw new ReglaNegocioException("error.dominio.anteproyecto.decision_consejo_no_permitida");
        if (decision.rutaArchivo() == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.resolucion_obligatoria");
        if (conceptosVersionActual().stream().anyMatch(c -> decision.fecha().isBefore(c.concepto().fecha())))
            throw new ReglaNegocioException("error.dominio.anteproyecto.fecha_invalida");
        return switch (decision.resultado()) {
            case APROBADO -> Estado.APROBADO;
            case NO_APROBADO -> Estado.NO_APROBADO;
            default -> throw new ReglaNegocioException("error.dominio.anteproyecto.resultado_consejo_invalido");
        };
    }

    /**
     * Registra la decisión del Consejo de Facultad. Si no aprueba, la versión
     * queda NO_APROBADO y el director puede radicar una nueva.
     *
     * @param decision decisión del Consejo con la ruta de la resolución
     */
    void registrarDecisionConsejo(Decision decision) {
        Estado resultado = estadoTrasDecisionConsejo(decision);
        this.decisionesConsejo.add(decision);
        this.estado = resultado;
    }

    private boolean puedeRadicar() {
        return this.estado == null || this.estado == Estado.CON_CORRECCIONES || this.estado == Estado.NO_APROBADO;
    }

    /**
     * @return true si el Departamento ya asignó los evaluadores
     */
    public boolean tieneEvaluadores() {
        return !this.evaluadores.isEmpty();
    }

    /**
     * @param usuarioId usuario a verificar
     * @return el evaluador asignado que corresponde al usuario, vacío si no lo es
     */
    public Optional<Evaluador> evaluador(Long usuarioId) {
        if (usuarioId == null)
            return Optional.empty();
        return this.evaluadores.stream().filter(evaluador -> usuarioId.equals(evaluador.getUsuarioId()))
                .findFirst();
    }

    /**
     * @return true si la versión actual está aprobada (por los evaluadores y,
     *         si ya decidió, por el Consejo de Facultad)
     */
    public boolean estaAprobado() {
        return this.estado == Estado.APROBADO;
    }

    /**
     * @return conceptos emitidos sobre la versión actual
     */
    public List<ConceptoEvaluacion> conceptosVersionActual() {
        Optional<Documento> actual = versionActual();
        if (actual.isEmpty())
            return List.of();
        int version = actual.get().version();
        return this.conceptos.stream().filter(concepto -> concepto.version() == version).toList();
    }

    /**
     * Indica si los evaluadores superaron el plazo para emitir concepto. Es
     * informativo (alertas): el vencimiento no cambia la etapa del trabajo.
     */
    public boolean evaluacionVencida(LocalDate hoy) {
        return this.estado == Estado.EN_REVISION && hoy.isAfter(this.fechaLimiteEvaluacion);
    }

    /**
     * Indica si el director superó el plazo para reenviar el anteproyecto
     * corregido. Es informativo (alertas): el reglamento no define una
     * consecuencia.
     */
    public boolean correccionesVencidas(LocalDate hoy) {
        return this.estado == Estado.CON_CORRECCIONES && hoy.isAfter(this.fechaLimiteCorrecciones);
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

    public List<Evaluador> getEvaluadores() {
        return Collections.unmodifiableList(evaluadores);
    }

    public List<ConceptoEvaluacion> getConceptos() {
        return Collections.unmodifiableList(conceptos);
    }

    public List<Decision> getDecisionesConsejo() {
        return Collections.unmodifiableList(decisionesConsejo);
    }

    public Estado getEstado() {
        return estado;
    }

    public LocalDate getFechaLimiteEvaluacion() {
        return fechaLimiteEvaluacion;
    }

    public LocalDate getFechaLimiteCorrecciones() {
        return fechaLimiteCorrecciones;
    }
}
