package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Periodo;

/**
 * Tiempo de desarrollo del trabajo de grado (Arts. 14-15). Empieza en la fecha
 * de notificación de la resolución del Consejo de Facultad que aprueba el
 * anteproyecto y dura los meses máximos de la modalidad: 9 en investigación y
 * hasta 12 en práctica profesional. La sustentación solo puede solicitarse
 * después de los meses mínimos: 3 en investigación y 6 en práctica
 * profesional.
 *
 * Durante la ejecución el título u objetivos pueden cambiarse una sola vez,
 * dentro de los primeros 6 meses y con aprobación del Consejo (Arts. 17-18), y
 * los estudiantes radican los informes mensuales (Art. 45), que revisan el
 * director, el codirector o el asesor.
 * El plazo puede extenderse una sola vez con una prórroga de 1 a 3 meses
 * (Art. 16) y suspenderse una sola vez hasta por un período académico (Art.
 * 27): al reanudar, el fin es la fecha de reanudación más los días que
 * faltaban al suspender.
 *
 * Sus operaciones que modifican el estado solo son accesibles desde el
 * paquete, para que únicamente {@link TrabajoGrado} las invoque.
 */
public class Ejecucion {
    /** Meses desde la notificación en los que se puede solicitar el cambio de título u objetivos */
    public static final int MESES_CAMBIO_OBJETIVOS = 6;
    /** Días calendario mínimos antes del fin del plazo para solicitar la prórroga (Art. 16) */
    public static final int DIAS_ANTICIPACION_PRORROGA = 15;

    /** Desde la fecha de notificación hasta el fin del plazo (extendido por la prórroga o la reanudación) */
    private Periodo periodo;
    /** Fecha desde la que se puede solicitar la sustentación */
    private final LocalDate fechaMinimaSustentacion;
    /** Solicitudes de cambio de título u objetivos, en orden */
    private final List<CambioObjetivos> cambiosObjetivos;
    /** Informes mensuales radicados, con sus revisiones */
    private final List<InformeMensual> informesMensuales;
    /** Prórroga solicitada, null si no se ha solicitado */
    private Prorroga prorroga;
    /** Solicitudes de suspensión, en orden */
    private final List<Suspension> suspensiones;

    private Ejecucion(Periodo periodo, LocalDate fechaMinimaSustentacion, List<CambioObjetivos> cambiosObjetivos,
            List<InformeMensual> informesMensuales, Prorroga prorroga, List<Suspension> suspensiones) {
        this.periodo = periodo;
        this.fechaMinimaSustentacion = fechaMinimaSustentacion;
        this.cambiosObjetivos = new ArrayList<>(cambiosObjetivos);
        this.informesMensuales = new ArrayList<>(informesMensuales);
        this.prorroga = prorroga;
        this.suspensiones = new ArrayList<>(suspensiones);
    }

    /** Reconstruye la ejecución con su estado persistido. */
    Ejecucion(DatosEjecucion datos) {
        this(new Periodo(datos.fechaNotificacion(), datos.fechaFin()), datos.fechaMinimaSustentacion(),
                datos.cambiosObjetivos(), datos.informesMensuales(), datos.prorroga(), datos.suspensiones());
    }

    /**
     * Inicia la ejecución con los plazos de la modalidad.
     *
     * @param fechaNotificacion fecha de notificación de la aprobación del Consejo
     * @param modalidad         modalidad del trabajo de grado
     */
    static Ejecucion iniciar(LocalDate fechaNotificacion, Modalidad modalidad) {
        if (fechaNotificacion == null || modalidad == null)
            throw new ReglaNegocioException("error.dominio.ejecucion.datos_incompletos");
        return new Ejecucion(Periodo.desde(fechaNotificacion, modalidad.getMesesMaximosEjecucion()),
                fechaNotificacion.plusMonths(modalidad.getMesesMinimosEjecucion()), List.of(), List.of(), null,
                List.of());
    }

    // ----- Cambio de título u objetivos -----

    /**
     * Registra una solicitud de cambio de título u objetivos. Solo se permite
     * dentro de los primeros 6 meses desde la notificación, si no hay otra
     * pendiente y si no se ha aprobado un cambio antes. Una solicitud no
     * aprobada no consume la oportunidad.
     *
     * @param solicitud solicitud pendiente de decisión
     */
    void solicitarCambioObjetivos(CambioObjetivos solicitud) {
        if (solicitud == null || !solicitud.estaPendiente())
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.datos_incompletos");
        if (cambioPendiente().isPresent())
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.solicitud_pendiente");
        if (this.cambiosObjetivos.stream().anyMatch(CambioObjetivos::fueAprobado))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.ya_realizado");
        LocalDate fecha = solicitud.fechaSolicitud();
        if (fecha.isBefore(getFechaNotificacion()) || fecha.isAfter(fechaLimiteCambioObjetivos()))
            throw new ReglaNegocioException("error.dominio.cambio_objetivos.fuera_de_plazo");
        this.cambiosObjetivos.add(solicitud);
    }

    /**
     * Registra la decisión del Consejo sobre la solicitud pendiente. La
     * decisión se valida al crear la solicitud resuelta, antes de modificar la
     * ejecución.
     *
     * @return la solicitud resuelta
     */
    CambioObjetivos resolverCambioObjetivos(Decision decision) {
        CambioObjetivos resuelta = cambioPendiente()
                .orElseThrow(() -> new ReglaNegocioException("error.dominio.cambio_objetivos.sin_solicitud_pendiente"))
                .resolver(decision);
        this.cambiosObjetivos.set(this.cambiosObjetivos.size() - 1, resuelta);
        return resuelta;
    }

    /**
     * @return la solicitud de cambio pendiente de decisión, si existe
     */
    public Optional<CambioObjetivos> cambioPendiente() {
        if (this.cambiosObjetivos.isEmpty())
            return Optional.empty();
        CambioObjetivos ultima = this.cambiosObjetivos.get(this.cambiosObjetivos.size() - 1);
        return ultima.estaPendiente() ? Optional.of(ultima) : Optional.empty();
    }

    /**
     * @return último día en que se puede solicitar el cambio de título u objetivos
     */
    public LocalDate fechaLimiteCambioObjetivos() {
        return getFechaNotificacion().plusMonths(MESES_CAMBIO_OBJETIVOS);
    }

    // ----- Informes mensuales -----

    /**
     * Registra un informe mensual con el siguiente consecutivo.
     *
     * @param rutaArchivo ruta del documento del informe
     * @param hoy         fecha de radicación
     * @return el informe registrado
     */
    InformeMensual registrarInformeMensual(String rutaArchivo, LocalDate hoy) {
        return registrarInformeMensual(rutaArchivo, hoy, null, null);
    }

    /**
     * @param radicadoPor   código de quien radica
     * @param observaciones observaciones opcionales de quien radica
     */
    InformeMensual registrarInformeMensual(String rutaArchivo, LocalDate hoy, String radicadoPor,
            String observaciones) {
        if (hoy == null)
            throw new ReglaNegocioException("error.dominio.informe_mensual.datos_incompletos");
        if (hoy.isBefore(getFechaNotificacion()))
            throw new ReglaNegocioException("error.dominio.informe_mensual.fecha_invalida");
        InformeMensual informe = new InformeMensual(this.informesMensuales.size() + 1, rutaArchivo, hoy, radicadoPor,
                observaciones, List.of());
        this.informesMensuales.add(informe);
        return informe;
    }

    /**
     * Registra la revisión de un informe mensual: una por revisor y no anterior
     * a la radicación del informe. No cambia el estado del trabajo.
     *
     * @param numero   consecutivo del informe revisado
     * @param revision revisión del director, codirector o asesor
     * @return el informe con la revisión
     */
    InformeMensual revisarInformeMensual(int numero, RevisionInforme revision) {
        if (revision == null)
            throw new ReglaNegocioException("error.dominio.informe_mensual.revision_incompleta");
        if (numero < 1 || numero > this.informesMensuales.size())
            throw new ReglaNegocioException("error.dominio.informe_mensual.no_encontrado");
        InformeMensual informe = this.informesMensuales.get(numero - 1);
        if (informe.revisadoPor(revision.revisorUsuarioId()))
            throw new ReglaNegocioException("error.dominio.informe_mensual.revision_repetida");
        if (revision.fecha().isBefore(informe.fecha()))
            throw new ReglaNegocioException("error.dominio.informe_mensual.fecha_revision_invalida");
        InformeMensual revisado = informe.conRevision(revision);
        this.informesMensuales.set(numero - 1, revisado);
        return revisado;
    }

    // ----- Prórroga -----

    /**
     * Registra la solicitud de prórroga. Solo una vez (una negada también
     * cuenta) y con al menos 15 días calendario antes del fin del plazo.
     *
     * @param solicitud prórroga pendiente de decisión
     */
    void solicitarProrroga(Prorroga solicitud) {
        if (solicitud == null || !solicitud.estaPendiente())
            throw new ReglaNegocioException("error.dominio.prorroga.datos_incompletos");
        if (this.prorroga != null)
            throw new ReglaNegocioException("error.dominio.prorroga.ya_solicitada");
        LocalDate fecha = solicitud.fechaSolicitud();
        if (fecha.isBefore(getFechaNotificacion()) || fecha.isAfter(fechaLimiteSolicitudProrroga()))
            throw new ReglaNegocioException("error.dominio.prorroga.fuera_de_plazo");
        this.prorroga = solicitud;
    }

    /**
     * Valida la decisión del Consejo sobre la prórroga pendiente, sin modificar
     * la ejecución.
     *
     * @return la prórroga resuelta
     */
    Prorroga prorrogaResuelta(Decision decision) {
        if (this.prorroga == null)
            throw new ReglaNegocioException("error.dominio.prorroga.sin_solicitud_pendiente");
        return this.prorroga.resolver(decision);
    }

    /**
     * Registra la prórroga resuelta; si fue aprobada, extiende el fin del plazo
     * por los meses solicitados.
     *
     * @param resuelta resultado de {@link #prorrogaResuelta(Decision)}
     */
    void aplicarProrroga(Prorroga resuelta) {
        this.prorroga = resuelta;
        if (resuelta.fueAprobada())
            this.periodo = this.periodo.extenderMeses(resuelta.meses());
    }

    /**
     * @return true si hay una prórroga pendiente de decisión del Consejo
     */
    public boolean tieneProrrogaPendiente() {
        return this.prorroga != null && this.prorroga.estaPendiente();
    }

    /**
     * @return último día en que se puede solicitar la prórroga (15 días
     *         calendario antes del fin del plazo)
     */
    public LocalDate fechaLimiteSolicitudProrroga() {
        return getFechaFin().minusDays(DIAS_ANTICIPACION_PRORROGA);
    }

    // ----- Suspensión -----

    /**
     * Registra la solicitud de suspensión. Solo si no hay otra pendiente, si no
     * se ha suspendido antes (una negada no consume la oportunidad) y dentro
     * del plazo de ejecución.
     *
     * @param solicitud suspensión pendiente de decisión
     */
    void solicitarSuspension(Suspension solicitud) {
        if (solicitud == null || !solicitud.estaPendiente())
            throw new ReglaNegocioException("error.dominio.suspension.datos_incompletos");
        if (tieneSuspensionPendiente())
            throw new ReglaNegocioException("error.dominio.suspension.solicitud_pendiente");
        if (this.suspensiones.stream().anyMatch(Suspension::fueAprobada))
            throw new ReglaNegocioException("error.dominio.suspension.ya_realizada");
        LocalDate fecha = solicitud.fechaSolicitud();
        if (fecha.isBefore(getFechaNotificacion()) || fecha.isAfter(getFechaFin()))
            throw new ReglaNegocioException("error.dominio.suspension.fuera_de_plazo");
        this.suspensiones.add(solicitud);
    }

    /**
     * Valida la decisión del Consejo sobre la suspensión pendiente, sin
     * modificar la ejecución. Si la aprueba, calcula los días que faltan del
     * plazo en la fecha de la decisión.
     *
     * @param etapaActual etapa del trabajo al decidir, a la que volverá al reanudar
     * @return la suspensión resuelta
     */
    Suspension suspensionResuelta(Decision decision, Proceso etapaActual) {
        Suspension pendiente = suspensionPendiente()
                .orElseThrow(() -> new ReglaNegocioException("error.dominio.suspension.sin_solicitud_pendiente"));
        long dias = decision == null ? 0 : this.periodo.diasRestantes(decision.fecha());
        return pendiente.resolver(decision, etapaActual, dias);
    }

    /**
     * Registra la suspensión resuelta por el Consejo.
     *
     * @param resuelta resultado de {@link #suspensionResuelta(Decision, Proceso)}
     */
    void aplicarSuspension(Suspension resuelta) {
        this.suspensiones.set(this.suspensiones.size() - 1, resuelta);
    }

    /**
     * Valida la reanudación de la suspensión activa, sin modificar la ejecución.
     *
     * @param fecha           fecha de reanudación
     * @param rutaReanudacion ruta del documento, null si es automática
     * @return la suspensión reanudada
     */
    Suspension suspensionReanudada(LocalDate fecha, String rutaReanudacion) {
        return suspensionActiva()
                .orElseThrow(() -> new ReglaNegocioException("error.dominio.suspension.sin_suspension_activa"))
                .reanudar(fecha, rutaReanudacion);
    }

    /**
     * Registra la reanudación: el nuevo fin es la fecha de reanudación más los
     * días que faltaban al suspender.
     *
     * @param reanudada resultado de {@link #suspensionReanudada(LocalDate, String)}
     */
    void aplicarReanudacion(Suspension reanudada) {
        this.suspensiones.set(this.suspensiones.size() - 1, reanudada);
        this.periodo = this.periodo.reanudarDesde(reanudada.fechaReanudacion(), reanudada.diasRestantes());
    }

    /**
     * @return la suspensión pendiente de decisión, si existe
     */
    public Optional<Suspension> suspensionPendiente() {
        return ultimaSuspension().filter(Suspension::estaPendiente);
    }

    /**
     * @return la suspensión aprobada que aún no se ha reanudado, si existe
     */
    public Optional<Suspension> suspensionActiva() {
        return ultimaSuspension().filter(Suspension::estaActiva);
    }

    /**
     * @return true si hay una suspensión pendiente de decisión del Consejo
     */
    public boolean tieneSuspensionPendiente() {
        return suspensionPendiente().isPresent();
    }

    private Optional<Suspension> ultimaSuspension() {
        if (this.suspensiones.isEmpty())
            return Optional.empty();
        return Optional.of(this.suspensiones.get(this.suspensiones.size() - 1));
    }

    // ----- Plazos -----

    /**
     * @return true si el plazo de ejecución terminó en la fecha dada
     */
    public boolean vencida(LocalDate hoy) {
        return this.periodo.vencido(hoy);
    }

    /**
     * @return true si ya transcurrieron los meses mínimos para solicitar la
     *         sustentación
     */
    public boolean permiteSolicitarSustentacion(LocalDate hoy) {
        return !hoy.isBefore(this.fechaMinimaSustentacion);
    }

    /**
     * @return días calendario que faltan para el fin del plazo, 0 si ya terminó
     */
    public long diasRestantes(LocalDate hoy) {
        return this.periodo.diasRestantes(hoy);
    }

    /**
     * @return estado persistible de la ejecución
     */
    public DatosEjecucion datos() {
        return new DatosEjecucion(getFechaNotificacion(), getFechaFin(), this.fechaMinimaSustentacion,
                this.cambiosObjetivos, this.informesMensuales, this.prorroga, this.suspensiones);
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public LocalDate getFechaNotificacion() {
        return periodo.inicio();
    }

    public LocalDate getFechaFin() {
        return periodo.fin();
    }

    public LocalDate getFechaMinimaSustentacion() {
        return fechaMinimaSustentacion;
    }

    public List<CambioObjetivos> getCambiosObjetivos() {
        return Collections.unmodifiableList(cambiosObjetivos);
    }

    public List<InformeMensual> getInformesMensuales() {
        return Collections.unmodifiableList(informesMensuales);
    }

    public Prorroga getProrroga() {
        return prorroga;
    }

    public List<Suspension> getSuspensiones() {
        return Collections.unmodifiableList(suspensiones);
    }
}
