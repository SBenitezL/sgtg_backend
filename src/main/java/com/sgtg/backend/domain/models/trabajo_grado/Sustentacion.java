package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Etapa de sustentación del trabajo de grado (Arts. 19-25, 46-55).
 * <ol>
 * <li>El director la solicita con el documento final (formato E), el paz y
 * salvo (formato F) y, en práctica profesional, el concepto favorable del
 * asesor.</li>
 * <li>El Consejo de Facultad la programa: dos jurados, fecha, hora y
 * salón.</li>
 * <li>Cada jurado califica (o registra la inasistencia). Si no coinciden,
 * rige la calificación menos favorable.</li>
 * <li>APROBADO: los jurados cargan la versión definitiva (formato H) y el
 * trabajo finaliza. APROBADO CON OBSERVACIONES: el director radica los ajustes
 * en 10 días hábiles y los jurados los verifican; si no se radican a tiempo o
 * no cumplen, se trata como APLAZADO. APLAZADO o inasistencia: en la primera
 * sesión se programa una segunda en máximo 60 días calendario; en la segunda,
 * el trabajo queda NO APROBADO. NO APROBADO cierra el trabajo.</li>
 * </ol>
 *
 * Cada operación que modifica el estado valida todo antes de cambiar algo, y
 * solo es accesible desde el paquete para que únicamente {@link TrabajoGrado}
 * la invoque.
 */
public class Sustentacion {
    /** Máximo de sesiones de sustentación (Art. 23) */
    public static final int MAXIMO_SESIONES = 2;
    /** Días hábiles para radicar los ajustes de una sustentación aprobada con observaciones */
    public static final int DIAS_HABILES_AJUSTES = 10;
    /** Días calendario para realizar la segunda sesión después de un aplazamiento (Art. 23) */
    public static final int DIAS_SEGUNDA_SESION = 60;

    private final String rutaDocumentoFinal;
    private final String rutaPazYSalvo;
    private final String rutaConceptoAsesor;
    private final LocalDate fechaSolicitud;
    private EstadoSustentacion estado;
    private final List<SesionSustentacion> sesiones;
    private final List<CalificacionJurado> calificaciones;
    private final List<RegistroJurado> inasistencias;
    private final List<RegistroJurado> verificaciones;
    private String rutaAjustes;
    private LocalDate fechaAjustes;
    private LocalDate fechaLimiteAjustes;
    private LocalDate fechaLimiteSegundaSesion;
    private String rutaVersionDefinitiva;
    private LocalDate fechaVersionDefinitiva;
    private Evaluador juradoVersionDefinitiva;
    private MotivoCierre motivoNoAprobacion;

    /** Reconstruye la sustentación con su estado persistido. */
    Sustentacion(DatosSustentacion datos) {
        this.rutaDocumentoFinal = datos.rutaDocumentoFinal();
        this.rutaPazYSalvo = datos.rutaPazYSalvo();
        this.rutaConceptoAsesor = datos.rutaConceptoAsesor();
        this.fechaSolicitud = datos.fechaSolicitud();
        this.estado = datos.estado();
        this.sesiones = new ArrayList<>(datos.sesiones());
        this.calificaciones = new ArrayList<>(datos.calificaciones());
        this.inasistencias = new ArrayList<>(datos.inasistencias());
        this.verificaciones = new ArrayList<>(datos.verificaciones());
        this.rutaAjustes = datos.rutaAjustes();
        this.fechaAjustes = datos.fechaAjustes();
        this.fechaLimiteAjustes = datos.fechaLimiteAjustes();
        this.fechaLimiteSegundaSesion = datos.fechaLimiteSegundaSesion();
        this.rutaVersionDefinitiva = datos.rutaVersionDefinitiva();
        this.fechaVersionDefinitiva = datos.fechaVersionDefinitiva();
        this.juradoVersionDefinitiva = datos.juradoVersionDefinitiva();
        this.motivoNoAprobacion = datos.motivoNoAprobacion();
    }

    /**
     * Crea la solicitud de sustentación.
     *
     * @param requiereConceptoAsesor true en práctica profesional
     */
    static Sustentacion solicitar(String rutaDocumentoFinal, String rutaPazYSalvo, String rutaConceptoAsesor,
            LocalDate fecha, boolean requiereConceptoAsesor) {
        if (CheckString.isBlank(rutaDocumentoFinal))
            throw new ReglaNegocioException("error.dominio.sustentacion.documento_final_obligatorio");
        if (CheckString.isBlank(rutaPazYSalvo))
            throw new ReglaNegocioException("error.dominio.sustentacion.paz_y_salvo_obligatorio");
        if (requiereConceptoAsesor && CheckString.isBlank(rutaConceptoAsesor))
            throw new ReglaNegocioException("error.dominio.sustentacion.concepto_asesor_obligatorio");
        if (fecha == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.solicitud_incompleta");
        return new Sustentacion(new DatosSustentacion(rutaDocumentoFinal.trim(), rutaPazYSalvo.trim(),
                CheckString.isBlank(rutaConceptoAsesor) ? null : rutaConceptoAsesor.trim(), fecha,
                EstadoSustentacion.SOLICITADA, List.of(), List.of(), List.of(), List.of(), null, null, null, null,
                null, null, null, null));
    }

    // ----- Programación -----

    /**
     * Programa la sesión siguiente: la primera tras la solicitud o la segunda
     * tras un aplazamiento, dentro de los 60 días calendario.
     *
     * @param sesion sesión con jurados, fecha, hora y salón
     */
    void programar(SesionSustentacion sesion) {
        if (sesion == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.programacion_incompleta");
        if (this.estado != EstadoSustentacion.SOLICITADA && this.estado != EstadoSustentacion.APLAZADA)
            throw new ReglaNegocioException("error.dominio.sustentacion.programacion_no_permitida");
        if (sesion.numero() != this.sesiones.size() + 1)
            throw new ReglaNegocioException("error.dominio.sustentacion.programacion_no_permitida");
        if (this.estado == EstadoSustentacion.APLAZADA && sesion.fecha().isAfter(this.fechaLimiteSegundaSesion))
            throw new ReglaNegocioException("error.dominio.sustentacion.segunda_sesion_fuera_de_plazo");
        this.sesiones.add(sesion);
        this.estado = EstadoSustentacion.PROGRAMADA;
    }

    // ----- Calificación e inasistencia -----

    /**
     * Registra la calificación de un jurado. Con las dos, define el resultado
     * de la sesión con la calificación menos favorable.
     *
     * @param calificacion calificación del jurado
     * @param calendario   calendario para el plazo de los ajustes
     */
    void calificar(CalificacionJurado calificacion, CalendarioHabil calendario) {
        if (calificacion == null || calendario == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.calificacion_incompleta");
        validarRegistroEnSesion(EstadoSustentacion.PROGRAMADA, calificacion.sesion(), calificacion.jurado(),
                calificacion.fecha(), sesionActual().fecha());
        List<CalificacionJurado> previas = calificacionesSesionActual();
        if (previas.stream().anyMatch(c -> c.jurado().esMismaPersona(calificacion.jurado())))
            throw new ReglaNegocioException("error.dominio.sustentacion.calificacion_repetida");
        this.calificaciones.add(calificacion);
        if (previas.isEmpty())
            return;
        Calificacion resultado = calificacion.calificacion().menosFavorable(previas.get(0).calificacion());
        switch (resultado) {
            case APROBADO -> this.estado = EstadoSustentacion.APROBADA;
            case APROBADO_CON_OBSERVACIONES -> {
                this.estado = EstadoSustentacion.EN_AJUSTES;
                this.fechaLimiteAjustes = calendario.sumarDiasHabiles(calificacion.fecha(), DIAS_HABILES_AJUSTES);
            }
            case APLAZADO -> aplazar(calificacion.fecha(), MotivoCierre.SUSTENTACION_NO_APROBADA);
            case NO_APROBADO -> noAprobar(MotivoCierre.SUSTENTACION_NO_APROBADA);
        }
    }

    /**
     * Registra la inasistencia a la sesión: en la primera se aplaza; en la
     * segunda el trabajo queda NO APROBADO. No se permite si un jurado ya
     * calificó la sesión.
     */
    void registrarInasistencia(RegistroJurado inasistencia) {
        if (inasistencia == null || inasistencia.favorable())
            throw new ReglaNegocioException("error.dominio.sustentacion.registro_incompleto");
        validarRegistroEnSesion(EstadoSustentacion.PROGRAMADA, inasistencia.sesion(), inasistencia.jurado(),
                inasistencia.fecha(), sesionActual().fecha());
        if (!calificacionesSesionActual().isEmpty())
            throw new ReglaNegocioException("error.dominio.sustentacion.sesion_ya_calificada");
        this.inasistencias.add(inasistencia);
        aplazar(inasistencia.fecha(), MotivoCierre.INASISTENCIA_SUSTENTACION);
    }

    // ----- Ajustes -----

    /**
     * Registra los ajustes del director a una sustentación aprobada con
     * observaciones, dentro de los 10 días hábiles.
     */
    void radicarAjustes(String ruta, LocalDate fecha) {
        if (this.estado != EstadoSustentacion.EN_AJUSTES)
            throw new ReglaNegocioException("error.dominio.sustentacion.ajustes_no_permitidos");
        if (CheckString.isBlank(ruta))
            throw new ReglaNegocioException("error.dominio.sustentacion.documento_ajustes_obligatorio");
        if (fecha == null || fecha.isAfter(this.fechaLimiteAjustes))
            throw new ReglaNegocioException("error.dominio.sustentacion.ajustes_fuera_de_plazo");
        this.rutaAjustes = ruta.trim();
        this.fechaAjustes = fecha;
        this.estado = EstadoSustentacion.AJUSTES_RADICADOS;
    }

    /**
     * Registra la verificación de un jurado sobre los ajustes. Con las dos: si
     * ambas cumplen, la sustentación queda aprobada; si alguna no, se trata
     * como APLAZADO.
     */
    void verificarAjustes(RegistroJurado verificacion) {
        if (verificacion == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.registro_incompleto");
        validarRegistroEnSesion(EstadoSustentacion.AJUSTES_RADICADOS, verificacion.sesion(), verificacion.jurado(),
                verificacion.fecha(), this.fechaAjustes);
        List<RegistroJurado> previas = verificacionesSesionActual();
        if (previas.stream().anyMatch(v -> v.jurado().esMismaPersona(verificacion.jurado())))
            throw new ReglaNegocioException("error.dominio.sustentacion.verificacion_repetida");
        this.verificaciones.add(verificacion);
        if (previas.isEmpty())
            return;
        if (verificacion.favorable() && previas.get(0).favorable())
            this.estado = EstadoSustentacion.APROBADA;
        else
            aplazar(verificacion.fecha(), MotivoCierre.SUSTENTACION_NO_APROBADA);
    }

    // ----- Versión definitiva -----

    /**
     * Registra la versión definitiva (formato H) que carga un jurado de la
     * sustentación aprobada: el trabajo finaliza.
     */
    void cargarVersionDefinitiva(Evaluador jurado, String ruta, LocalDate fecha) {
        if (this.estado != EstadoSustentacion.APROBADA)
            throw new ReglaNegocioException("error.dominio.sustentacion.version_definitiva_no_permitida");
        if (CheckString.isBlank(ruta))
            throw new ReglaNegocioException("error.dominio.sustentacion.documento_version_definitiva_obligatorio");
        if (jurado == null || !sesionActual().esJurado(jurado.getUsuarioId()))
            throw new ReglaNegocioException("error.dominio.sustentacion.jurado_no_asignado");
        if (fecha == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.registro_incompleto");
        this.rutaVersionDefinitiva = ruta.trim();
        this.fechaVersionDefinitiva = fecha;
        this.juradoVersionDefinitiva = jurado;
        this.estado = EstadoSustentacion.FINALIZADA;
    }

    // ----- Plazos -----

    /**
     * Aplica los plazos: si vencieron los 10 días hábiles sin ajustes, se trata
     * como APLAZADO; si vencieron los 60 días del aplazamiento sin programar
     * la segunda sesión, el trabajo queda NO APROBADO por vencimiento.
     *
     * @return true si cambió el estado
     */
    boolean verificarPlazos(LocalDate hoy) {
        if (this.estado == EstadoSustentacion.EN_AJUSTES && hoy.isAfter(this.fechaLimiteAjustes)) {
            aplazar(this.fechaLimiteAjustes.plusDays(1), MotivoCierre.SUSTENTACION_NO_APROBADA);
            return true;
        }
        if (this.estado == EstadoSustentacion.APLAZADA && hoy.isAfter(this.fechaLimiteSegundaSesion)) {
            noAprobar(MotivoCierre.VENCIMIENTO_PLAZO);
            return true;
        }
        return false;
    }

    // ----- Apoyo -----

    /**
     * En la primera sesión, aplaza y fija el plazo de la segunda; en la
     * segunda, cierra con el motivo indicado.
     */
    private void aplazar(LocalDate fecha, MotivoCierre motivoSiSegunda) {
        if (sesionActual().numero() < MAXIMO_SESIONES) {
            this.estado = EstadoSustentacion.APLAZADA;
            this.fechaLimiteSegundaSesion = fecha.plusDays(DIAS_SEGUNDA_SESION);
        } else {
            noAprobar(motivoSiSegunda);
        }
    }

    private void noAprobar(MotivoCierre motivo) {
        this.estado = EstadoSustentacion.NO_APROBADA;
        this.motivoNoAprobacion = motivo;
    }

    private void validarRegistroEnSesion(EstadoSustentacion esperado, int sesion, Evaluador jurado, LocalDate fecha,
            LocalDate fechaMinima) {
        if (this.estado != esperado)
            throw new ReglaNegocioException("error.dominio.sustentacion.registro_no_permitido");
        SesionSustentacion actual = sesionActual();
        if (sesion != actual.numero() || !actual.esJurado(jurado.getUsuarioId()))
            throw new ReglaNegocioException("error.dominio.sustentacion.jurado_no_asignado");
        if (fecha.isBefore(fechaMinima))
            throw new ReglaNegocioException("error.dominio.sustentacion.fecha_registro_invalida");
    }

    /**
     * @return la última sesión programada
     */
    public SesionSustentacion sesionActual() {
        if (this.sesiones.isEmpty())
            throw new ReglaNegocioException("error.dominio.sustentacion.registro_no_permitido");
        return this.sesiones.get(this.sesiones.size() - 1);
    }

    /**
     * @return el jurado de la sesión actual que corresponde al usuario, vacío si no lo es
     */
    public Optional<Evaluador> jurado(Long usuarioId) {
        if (this.sesiones.isEmpty() || usuarioId == null)
            return Optional.empty();
        return sesionActual().jurados().stream().filter(j -> usuarioId.equals(j.getUsuarioId())).findFirst();
    }

    /**
     * @return true si el usuario es o fue jurado de alguna sesión
     */
    public boolean esJurado(Long usuarioId) {
        return this.sesiones.stream().anyMatch(s -> s.esJurado(usuarioId));
    }

    public List<CalificacionJurado> calificacionesSesionActual() {
        if (this.sesiones.isEmpty())
            return List.of();
        int numero = sesionActual().numero();
        return this.calificaciones.stream().filter(c -> c.sesion() == numero).toList();
    }

    private List<RegistroJurado> verificacionesSesionActual() {
        int numero = sesionActual().numero();
        return this.verificaciones.stream().filter(v -> v.sesion() == numero).toList();
    }

    /**
     * @return estado persistible de la sustentación
     */
    public DatosSustentacion datos() {
        return new DatosSustentacion(rutaDocumentoFinal, rutaPazYSalvo, rutaConceptoAsesor, fechaSolicitud, estado,
                sesiones, calificaciones, inasistencias, verificaciones, rutaAjustes, fechaAjustes,
                fechaLimiteAjustes, fechaLimiteSegundaSesion, rutaVersionDefinitiva, fechaVersionDefinitiva,
                juradoVersionDefinitiva, motivoNoAprobacion);
    }

    public String getRutaDocumentoFinal() {
        return rutaDocumentoFinal;
    }

    public String getRutaPazYSalvo() {
        return rutaPazYSalvo;
    }

    public String getRutaConceptoAsesor() {
        return rutaConceptoAsesor;
    }

    public LocalDate getFechaSolicitud() {
        return fechaSolicitud;
    }

    public EstadoSustentacion getEstado() {
        return estado;
    }

    public List<SesionSustentacion> getSesiones() {
        return Collections.unmodifiableList(sesiones);
    }

    public List<CalificacionJurado> getCalificaciones() {
        return Collections.unmodifiableList(calificaciones);
    }

    public List<RegistroJurado> getInasistencias() {
        return Collections.unmodifiableList(inasistencias);
    }

    public List<RegistroJurado> getVerificaciones() {
        return Collections.unmodifiableList(verificaciones);
    }

    public String getRutaAjustes() {
        return rutaAjustes;
    }

    public LocalDate getFechaAjustes() {
        return fechaAjustes;
    }

    public LocalDate getFechaLimiteAjustes() {
        return fechaLimiteAjustes;
    }

    public LocalDate getFechaLimiteSegundaSesion() {
        return fechaLimiteSegundaSesion;
    }

    public String getRutaVersionDefinitiva() {
        return rutaVersionDefinitiva;
    }

    public LocalDate getFechaVersionDefinitiva() {
        return fechaVersionDefinitiva;
    }

    public Evaluador getJuradoVersionDefinitiva() {
        return juradoVersionDefinitiva;
    }

    public MotivoCierre getMotivoNoAprobacion() {
        return motivoNoAprobacion;
    }
}
