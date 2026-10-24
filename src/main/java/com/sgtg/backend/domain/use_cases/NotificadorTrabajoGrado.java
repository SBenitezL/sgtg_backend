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
import com.sgtg.backend.domain.models.trabajo_grado.ConceptoEvaluacion;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;

/**
 * Genera las notificaciones del buzón a partir de los hechos del trabajo de
 * grado y de los plazos que revisa la tarea diaria. Una falla al notificar no
 * deshace la operación del trabajo, que ya quedó guardada.
 */
public class NotificadorTrabajoGrado {

    /** Días de anticipación de los avisos de la propuesta y el anteproyecto */
    public static final int DIAS_AVISO_EVALUACION = 3;
    /** Días de anticipación del aviso de fin del plazo de ejecución */
    public static final int DIAS_AVISO_EJECUCION = 30;

    private static final System.Logger LOG = System.getLogger(NotificadorTrabajoGrado.class.getName());

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
            this.notificacionGateway.guardarTodas(usuarios.stream()
                    .map(usuario -> Notificacion.nueva(usuario, evento.getTipo(), evento.getTitulo(), mensaje,
                            trabajo.getId(), trabajo.getEtapa(), null, ahora))
                    .toList());
            return null;
        });
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
     * envía una sola vez por usuario y fecha límite.
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
            case EVALUACION -> avisar(trabajo, trabajo.getAnteproyecto().getFechaLimiteEvaluacion(), hoy,
                    DIAS_AVISO_EVALUACION, EventoTrabajoGrado.PLAZO_EVALUACION_POR_VENCER,
                    EventoTrabajoGrado.PLAZO_EVALUACION_VENCIDO);
            case EN_CURSO, PRORROGA -> trabajo.getEjecucion() == null ? 0
                    : avisar(trabajo, trabajo.getEjecucion().getFechaFin(), hoy, DIAS_AVISO_EJECUCION,
                            EventoTrabajoGrado.PLAZO_TRABAJO_POR_VENCER, null);
            default -> 0;
        });
        return creados == null ? 0 : creados;
    }

    private int avisar(TrabajoGrado trabajo, LocalDate limite, LocalDate hoy, int diasAviso,
            EventoTrabajoGrado porVencer, EventoTrabajoGrado vencido) {
        if (limite == null)
            return 0;
        long dias = ChronoUnit.DAYS.between(hoy, limite);
        EventoTrabajoGrado evento = dias < 0 ? vencido : dias <= diasAviso ? porVencer : null;
        if (evento == null)
            return 0;
        String clave = evento.name() + ":" + trabajo.getId() + ":" + limite;
        LocalDateTime ahora = LocalDateTime.now(this.clock);
        String mensaje = evento.mensaje(trabajo.getTitulo(), String.valueOf(dias));
        List<Notificacion> nuevas = destinatarios(trabajo, evento).stream()
                .filter(usuario -> !this.notificacionGateway.existeClave(usuario, clave))
                .map(usuario -> Notificacion.nueva(usuario, evento.getTipo(), evento.getTitulo(), mensaje,
                        trabajo.getId(), trabajo.getEtapa(), clave, ahora))
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
