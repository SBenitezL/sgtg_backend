package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.Participante;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.utils.CheckString;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Raíz del agregado del trabajo de grado. Contiene todas sus etapas y es la
 * única clase que puede cambiar la etapa en la que se encuentra, registrando
 * cada cambio en el historial. Las reglas propias de cada modalidad están en
 * {@link TrabajoInvestigacion} y {@link PracticaProfesional}.
 */
public abstract class TrabajoGrado {
    /** Identificador, null mientras no se haya persistido */
    private Long id;
    /** Título del trabajo de grado */
    private String titulo;
    /** Objetivos del trabajo de grado */
    private List<String> objetivos;
    /** Estudiantes que realizan el trabajo */
    private List<Estudiante> estudiantes;
    /** Director del trabajo */
    private Director director;
    /**
     * Codirector, siempre un docente de la universidad; obligatorio y de planta
     * del mismo departamento si el director no es de planta (Art. 6)
     */
    private Director codirector;
    /** Asesor, opcional en investigación y obligatorio en práctica profesional */
    private Asesor asesor;
    /** Etapa actual del proceso */
    private Proceso etapa;
    /** Etapa de propuesta */
    private Propuesta propuesta;
    /** Etapa de anteproyecto y evaluación */
    private Anteproyecto anteproyecto;
    /** Tiempo de desarrollo, null hasta que el Consejo apruebe el anteproyecto */
    private Ejecucion ejecucion;
    /** Etapa de sustentación, null hasta que el director la solicite */
    private Sustentacion sustentacion;
    /** Solicitudes de cancelación, en orden */
    private List<Cancelacion> cancelaciones;
    /** Historial de cambios de etapa */
    private List<CambioEtapa> historial;
    /** Motivo por el que se cerró el trabajo sin aprobarse, null si sigue activo */
    private MotivoCierre motivoCierre;

    /**
     * Crea un trabajo de grado nuevo en la etapa de propuesta.
     *
     * @param codirector obligatorio y de planta del departamento del director
     *                   cuando el director no es de planta, null si no aplica
     * @param asesor     asesor del trabajo, null si no tiene
     */
    protected TrabajoGrado(Long id, String titulo, List<String> objetivos, List<Estudiante> estudiantes,
            Director director, Director codirector, Asesor asesor) {
        validarDescripcion(titulo, objetivos);
        validarEstudiantes(estudiantes);
        validarDireccion(director, codirector, estudiantes);
        this.id = id;
        this.titulo = titulo;
        this.objetivos = new ArrayList<>(objetivos);
        this.estudiantes = new ArrayList<>(estudiantes);
        this.director = director;
        this.codirector = codirector;
        this.asesor = asesor;
        this.etapa = Proceso.PROPUESTA;
        this.propuesta = new Propuesta();
        this.anteproyecto = new Anteproyecto();
        this.historial = new ArrayList<>();
        this.cancelaciones = new ArrayList<>();
    }

    /**
     * Reconstruye el trabajo con su estado persistido, sin aplicar las reglas de
     * creación. Solo para las subclases; desde fuera se usa
     * {@link #reconstruir(DatosTrabajoGrado)}.
     */
    protected TrabajoGrado(DatosTrabajoGrado datos) {
        this.id = datos.id();
        this.titulo = datos.titulo();
        this.objetivos = new ArrayList<>(datos.objetivos());
        this.estudiantes = new ArrayList<>(datos.estudiantes());
        this.director = datos.director();
        this.codirector = datos.codirector();
        this.asesor = datos.asesor();
        this.etapa = datos.etapa();
        this.propuesta = new Propuesta(datos.propuesta());
        this.anteproyecto = new Anteproyecto(datos.anteproyecto());
        this.ejecucion = datos.ejecucion() == null ? null : new Ejecucion(datos.ejecucion());
        this.sustentacion = datos.sustentacion() == null ? null : new Sustentacion(datos.sustentacion());
        this.cancelaciones = new ArrayList<>(datos.cancelaciones());
        this.historial = new ArrayList<>(datos.historial());
        this.motivoCierre = datos.motivoCierre();
    }

    /**
     * Reconstruye un trabajo de grado guardado, según su modalidad. Lo usa la
     * infraestructura al leer de la base de datos; no vuelve a aplicar las
     * reglas de creación (ej. si el director pasó a planta después de la
     * asignación, no se exige de nuevo el codirector).
     *
     * @param datos estado persistido del trabajo
     * @return el trabajo de grado en su etapa actual
     */
    public static TrabajoGrado reconstruir(DatosTrabajoGrado datos) {
        return switch (datos.modalidad()) {
            case TRABAJO_DE_INVESTIGACION -> new TrabajoInvestigacion(datos);
            case PRACTICA_PROFESIONAL -> new PracticaProfesional(datos);
        };
    }

    private static void validarDescripcion(String titulo, List<String> objetivos) {
        if (CheckString.isBlank(titulo))
            throw new ReglaNegocioException("error.dominio.trabajo_grado.titulo_obligatorio");
        if (objetivos == null || objetivos.isEmpty() || objetivos.stream().anyMatch(CheckString::isBlank))
            throw new ReglaNegocioException("error.dominio.trabajo_grado.objetivos_obligatorios");
    }

    private static void validarEstudiantes(List<Estudiante> estudiantes) {
        // anyMatch en lugar de contains(null): las listas inmutables lanzan NPE en contains(null)
        if (estudiantes == null || estudiantes.isEmpty() || estudiantes.stream().anyMatch(Objects::isNull))
            throw new ReglaNegocioException("error.dominio.trabajo_grado.estudiantes_obligatorios");
        for (int i = 0; i < estudiantes.size(); i++)
            for (int j = i + 1; j < estudiantes.size(); j++)
                if (estudiantes.get(i).esMismaPersona(estudiantes.get(j)))
                    throw new ReglaNegocioException("error.dominio.trabajo_grado.estudiante_repetido");
    }

    private static void validarDireccion(Director director, Director codirector, List<Estudiante> estudiantes) {
        if (director == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.director_obligatorio");
        if (director.requiereCodirector()
                && (codirector == null || !codirector.esDePlantaDe(director.getDepartamento())))
            throw new ReglaNegocioException("error.dominio.trabajo_grado.codirector_de_planta_obligatorio");
        if (codirector != null && !codirector.getTipoVinculacion().esDocente())
            throw new ReglaNegocioException("error.dominio.trabajo_grado.codirector_debe_ser_docente");
        if (codirector != null && codirector.esMismaPersona(director))
            throw new ReglaNegocioException("error.dominio.trabajo_grado.codirector_igual_director");
        if (esAlgunEstudiante(director, estudiantes) || esAlgunEstudiante(codirector, estudiantes))
            throw new ReglaNegocioException("error.dominio.trabajo_grado.director_es_estudiante");
    }

    private static boolean esAlgunEstudiante(Participante participante, List<Estudiante> estudiantes) {
        return participante != null && estudiantes.stream().anyMatch(participante::esMismaPersona);
    }

    /**
     * @return modalidad del trabajo de grado
     */
    public abstract Modalidad getModalidad();

    // ----- Fase 1: propuesta -----

    /**
     * Radica una versión de la propuesta (formato A) ante el Comité de Programa.
     *
     * @param ruta       ruta del documento en el almacenamiento de archivos
     * @param hoy        fecha de radicación
     * @param calendario calendario para calcular el plazo del Comité
     */
    public void radicarPropuesta(String ruta, LocalDate hoy, CalendarioHabil calendario) {
        validarEtapa(Proceso.PROPUESTA);
        this.propuesta.radicar(ruta, hoy, calendario);
    }

    /**
     * Registra el concepto del Comité de Programa. Si es aprobatorio, el trabajo
     * avanza a anteproyecto; si no es aprobado permanece en propuesta a la
     * espera de una nueva versión.
     *
     * @param decision    decisión del Comité de Programa
     * @param responsable código del usuario que registra la decisión
     * @param ahora       fecha y hora del registro
     */
    public void registrarDecisionComite(Decision decision, String responsable, LocalDateTime ahora) {
        validarEtapa(Proceso.PROPUESTA);
        // Se valida el cambio de etapa antes de modificar la propuesta
        CambioEtapa cambio = decision != null && decision.esAprobatoria()
                ? new CambioEtapa(this.etapa, Proceso.ANTEPROYECTO, ahora, responsable, decision.observaciones())
                : null;
        this.propuesta.registrarDecision(decision);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    // ----- Fase 2: anteproyecto y evaluación -----

    /**
     * Radica una versión del anteproyecto ante el Departamento (Art. 13). La
     * primera versión queda a la espera de la asignación de evaluadores; una
     * versión corregida pasa de inmediato a evaluación con los mismos
     * evaluadores.
     *
     * @param ruta        ruta del documento en el almacenamiento de archivos
     * @param responsable código del usuario que radica (el director)
     * @param ahora       fecha y hora de la radicación
     * @param calendario  calendario para calcular el plazo de los evaluadores
     */
    public void radicarAnteproyecto(String ruta, String responsable, LocalDateTime ahora, CalendarioHabil calendario) {
        validarEtapa(Proceso.ANTEPROYECTO);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        // Con evaluadores asignados, la versión corregida vuelve a evaluación
        CambioEtapa cambio = this.anteproyecto.tieneEvaluadores()
                ? new CambioEtapa(this.etapa, Proceso.EVALUACION, ahora, responsable,
                        "Radicación del anteproyecto corregido")
                : null;
        this.anteproyecto.radicar(ruta, ahora.toLocalDate(), calendario);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    /**
     * Registra los dos evaluadores asignados por el Departamento (Art. 13) y
     * pasa el anteproyecto a evaluación. Los evaluadores deben ser personas
     * distintas y no pueden ser director, codirector, asesor ni estudiantes
     * del trabajo.
     *
     * @param evaluador1  primer evaluador
     * @param evaluador2  segundo evaluador
     * @param responsable código del usuario que asigna (jefe de departamento)
     * @param ahora       fecha y hora de la asignación
     * @param calendario  calendario para calcular el plazo de los evaluadores
     */
    public void asignarEvaluadores(Evaluador evaluador1, Evaluador evaluador2, String responsable,
            LocalDateTime ahora, CalendarioHabil calendario) {
        validarEtapa(Proceso.ANTEPROYECTO);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        List<Evaluador> evaluadores = Arrays.asList(evaluador1, evaluador2);
        this.anteproyecto.validarAsignacion(evaluadores, ahora.toLocalDate(), calendario);
        if (evaluadores.stream().anyMatch(this::tieneOtroPapel))
            throw new ReglaNegocioException("error.dominio.anteproyecto.evaluador_con_conflicto");
        CambioEtapa cambio = new CambioEtapa(this.etapa, Proceso.EVALUACION, ahora, responsable,
                "Asignación de evaluadores");
        this.anteproyecto.asignarEvaluadores(evaluadores, ahora.toLocalDate(), calendario);
        aplicarCambio(cambio);
    }

    /**
     * Registra el concepto de un evaluador sobre la versión actual del
     * anteproyecto. Cuando ambos han emitido concepto: si los dos aprueban, el
     * trabajo pasa a aprobación del Consejo de Facultad; si alguno pide
     * correcciones, vuelve a anteproyecto para que el director radique una
     * nueva versión.
     *
     * @param concepto   concepto del evaluador: APROBADO o CON_CORRECCIONES
     * @param rutaFormato ruta del formato B, null si no se cargó
     * @param ahora      fecha y hora del registro
     * @param calendario calendario para calcular el plazo de las correcciones
     */
    public void registrarConceptoEvaluador(Concepto concepto, String rutaFormato, LocalDateTime ahora,
            CalendarioHabil calendario) {
        validarEtapa(Proceso.EVALUACION);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        // Se valida el cambio de etapa antes de modificar el anteproyecto
        Estado resultado = this.anteproyecto.estadoTrasConcepto(concepto);
        String responsable = concepto.emisor().getCodigo();
        CambioEtapa cambio = switch (resultado) {
            case APROBADO -> new CambioEtapa(this.etapa, Proceso.APROBACION_CONSEJO, ahora, responsable,
                    "Anteproyecto aprobado por ambos evaluadores");
            case CON_CORRECCIONES -> new CambioEtapa(this.etapa, Proceso.ANTEPROYECTO, ahora, responsable,
                    "Anteproyecto con correcciones de los evaluadores");
            default -> null;
        };
        this.anteproyecto.registrarConcepto(concepto, rutaFormato, calendario);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    // ----- Fase 3: aprobación del Consejo de Facultad -----

    /**
     * Registra el concepto del Consejo de Facultad sobre el anteproyecto
     * aprobado por los evaluadores (Art. 14). La decisión adjunta la ruta de
     * la resolución. Si lo aprueba, la fecha de la decisión es la de
     * notificación: el trabajo pasa a en curso y empieza el plazo de la
     * modalidad. Si no lo aprueba, vuelve a anteproyecto para radicar una
     * nueva versión que revisan los evaluadores.
     *
     * @param decision    decisión del Consejo: APROBADO o NO_APROBADO
     * @param responsable código del usuario que registra la decisión
     * @param ahora       fecha y hora del registro
     */
    public void registrarDecisionConsejo(Decision decision, String responsable, LocalDateTime ahora) {
        validarEtapa(Proceso.APROBACION_CONSEJO);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        // Se valida todo antes de modificar el anteproyecto
        Estado resultado = this.anteproyecto.estadoTrasDecisionConsejo(decision);
        Ejecucion nueva = null;
        CambioEtapa cambio;
        if (resultado == Estado.APROBADO) {
            nueva = Ejecucion.iniciar(decision.fecha(), getModalidad());
            cambio = new CambioEtapa(this.etapa, Proceso.EN_CURSO, ahora, responsable,
                    "Anteproyecto aprobado por el Consejo de Facultad");
        } else {
            cambio = new CambioEtapa(this.etapa, Proceso.ANTEPROYECTO, ahora, responsable,
                    "Anteproyecto no aprobado por el Consejo de Facultad");
        }
        this.anteproyecto.registrarDecisionConsejo(decision);
        if (nueva != null)
            this.ejecucion = nueva;
        aplicarCambio(cambio);
    }

    // ----- Fase 4: en curso -----

    /** Responsable registrado en los cambios de etapa que aplica el sistema (ej. vencimientos). */
    public static final String RESPONSABLE_SISTEMA = "SISTEMA";

    /**
     * Registra la solicitud del director para cambiar el título u objetivos
     * (Arts. 17-18). Solo en curso, una sola vez (un cambio aprobado agota la
     * oportunidad) y dentro de los primeros 6 meses desde la notificación. La
     * decide el Consejo de Facultad con {@link #resolverCambioObjetivos}.
     *
     * @param titulo        título propuesto
     * @param objetivos     objetivos propuestos
     * @param justificacion motivo del cambio
     * @param hoy           fecha de la solicitud
     */
    public void solicitarCambioObjetivos(String titulo, List<String> objetivos, String justificacion,
            LocalDate hoy) {
        validarEtapa(Proceso.EN_CURSO);
        this.ejecucion.solicitarCambioObjetivos(
                CambioObjetivos.solicitar(this.titulo, this.objetivos, titulo, objetivos, justificacion, hoy));
    }

    /**
     * Registra la decisión del Consejo de Facultad sobre el cambio de título u
     * objetivos solicitado. Si lo aprueba, el trabajo adopta la nueva
     * descripción; la etapa no cambia.
     *
     * @param decision decisión del Consejo: APROBADO o NO_APROBADO, con la resolución
     */
    public void resolverCambioObjetivos(Decision decision) {
        validarEtapa(Proceso.EN_CURSO);
        CambioObjetivos resuelta = this.ejecucion.resolverCambioObjetivos(decision);
        if (resuelta.fueAprobado()) {
            this.titulo = resuelta.tituloPropuesto();
            this.objetivos = new ArrayList<>(resuelta.objetivosPropuestos());
        }
    }

    /**
     * Radica un informe mensual de la práctica profesional (Art. 45). Lo radica
     * el director con el documento adjunto, mientras el trabajo está en curso o
     * en prórroga.
     *
     * @param rutaArchivo ruta del documento del informe
     * @param hoy         fecha de radicación
     * @return el informe registrado
     */
    public InformeMensual registrarInformeMensual(String rutaArchivo, LocalDate hoy) {
        if (getModalidad() != Modalidad.PRACTICA_PROFESIONAL)
            throw new ReglaNegocioException("error.dominio.informe_mensual.solo_practica");
        if (this.etapa != Proceso.EN_CURSO && this.etapa != Proceso.PRORROGA)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
        return this.ejecucion.registrarInformeMensual(rutaArchivo, hoy);
    }

    // ----- Fase 5: prórroga -----

    /**
     * Registra la solicitud de prórroga del director (Arts. 16, 41): una sola
     * vez (una negada también cuenta), por 1 a 3 meses, con el documento de la
     * solicitud y al menos 15 días calendario antes del fin del plazo. La
     * decide el Consejo de Facultad con {@link #resolverProrroga}.
     *
     * @param meses         meses solicitados, de 1 a 3
     * @param rutaSolicitud ruta del documento de la solicitud
     * @param hoy           fecha de la solicitud
     */
    public void solicitarProrroga(int meses, String rutaSolicitud, LocalDate hoy) {
        validarEtapa(Proceso.EN_CURSO);
        this.ejecucion.solicitarProrroga(Prorroga.solicitar(meses, rutaSolicitud, hoy));
    }

    /**
     * Registra la decisión del Consejo de Facultad sobre la prórroga. Si la
     * aprueba, el plazo se extiende por los meses solicitados y el trabajo pasa
     * a prórroga; si no, sigue en curso con el plazo original.
     *
     * @param decision    decisión del Consejo: APROBADO o NO_APROBADO, con la resolución
     * @param responsable código del usuario que registra la decisión
     * @param ahora       fecha y hora del registro
     */
    public void resolverProrroga(Decision decision, String responsable, LocalDateTime ahora) {
        validarEtapa(Proceso.EN_CURSO);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.fecha_obligatoria");
        // Se valida todo antes de modificar la ejecución
        Prorroga resuelta = this.ejecucion.prorrogaResuelta(decision);
        CambioEtapa cambio = resuelta.fueAprobada()
                ? new CambioEtapa(this.etapa, Proceso.PRORROGA, ahora, responsable,
                        "Prórroga de " + resuelta.meses() + " mes(es) aprobada por el Consejo de Facultad")
                : null;
        this.ejecucion.aplicarProrroga(resuelta);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    // ----- Fase 6: suspensión -----

    /**
     * Registra la solicitud de suspensión del director (Arts. 27, 57), con el
     * documento adjunto, desde en curso o prórroga. Solo se suspende una vez;
     * una solicitud negada no consume la oportunidad. La decide el Consejo de
     * Facultad con {@link #resolverSuspension}.
     *
     * @param rutaSolicitud ruta del documento de la solicitud
     * @param hoy           fecha de la solicitud
     */
    public void solicitarSuspension(String rutaSolicitud, LocalDate hoy) {
        validarEnDesarrollo();
        this.ejecucion.solicitarSuspension(Suspension.solicitar(rutaSolicitud, hoy));
    }

    /**
     * Registra la decisión del Consejo de Facultad sobre la suspensión. Si la
     * aprueba, rige desde la fecha de la decisión: se congelan los días que
     * faltan del plazo y el trabajo pasa a suspendido hasta que se reanude, a
     * más tardar el primer día del período académico posterior al siguiente.
     *
     * @param decision    decisión del Consejo: APROBADO o NO_APROBADO, con la resolución
     * @param responsable código del usuario que registra la decisión
     * @param ahora       fecha y hora del registro
     */
    public void resolverSuspension(Decision decision, String responsable, LocalDateTime ahora) {
        validarEnDesarrollo();
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.fecha_obligatoria");
        // Se valida todo antes de modificar la ejecución
        Suspension resuelta = this.ejecucion.suspensionResuelta(decision, this.etapa);
        CambioEtapa cambio = resuelta.fueAprobada()
                ? new CambioEtapa(this.etapa, Proceso.SUSPENDIDO, ahora, responsable,
                        "Suspensión aprobada por el Consejo de Facultad hasta el "
                                + resuelta.fechaMaximaReanudacion())
                : null;
        this.ejecucion.aplicarSuspension(resuelta);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    /**
     * Reanuda el trabajo suspendido a solicitud del director, con el documento
     * adjunto. Vuelve a la etapa en la que estaba y el nuevo fin del plazo es
     * la fecha de reanudación más los días que faltaban al suspender.
     *
     * @param rutaDocumento ruta del documento de la reanudación
     * @param responsable   código del usuario que reanuda
     * @param ahora         fecha y hora de la reanudación
     */
    public void reanudar(String rutaDocumento, String responsable, LocalDateTime ahora) {
        if (CheckString.isBlank(rutaDocumento))
            throw new ReglaNegocioException("error.dominio.suspension.documento_reanudacion_obligatorio");
        reanudar(rutaDocumento, responsable, ahora, ahora == null ? null : ahora.toLocalDate(),
                "Reanudación solicitada por el director");
    }

    private void reanudar(String rutaDocumento, String responsable, LocalDateTime ahora, LocalDate fecha,
            String observacion) {
        validarEtapa(Proceso.SUSPENDIDO);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.fecha_obligatoria");
        Suspension reanudada = this.ejecucion.suspensionReanudada(fecha, rutaDocumento);
        CambioEtapa cambio = new CambioEtapa(this.etapa, reanudada.etapaPrevia(), ahora, responsable, observacion);
        this.ejecucion.aplicarReanudacion(reanudada);
        aplicarCambio(cambio);
    }

    // ----- Fase 7: sustentación -----

    /**
     * Registra la solicitud de sustentación del director (Arts. 19-20) y pasa
     * el trabajo a sustentación, lo que detiene el vencimiento del plazo. Se
     * permite desde en curso o prórroga, después de los meses mínimos de la
     * modalidad y antes del fin del plazo, sin prórroga ni suspensión
     * pendientes de decisión.
     *
     * @param rutaDocumentoFinal ruta del documento final (formato E)
     * @param rutaPazYSalvo      ruta del paz y salvo (formato F)
     * @param rutaConceptoAsesor ruta del concepto favorable del asesor; obligatoria en práctica profesional
     * @param responsable        código del director
     * @param ahora              fecha y hora de la solicitud
     */
    public void solicitarSustentacion(String rutaDocumentoFinal, String rutaPazYSalvo, String rutaConceptoAsesor,
            String responsable, LocalDateTime ahora) {
        validarEnDesarrollo();
        validarAhora(ahora);
        LocalDate hoy = ahora.toLocalDate();
        if (!this.ejecucion.permiteSolicitarSustentacion(hoy))
            throw new ReglaNegocioException("error.dominio.sustentacion.antes_de_tiempo");
        if (this.ejecucion.vencida(hoy))
            throw new ReglaNegocioException("error.dominio.sustentacion.plazo_vencido");
        if (this.ejecucion.tieneProrrogaPendiente() || this.ejecucion.tieneSuspensionPendiente())
            throw new ReglaNegocioException("error.dominio.sustentacion.decision_pendiente");
        Sustentacion nueva = Sustentacion.solicitar(rutaDocumentoFinal, rutaPazYSalvo, rutaConceptoAsesor, hoy,
                getModalidad() == Modalidad.PRACTICA_PROFESIONAL);
        CambioEtapa cambio = new CambioEtapa(this.etapa, Proceso.SUSTENTACION, ahora, responsable,
                "Solicitud de sustentación");
        this.sustentacion = nueva;
        aplicarCambio(cambio);
    }

    /**
     * Registra la programación del Consejo de Facultad: dos jurados (pueden
     * ser los evaluadores del anteproyecto, pero no director, codirector,
     * asesor ni estudiantes), fecha, hora y salón. Programa la primera sesión
     * o, después de un aplazamiento, la segunda (máximo 60 días calendario).
     *
     * @param responsable código del usuario del Consejo
     * @param ahora       fecha y hora del registro
     */
    public void programarSustentacion(Evaluador jurado1, Evaluador jurado2, LocalDateTime fechaHora, String salon,
            String responsable, LocalDateTime ahora) {
        if ((this.etapa != Proceso.SUSTENTACION && this.etapa != Proceso.APLAZADO) || this.sustentacion == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
        validarAhora(ahora);
        if (Arrays.asList(jurado1, jurado2).stream().filter(Objects::nonNull).anyMatch(this::tieneOtroPapel))
            throw new ReglaNegocioException("error.dominio.sustentacion.jurado_con_conflicto");
        SesionSustentacion sesion = new SesionSustentacion(this.sustentacion.getSesiones().size() + 1,
                Arrays.asList(jurado1, jurado2), fechaHora, salon, ahora.toLocalDate());
        CambioEtapa cambio = this.etapa == Proceso.APLAZADO
                ? new CambioEtapa(this.etapa, Proceso.SUSTENTACION, ahora, responsable,
                        "Programación de la segunda sesión de sustentación")
                : null;
        this.sustentacion.programar(sesion);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    /**
     * Registra la calificación de un jurado de la sesión actual. Con las dos
     * calificaciones rige la menos favorable: APROBADO espera la versión
     * definitiva, APROBADO CON OBSERVACIONES abre 10 días hábiles para los
     * ajustes, APLAZADO pasa a aplazado (o cierra en la segunda sesión) y NO
     * APROBADO cierra el trabajo.
     *
     * @param jurado     jurado que califica
     * @param rutaActa   ruta del acta de sustentación (formato G), opcional
     * @param ahora      fecha y hora de la calificación
     * @param calendario calendario para el plazo de los ajustes
     */
    public void calificarSustentacion(Evaluador jurado, Calificacion calificacion, String observaciones,
            String rutaActa, LocalDateTime ahora, CalendarioHabil calendario) {
        validarEtapaSustentacion();
        validarAhora(ahora);
        this.sustentacion.calificar(new CalificacionJurado(this.sustentacion.sesionActual().numero(), jurado,
                calificacion, observaciones, rutaActa, ahora.toLocalDate()), calendario);
        sincronizarConSustentacion(jurado.getCodigo(), ahora);
    }

    /**
     * Registra que el estudiante no asistió a la sesión: en la primera se
     * aplaza; en la segunda el trabajo queda NO APROBADO por inasistencia.
     *
     * @param jurado        jurado que la registra
     * @param observaciones soporte de la inasistencia
     */
    public void registrarInasistencia(Evaluador jurado, String observaciones, LocalDateTime ahora) {
        validarEtapaSustentacion();
        validarAhora(ahora);
        this.sustentacion.registrarInasistencia(new RegistroJurado(this.sustentacion.sesionActual().numero(),
                jurado, false, observaciones, ahora.toLocalDate()));
        sincronizarConSustentacion(jurado.getCodigo(), ahora);
    }

    /**
     * Registra los ajustes que radica el director a una sustentación aprobada
     * con observaciones, dentro de los 10 días hábiles.
     *
     * @param rutaDocumento ruta del documento con los ajustes
     */
    public void radicarAjustesSustentacion(String rutaDocumento, LocalDateTime ahora) {
        validarEtapaSustentacion();
        validarAhora(ahora);
        this.sustentacion.radicarAjustes(rutaDocumento, ahora.toLocalDate());
    }

    /**
     * Registra la verificación de un jurado sobre los ajustes. Si ambos
     * cumplen, la sustentación queda aprobada; si alguno no, se trata como
     * APLAZADO.
     *
     * @param cumple        true si los ajustes cumplen
     * @param observaciones obligatorias si no cumplen
     */
    public void verificarAjustesSustentacion(Evaluador jurado, boolean cumple, String observaciones,
            LocalDateTime ahora) {
        validarEtapaSustentacion();
        validarAhora(ahora);
        this.sustentacion.verificarAjustes(new RegistroJurado(this.sustentacion.sesionActual().numero(), jurado,
                cumple, observaciones, ahora.toLocalDate()));
        sincronizarConSustentacion(jurado.getCodigo(), ahora);
    }

    /**
     * Registra la versión definitiva (formato H) que carga un jurado de la
     * sustentación aprobada: el trabajo finaliza.
     *
     * @param rutaDocumento ruta de la versión definitiva
     */
    public void cargarVersionDefinitiva(Evaluador jurado, String rutaDocumento, LocalDateTime ahora) {
        validarEtapaSustentacion();
        validarAhora(ahora);
        this.sustentacion.cargarVersionDefinitiva(jurado, rutaDocumento, ahora.toLocalDate());
        sincronizarConSustentacion(jurado.getCodigo(), ahora);
    }

    /**
     * @param usuarioId usuario a verificar
     * @return true si el usuario es o fue jurado de la sustentación
     */
    public boolean esJurado(Long usuarioId) {
        return this.sustentacion != null && this.sustentacion.esJurado(usuarioId);
    }

    private void validarEtapaSustentacion() {
        if (this.etapa != Proceso.SUSTENTACION || this.sustentacion == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
    }

    private static void validarAhora(LocalDateTime ahora) {
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.fecha_obligatoria");
    }

    /**
     * Lleva el trabajo a la etapa que corresponde al estado de la
     * sustentación: aplazado, finalizado o no aprobado.
     */
    private void sincronizarConSustentacion(String responsable, LocalDateTime ahora) {
        switch (this.sustentacion.getEstado()) {
            case APLAZADA -> {
                if (this.etapa == Proceso.SUSTENTACION)
                    aplicarCambio(new CambioEtapa(this.etapa, Proceso.APLAZADO, ahora, responsable,
                            "Sustentación aplazada; segunda sesión a más tardar el "
                                    + this.sustentacion.getFechaLimiteSegundaSesion()));
            }
            case FINALIZADA -> aplicarCambio(new CambioEtapa(this.etapa, Proceso.FINALIZADO, ahora, responsable,
                    "Versión definitiva cargada"));
            case NO_APROBADA -> cerrar(this.sustentacion.getMotivoNoAprobacion(), responsable, ahora,
                    "Sustentación no aprobada");
            default -> {
                // Los demás estados no cambian la etapa
            }
        }
    }

    // ----- Fase 8: cancelación -----

    /**
     * Registra la solicitud de cancelación del director (Arts. 26, 56), con el
     * formato de solicitud adjunto, desde cualquier etapa no final. Mientras el
     * Consejo decide, el trabajo sigue su curso; solo puede haber una solicitud
     * pendiente.
     *
     * @param rutaSolicitud ruta del formato de solicitud de cancelación
     * @param hoy           fecha de la solicitud
     */
    public void solicitarCancelacion(String rutaSolicitud, LocalDate hoy) {
        if (estaCerrado())
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
        if (cancelacionPendiente().isPresent())
            throw new ReglaNegocioException("error.dominio.cancelacion.solicitud_pendiente");
        this.cancelaciones.add(Cancelacion.solicitar(rutaSolicitud, hoy, this.etapa));
    }

    /**
     * Registra la decisión del Consejo de Facultad sobre la cancelación. Si la
     * aprueba, el trabajo queda CANCELADO; si no, sigue su curso y se puede
     * volver a solicitar.
     *
     * @param decision    decisión del Consejo: APROBADO o NO_APROBADO, con la resolución
     * @param responsable código del usuario que registra la decisión
     * @param ahora       fecha y hora del registro
     */
    public void resolverCancelacion(Decision decision, String responsable, LocalDateTime ahora) {
        if (estaCerrado())
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
        validarAhora(ahora);
        Cancelacion resuelta = cancelacionPendiente()
                .orElseThrow(() -> new ReglaNegocioException("error.dominio.cancelacion.sin_solicitud_pendiente"))
                .resolver(decision);
        // Se valida el cambio de etapa antes de modificar el trabajo
        CambioEtapa cambio = resuelta.fueAprobada()
                ? new CambioEtapa(this.etapa, MotivoCierre.CANCELACION_DIRECTOR.getEtapaCierre(), ahora, responsable,
                        "Cancelación aprobada por el Consejo de Facultad")
                : null;
        this.cancelaciones.set(this.cancelaciones.size() - 1, resuelta);
        if (cambio != null) {
            aplicarCambio(cambio);
            this.motivoCierre = MotivoCierre.CANCELACION_DIRECTOR;
        }
    }

    /**
     * @return la solicitud de cancelación pendiente de decisión, si existe
     */
    public Optional<Cancelacion> cancelacionPendiente() {
        if (this.cancelaciones.isEmpty())
            return Optional.empty();
        Cancelacion ultima = this.cancelaciones.get(this.cancelaciones.size() - 1);
        return ultima.estaPendiente() ? Optional.of(ultima) : Optional.empty();
    }

    private void validarEnDesarrollo() {
        if (this.etapa != Proceso.EN_CURSO && this.etapa != Proceso.PRORROGA)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
    }

    /**
     * Aplica los plazos del reglamento. Lo invoca una tarea programada diaria:
     * <ul>
     * <li>Un trabajo suspendido que llega a la fecha máxima de reanudación se
     * reanuda automáticamente en esa fecha.</li>
     * <li>En sustentación: si vencieron los 10 días hábiles de los ajustes, se
     * aplaza; si vencieron los 60 días del aplazamiento sin segunda sesión,
     * queda NO APROBADO por vencimiento.</li>
     * <li>Si el plazo de ejecución terminó sin iniciar la sustentación, el
     * trabajo queda NO APROBADO por vencimiento (Art. 21). Mientras haya una
     * prórroga o una suspensión pendiente de decisión no se cierra: se espera
     * al Consejo.</li>
     * </ul>
     *
     * @param ahora fecha y hora de la verificación
     * @return true si el trabajo cambió de etapa
     */
    public boolean verificarVencimientos(LocalDateTime ahora) {
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.fecha_obligatoria");
        LocalDate hoy = ahora.toLocalDate();
        if ((this.etapa == Proceso.SUSTENTACION || this.etapa == Proceso.APLAZADO) && this.sustentacion != null) {
            if (!this.sustentacion.verificarPlazos(hoy))
                return false;
            sincronizarConSustentacion(RESPONSABLE_SISTEMA, ahora);
            return true;
        }
        boolean cambio = false;
        if (this.etapa == Proceso.SUSPENDIDO) {
            Suspension activa = this.ejecucion.suspensionActiva().orElse(null);
            if (activa == null || hoy.isBefore(activa.fechaMaximaReanudacion()))
                return false;
            reanudar(null, RESPONSABLE_SISTEMA, ahora, activa.fechaMaximaReanudacion(),
                    "Reanudación automática al llegar la fecha máxima de la suspensión");
            cambio = true;
        }
        boolean desarrollando = this.etapa == Proceso.EN_CURSO || this.etapa == Proceso.PRORROGA;
        if (!desarrollando || this.ejecucion == null || !this.ejecucion.vencida(hoy)
                || this.ejecucion.tieneProrrogaPendiente() || this.ejecucion.tieneSuspensionPendiente())
            return cambio;
        cerrar(MotivoCierre.VENCIMIENTO_PLAZO, RESPONSABLE_SISTEMA, ahora,
                "Plazo de ejecución vencido el " + this.ejecucion.getFechaFin());
        return true;
    }

    /**
     * Cierra el trabajo en la etapa final que corresponde al motivo.
     */
    private void cerrar(MotivoCierre motivo, String responsable, LocalDateTime ahora, String observacion) {
        CambioEtapa cambio = new CambioEtapa(this.etapa, motivo.getEtapaCierre(), ahora, responsable, observacion);
        aplicarCambio(cambio);
        this.motivoCierre = motivo;
    }

    /**
     * Un evaluador no puede cumplir otro papel en el mismo trabajo.
     */
    private boolean tieneOtroPapel(Evaluador evaluador) {
        return evaluador.esMismaPersona(this.director)
                || evaluador.esMismaPersona(this.codirector)
                || evaluador.esMismaPersona(this.asesor)
                || esAlgunEstudiante(evaluador, this.estudiantes);
    }

    // ----- Soporte de etapas -----

    /**
     * Verifica que el trabajo se encuentre en la etapa indicada.
     */
    protected void validarEtapa(Proceso esperada) {
        if (this.etapa != esperada)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
    }

    /**
     * Registra el cambio en el historial y actualiza la etapa. El cambio ya fue
     * validado al construirse ({@link CambioEtapa} solo acepta transiciones
     * permitidas por {@link Proceso}).
     */
    private void aplicarCambio(CambioEtapa cambio) {
        if (cambio.origen() != this.etapa)
            throw new ReglaNegocioException("error.dominio.proceso.transicion_invalida");
        this.historial.add(cambio);
        this.etapa = cambio.destino();
    }

    /**
     * @param usuarioId usuario a verificar
     * @return true si el usuario es el director del trabajo
     */
    public boolean esDirector(Long usuarioId) {
        return usuarioId != null && usuarioId.equals(this.director.getUsuarioId());
    }

    /**
     * @param usuarioId usuario a verificar
     * @return true si el usuario es evaluador asignado del anteproyecto
     */
    public boolean esEvaluador(Long usuarioId) {
        return this.anteproyecto.evaluador(usuarioId).isPresent();
    }

    /**
     * @param usuarioId usuario a verificar
     * @return true si el usuario es estudiante, director, codirector, asesor,
     *         evaluador o jurado del trabajo
     */
    public boolean participa(Long usuarioId) {
        if (usuarioId == null)
            return false;
        return esDirector(usuarioId)
                || (this.codirector != null && usuarioId.equals(this.codirector.getUsuarioId()))
                || (this.asesor != null && usuarioId.equals(this.asesor.getUsuarioId()))
                || this.estudiantes.stream().anyMatch(estudiante -> usuarioId.equals(estudiante.getUsuarioId()))
                || esEvaluador(usuarioId) || esJurado(usuarioId);
    }

    /**
     * @return true si el trabajo está en una etapa final
     */
    public boolean estaCerrado() {
        return this.etapa.esFinal();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getObjetivos() {
        return Collections.unmodifiableList(objetivos);
    }

    public List<Estudiante> getEstudiantes() {
        return Collections.unmodifiableList(estudiantes);
    }

    public Director getDirector() {
        return director;
    }

    public Director getCodirector() {
        return codirector;
    }

    public Asesor getAsesor() {
        return asesor;
    }

    public Proceso getEtapa() {
        return etapa;
    }

    public Propuesta getPropuesta() {
        return propuesta;
    }

    public Anteproyecto getAnteproyecto() {
        return anteproyecto;
    }

    public Ejecucion getEjecucion() {
        return ejecucion;
    }

    public Sustentacion getSustentacion() {
        return sustentacion;
    }

    public List<Cancelacion> getCancelaciones() {
        return Collections.unmodifiableList(cancelaciones);
    }

    public List<CambioEtapa> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public MotivoCierre getMotivoCierre() {
        return motivoCierre;
    }
}
