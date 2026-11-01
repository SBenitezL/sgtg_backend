package com.sgtg.backend.domain.use_cases;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.notificacion.Destinatario;
import com.sgtg.backend.domain.models.notificacion.EventoTrabajoGrado;
import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.Participante;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.trabajo_grado.CambioEtapa;
import com.sgtg.backend.domain.models.trabajo_grado.CambioObjetivos;
import com.sgtg.backend.domain.models.trabajo_grado.ConceptoEvaluacion;
import com.sgtg.backend.domain.models.trabajo_grado.Ejecucion;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.EstadoReunion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.ReunionDelPlazo;
import com.sgtg.backend.domain.models.trabajo_grado.Sustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;

/**
 * Genera las notificaciones del buzón a partir de los hechos del trabajo de
 * grado y de los plazos que revisa la tarea diaria. Una falla al notificar no
 * deshace la operación del trabajo, que ya quedó guardada.
 */
public class NotificadorTrabajoGrado {

    /**
     * Días de anticipación de los avisos de la propuesta, el anteproyecto (evaluación y correcciones) y los
     * ajustes de la sustentación
     */
    public static final int DIAS_AVISO_EVALUACION = 3;
    /** Días de anticipación del primer aviso de fin del plazo de ejecución */
    public static final int DIAS_AVISO_EJECUCION = 30;
    /** Días de anticipación del último aviso de fin del plazo de ejecución (si no se entrega, no aprobado) */
    public static final int DIAS_ULTIMO_AVISO_EJECUCION = 7;
    /** Días de anticipación del aviso del fin del plazo para cambiar el título u objetivos */
    public static final int DIAS_AVISO_CAMBIO_OBJETIVOS = 15;
    /** Días de anticipación del aviso del fin del plazo para solicitar la prórroga y para la segunda sesión */
    public static final int DIAS_AVISO_SOLICITUD = 7;
    /** Días antes de una reunión de seguimiento agendada en que se recuerda (el día anterior y el mismo día) */
    public static final int DIAS_AVISO_REUNION = 1;
    /** Días antes de la fecha de una reunión de seguimiento sin agendar en que se avisa */
    public static final int DIAS_AVISO_REUNION_SIN_AGENDAR = 3;
    /**
     * Días tras una fecha en que aún se envía su aviso (oportunidad que se abrió o que se cerró, reunión
     * vencida): pasados, el aviso ya no sirve y no se envía (ej. a los trabajos que ya existían al activarlo)
     */
    public static final int DIAS_VIGENCIA_AVISO = 7;
    private static final java.time.format.DateTimeFormatter FORMATO_REUNION = java.time.format.DateTimeFormatter
            .ofPattern("dd/MM/yyyy 'a las' HH:mm");
    private static final java.time.format.DateTimeFormatter FORMATO_FECHA = java.time.format.DateTimeFormatter
            .ofPattern("dd/MM/yyyy");

    private static final System.Logger LOG = System.getLogger(NotificadorTrabajoGrado.class.getName());
    /** Hechos de una sesión de sustentación: la notificación guarda el número de la sesión */
    private static final Set<EventoTrabajoGrado> EVENTOS_DE_SESION = Set.of(EventoTrabajoGrado.SUSTENTACION_PROGRAMADA,
            EventoTrabajoGrado.NUEVA_FECHA_SOLICITADA, EventoTrabajoGrado.NUEVA_FECHA_RESUELTA,
            EventoTrabajoGrado.SUSTENTACION_CALIFICADA, EventoTrabajoGrado.INASISTENCIA_REGISTRADA,
            EventoTrabajoGrado.AJUSTES_RADICADOS, EventoTrabajoGrado.AJUSTES_VERIFICADOS);

    private final ManageNotificacionGatewayIntPort notificacionGateway;
    private final ManageUserGatewayIntPort userGateway;
    private final ManagePerfilGatewayIntPort perfilGateway;
    private final ManageCatalogoGatewayIntPort catalogoGateway;
    private final Clock clock;

    public NotificadorTrabajoGrado(ManageNotificacionGatewayIntPort notificacionGateway,
            ManageUserGatewayIntPort userGateway, ManagePerfilGatewayIntPort perfilGateway,
            ManageCatalogoGatewayIntPort catalogoGateway, Clock clock) {
        this.notificacionGateway = notificacionGateway;
        this.userGateway = userGateway;
        this.perfilGateway = perfilGateway;
        this.catalogoGateway = catalogoGateway;
        this.clock = clock;
    }

    /**
     * Notifica el hecho a sus destinatarios, salvo a quien lo realizó.
     *
     * @param trabajo trabajo ya guardado
     * @param evento  hecho ocurrido
     * @param actorId usuario que lo realizó (null si fue el sistema)
     * @param detalle dato propio del hecho para el texto, puede ser null
     */
    public void notificar(TrabajoGrado trabajo, EventoTrabajoGrado evento, Long actorId, String detalle) {
        sinInterrumpir(() -> {
            Set<Long> usuarios = destinatarios(trabajo, evento);
            usuarios.remove(actorId);
            LocalDateTime ahora = LocalDateTime.now(this.clock);
            String mensaje = evento.mensaje(trabajo.getTitulo(), detalle);
            Integer sesion = sesionDe(trabajo, evento);
            this.notificacionGateway.guardarTodas(usuarios.stream()
                    .map(usuario -> Notificacion.nueva(usuario, evento.getTipo(), evento.getTitulo(), mensaje,
                            trabajo.getId(), trabajo.getEtapa(), evento, null, ahora, sesion))
                    .toList());
            return null;
        });
    }

    /**
     * Sesión de sustentación a la que se refiere el hecho (la actual al
     * notificar), para que el front lleve a su detalle; null si no es de una sesión.
     */
    private static Integer sesionDe(TrabajoGrado trabajo, EventoTrabajoGrado evento) {
        if (!EVENTOS_DE_SESION.contains(evento) || trabajo.getSustentacion() == null
                || trabajo.getSustentacion().getSesiones().isEmpty())
            return null;
        return trabajo.getSustentacion().getSesiones().size();
    }

    /**
     * Notifica el cambio de etapa que aplicó la tarea diaria (cierre por plazo,
     * reanudación automática...).
     */
    public void notificarCambioDelSistema(TrabajoGrado trabajo) {
        List<CambioEtapa> historial = trabajo.getHistorial();
        CambioEtapa ultimo = historial.isEmpty() ? null : historial.get(historial.size() - 1);
        if (trabajo.getEtapa() == Proceso.NO_APROBADO)
            notificar(trabajo, EventoTrabajoGrado.TRABAJO_CERRADO_POR_PLAZO, null,
                    ultimo == null ? "vencimiento de plazos" : ultimo.observacion());
        else
            notificar(trabajo, EventoTrabajoGrado.ETAPA_ACTUALIZADA, null, nombreEtapa(trabajo.getEtapa()));
    }

    /**
     * Crea los avisos de plazos por vencer o vencidos del trabajo. Cada aviso se
     * envía una sola vez por usuario y fecha límite, y cada plazo tiene pocos
     * avisos (uno antes de vencer y, si sirve, uno al vencer), para no saturar el buzón.
     *
     * @param trabajo trabajo a revisar
     * @param hoy     fecha de la revisión
     * @return cantidad de avisos creados
     */
    public int avisarPlazos(TrabajoGrado trabajo, LocalDate hoy) {
        Integer creados = sinInterrumpir(() -> switch (trabajo.getEtapa()) {
            case PROPUESTA -> trabajo.getPropuesta().getEstado() == Estado.EN_REVISION
                    ? avisar(trabajo, trabajo.getPropuesta().getFechaLimiteConcepto(), hoy, DIAS_AVISO_EVALUACION,
                            EventoTrabajoGrado.PLAZO_PROPUESTA_POR_VENCER, EventoTrabajoGrado.PLAZO_PROPUESTA_VENCIDO)
                    : 0;
            // Las correcciones que pidieron los evaluadores devuelven el trabajo a anteproyecto
            case ANTEPROYECTO -> trabajo.getAnteproyecto() != null
                    && trabajo.getAnteproyecto().getEstado() == Estado.CON_CORRECCIONES
                            ? avisar(trabajo, trabajo.getAnteproyecto().getFechaLimiteCorrecciones(), hoy,
                                    DIAS_AVISO_EVALUACION, EventoTrabajoGrado.PLAZO_CORRECCIONES_POR_VENCER,
                                    EventoTrabajoGrado.PLAZO_CORRECCIONES_VENCIDO)
                            : 0;
            case EVALUACION -> avisar(trabajo, trabajo.getAnteproyecto().getFechaLimiteEvaluacion(), hoy,
                    DIAS_AVISO_EVALUACION, EventoTrabajoGrado.PLAZO_EVALUACION_POR_VENCER,
                    EventoTrabajoGrado.PLAZO_EVALUACION_VENCIDO);
            case EN_CURSO, PRORROGA -> trabajo.getEjecucion() == null ? 0
                    : avisarEjecucion(trabajo, hoy) + avisarReuniones(trabajo, hoy);
            case SUSTENTACION, APLAZADO -> avisarSustentacion(trabajo, hoy);
            default -> 0;
        });
        return creados == null ? 0 : creados;
    }

    /** Aviso por vencer (con los días que faltan) y vencido de un plazo que se debe cumplir. */
    private int avisar(TrabajoGrado trabajo, LocalDate limite, LocalDate hoy, int diasAviso,
            EventoTrabajoGrado porVencer, EventoTrabajoGrado vencido) {
        if (limite == null)
            return 0;
        long dias = ChronoUnit.DAYS.between(hoy, limite);
        EventoTrabajoGrado evento = dias < 0 ? vencido : dias <= diasAviso ? porVencer : null;
        if (evento == null)
            return 0;
        return crearAvisos(trabajo, evento, clave(evento, trabajo, limite), String.valueOf(dias));
    }

    /**
     * Aviso por vencer y vencido de una fecha que abre o cierra una oportunidad,
     * con la fecha como detalle. El vencido solo se envía en los primeros días
     * tras la fecha ({@link #DIAS_VIGENCIA_AVISO}).
     */
    private int avisarFecha(TrabajoGrado trabajo, LocalDate limite, LocalDate hoy, int diasAviso,
            EventoTrabajoGrado porVencer, EventoTrabajoGrado vencido) {
        if (limite == null)
            return 0;
        long dias = ChronoUnit.DAYS.between(hoy, limite);
        EventoTrabajoGrado evento = dias < 0 ? (-dias <= DIAS_VIGENCIA_AVISO ? vencido : null)
                : dias <= diasAviso ? porVencer : null;
        if (evento == null)
            return 0;
        return crearAvisos(trabajo, evento, clave(evento, trabajo, limite), limite.format(FORMATO_FECHA));
    }

    private static String clave(EventoTrabajoGrado evento, TrabajoGrado trabajo, Object referencia) {
        return evento.name() + ":" + trabajo.getId() + ":" + referencia;
    }

    /**
     * Avisos del plazo de ejecución:
     * <ul>
     * <li>fin del plazo: a los 30 días y el último a los 7, que advierte que sin
     * la solicitud de sustentación el trabajo queda no aprobado (el cierre
     * también se notifica);</li>
     * <li>desde cuándo se puede solicitar la sustentación;</li>
     * <li>fin del plazo para cambiar el título u objetivos, mientras no se haya
     * aprobado un cambio ni haya uno pendiente;</li>
     * <li>fin del plazo para solicitar la prórroga, mientras no se haya solicitado.</li>
     * </ul>
     */
    private int avisarEjecucion(TrabajoGrado trabajo, LocalDate hoy) {
        Ejecucion ejecucion = trabajo.getEjecucion();
        LocalDate fin = ejecucion.getFechaFin();
        int creados = fin != null && ChronoUnit.DAYS.between(hoy, fin) <= DIAS_ULTIMO_AVISO_EJECUCION
                ? avisarFecha(trabajo, fin, hoy, DIAS_ULTIMO_AVISO_EJECUCION,
                        EventoTrabajoGrado.PLAZO_TRABAJO_ULTIMO_AVISO, null)
                : avisar(trabajo, fin, hoy, DIAS_AVISO_EJECUCION, EventoTrabajoGrado.PLAZO_TRABAJO_POR_VENCER, null);
        LocalDate minima = ejecucion.getFechaMinimaSustentacion();
        if (minima != null && !hoy.isBefore(minima)
                && ChronoUnit.DAYS.between(minima, hoy) <= DIAS_VIGENCIA_AVISO && !ejecucion.vencida(hoy))
            creados += crearAvisos(trabajo, EventoTrabajoGrado.SUSTENTACION_HABILITADA,
                    clave(EventoTrabajoGrado.SUSTENTACION_HABILITADA, trabajo, minima), minima.format(FORMATO_FECHA));
        if (ejecucion.cambioPendiente().isEmpty()
                && ejecucion.getCambiosObjetivos().stream().noneMatch(CambioObjetivos::fueAprobado))
            creados += avisarFecha(trabajo, ejecucion.fechaLimiteCambioObjetivos(), hoy, DIAS_AVISO_CAMBIO_OBJETIVOS,
                    EventoTrabajoGrado.PLAZO_CAMBIO_OBJETIVOS_POR_VENCER,
                    EventoTrabajoGrado.PLAZO_CAMBIO_OBJETIVOS_VENCIDO);
        if (ejecucion.getProrroga() == null)
            creados += avisarFecha(trabajo, ejecucion.fechaLimiteSolicitudProrroga(), hoy, DIAS_AVISO_SOLICITUD,
                    EventoTrabajoGrado.PLAZO_PRORROGA_POR_VENCER, EventoTrabajoGrado.PLAZO_PRORROGA_VENCIDO);
        return creados;
    }

    /**
     * Avisos de la sustentación: los ajustes por radicar (si vencen, la sesión
     * queda aplazada) y la segunda sesión por programar (si vence, el trabajo
     * queda no aprobado). Los vencimientos los notifica el cierre del sistema.
     */
    private int avisarSustentacion(TrabajoGrado trabajo, LocalDate hoy) {
        Sustentacion sustentacion = trabajo.getSustentacion();
        if (sustentacion == null)
            return 0;
        return switch (sustentacion.getEstado()) {
            case EN_AJUSTES -> avisar(trabajo, sustentacion.getFechaLimiteAjustes(), hoy, DIAS_AVISO_EVALUACION,
                    EventoTrabajoGrado.PLAZO_AJUSTES_POR_VENCER, null);
            case APLAZADA -> avisarFecha(trabajo, sustentacion.getFechaLimiteSegundaSesion(), hoy,
                    DIAS_AVISO_SOLICITUD, EventoTrabajoGrado.PLAZO_SEGUNDA_SESION_POR_VENCER, null);
            default -> 0;
        };
    }

    /**
     * Avisos de las reuniones de seguimiento:
     * <ul>
     * <li>la reunión agendada se recuerda el día anterior y el mismo día;</li>
     * <li>la que aún no se agenda, 3 días antes de su fecha;</li>
     * <li>de la última reunión cuya fecha pasó, un solo aviso: vencida si no se
     * registró como realizada (en sus primeros días) o, si se realizó, el
     * informe mensual pendiente.</li>
     * </ul>
     * Solo se avisa la última que pasó, para no acumular avisos de meses anteriores.
     */
    private int avisarReuniones(TrabajoGrado trabajo, LocalDate hoy) {
        List<ReunionDelPlazo> reuniones = trabajo.getEjecucion().reuniones(hoy);
        int creados = 0;
        for (ReunionDelPlazo reunion : reuniones) {
            if (reunion.estado() == EstadoReunion.AGENDADA
                    && ChronoUnit.DAYS.between(hoy, reunion.fecha()) <= DIAS_AVISO_REUNION) {
                String detalle = reunion.numero() + ", el " + reunion.agendada().fechaHora().format(FORMATO_REUNION)
                        + " (" + reunion.agendada().lugar() + ")";
                creados += crearAvisos(trabajo, EventoTrabajoGrado.REUNION_PROXIMA, clave(
                        EventoTrabajoGrado.REUNION_PROXIMA, trabajo,
                        reunion.numero() + ":" + reunion.agendada().fechaHora()), detalle);
            } else if (reunion.estado() == EstadoReunion.PENDIENTE
                    && ChronoUnit.DAYS.between(hoy, reunion.fecha()) <= DIAS_AVISO_REUNION_SIN_AGENDAR) {
                creados += crearAvisos(trabajo, EventoTrabajoGrado.REUNION_POR_VENCER,
                        clave(EventoTrabajoGrado.REUNION_POR_VENCER, trabajo, reunion.numero() + ":" + reunion.fecha()),
                        reunion.numero() + ", que corresponde al " + reunion.fecha().format(FORMATO_FECHA) + ",");
            }
        }
        ReunionDelPlazo ultimaPasada = reuniones.stream()
                .filter(reunion -> reunion.fecha() != null && reunion.fecha().isBefore(hoy))
                .reduce((anterior, siguiente) -> siguiente).orElse(null);
        if (ultimaPasada == null)
            return creados;
        if (ultimaPasada.estado() == EstadoReunion.VENCIDA) {
            if (ChronoUnit.DAYS.between(ultimaPasada.fecha(), hoy) <= DIAS_VIGENCIA_AVISO)
                creados += crearAvisos(trabajo, EventoTrabajoGrado.REUNION_VENCIDA,
                        clave(EventoTrabajoGrado.REUNION_VENCIDA, trabajo, ultimaPasada.numero()),
                        "la reunión " + ultimaPasada.numero() + " no se registró como realizada"
                                + (ultimaPasada.conInforme() ? "" : " y aún no se ha radicado su informe mensual"));
        } else if (!ultimaPasada.conInforme())
            creados += crearAvisos(trabajo, EventoTrabajoGrado.REUNION_SIN_INFORME,
                    clave(EventoTrabajoGrado.REUNION_SIN_INFORME, trabajo, ultimaPasada.numero()),
                    String.valueOf(ultimaPasada.numero()));
        return creados;
    }

    /** Crea el aviso para cada destinatario que aún no lo tiene (la clave evita repetirlo cada día). */
    private int crearAvisos(TrabajoGrado trabajo, EventoTrabajoGrado evento, String clave, String detalle) {
        LocalDateTime ahora = LocalDateTime.now(this.clock);
        String mensaje = evento.mensaje(trabajo.getTitulo(), detalle);
        List<Notificacion> nuevas = destinatarios(trabajo, evento).stream()
                .filter(usuario -> !this.notificacionGateway.existeClave(usuario, clave))
                .map(usuario -> Notificacion.nueva(usuario, evento.getTipo(), evento.getTitulo(), mensaje,
                        trabajo.getId(), trabajo.getEtapa(), evento, clave, ahora))
                .toList();
        if (!nuevas.isEmpty())
            this.notificacionGateway.guardarTodas(nuevas);
        return nuevas.size();
    }

    // ----- Destinatarios -----

    private Set<Long> destinatarios(TrabajoGrado trabajo, EventoTrabajoGrado evento) {
        Set<Long> usuarios = new LinkedHashSet<>();
        List<Usuario> todos = null;
        for (Destinatario destinatario : evento.getDestinatarios()) {
            switch (destinatario) {
                case ESTUDIANTES -> trabajo.getEstudiantes().forEach(estudiante -> agregar(usuarios, estudiante));
                case DIRECCION -> {
                    agregar(usuarios, trabajo.getDirector());
                    agregar(usuarios, trabajo.getCodirector());
                    agregar(usuarios, trabajo.getAsesor());
                }
                case EVALUADORES -> trabajo.getAnteproyecto().getEvaluadores()
                        .forEach(evaluador -> agregar(usuarios, evaluador));
                case EVALUADORES_PENDIENTES -> evaluadoresPendientes(trabajo)
                        .forEach(evaluador -> agregar(usuarios, evaluador));
                case JURADOS -> {
                    if (trabajo.getSustentacion() != null && !trabajo.getSustentacion().getSesiones().isEmpty())
                        trabajo.getSustentacion().sesionActual().jurados()
                                .forEach(jurado -> agregar(usuarios, jurado));
                }
                case COMITE, CONSEJO, JEFE_DEP, DECANATURA -> {
                    if (todos == null)
                        todos = this.userGateway.listar();
                    todos.stream().filter(Usuario::isActivo)
                            .filter(usuario -> tieneRol(usuario, destinatario.name()))
                            .filter(usuario -> destinatario != Destinatario.JEFE_DEP
                                    || esDelDepartamentoDelDirector(usuario, trabajo))
                            .filter(usuario -> destinatario != Destinatario.COMITE
                                    || esDelComiteDelPrograma(usuario, trabajo))
                            .forEach(usuario -> usuarios.add(usuario.getCusuari()));
                }
            }
        }
        return usuarios;
    }

    private static void agregar(Set<Long> usuarios, Participante participante) {
        if (participante != null && participante.getUsuarioId() != null)
            usuarios.add(participante.getUsuarioId());
    }

    private static List<Evaluador> evaluadoresPendientes(TrabajoGrado trabajo) {
        List<ConceptoEvaluacion> emitidos = trabajo.getAnteproyecto().conceptosVersionActual();
        return trabajo.getAnteproyecto().getEvaluadores().stream()
                .filter(evaluador -> emitidos.stream()
                        .noneMatch(concepto -> concepto.concepto().emisor().esMismaPersona(evaluador)))
                .toList();
    }

    private static boolean tieneRol(Usuario usuario, String rol) {
        return usuario.getRole() != null && usuario.getRole().stream().anyMatch(role -> rol.equals(role.getTrole()));
    }

    private boolean esDelDepartamentoDelDirector(Usuario usuario, TrabajoGrado trabajo) {
        PerfilDocente perfil = this.perfilGateway.buscarPerfilDocente(usuario.getCusuari());
        return perfil != null && perfil.getDepartamento() != null
                && perfil.getDepartamento().esMismo(trabajo.getDirector().getDepartamento());
    }

    /** El Comité del programa de los estudiantes: docentes del departamento al que está adscrito. */
    private boolean esDelComiteDelPrograma(Usuario usuario, TrabajoGrado trabajo) {
        PerfilDocente perfil = this.perfilGateway.buscarPerfilDocente(usuario.getCusuari());
        if (perfil == null || perfil.getDepartamento() == null)
            return false;
        Programa programa = this.catalogoGateway.buscarPrograma(trabajo.getPrograma().id());
        return programa != null && programa.esDelDepartamento(perfil.getDepartamento());
    }

    /** Nombre de la etapa para el texto de las notificaciones. */
    static String nombreEtapa(Proceso etapa) {
        return switch (Objects.requireNonNull(etapa)) {
            case PROPUESTA -> "propuesta";
            case ANTEPROYECTO -> "anteproyecto";
            case EVALUACION -> "evaluación del anteproyecto";
            case APROBACION_CONSEJO -> "aprobación del Consejo";
            case EN_CURSO -> "en curso";
            case PRORROGA -> "prórroga";
            case SUSPENDIDO -> "suspendido";
            case SUSTENTACION -> "sustentación";
            case APLAZADO -> "aplazado";
            case FINALIZADO -> "finalizado";
            case NO_APROBADO -> "no aprobado";
            case CANCELADO -> "cancelado";
        };
    }

    private static <T> T sinInterrumpir(Supplier<T> accion) {
        try {
            return accion.get();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "No se pudo generar la notificación", e);
            return null;
        }
    }

}
