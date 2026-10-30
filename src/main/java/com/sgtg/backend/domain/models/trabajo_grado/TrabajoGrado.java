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
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.services.CalendarioHabil;
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
    /** Descripción general de la propuesta, null si no tiene */
    private String descripcion;
    /** Línea de investigación, null si no tiene */
    private String lineaInvestigacion;
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
     * Versión con la que se leyó el trabajo, null si es nuevo. La persistencia la
     * usa para rechazar el guardado si otra operación lo modificó entre tanto.
     */
    private Long version;

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
        this.descripcion = datos.descripcion();
        this.lineaInvestigacion = datos.lineaInvestigacion();
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
        this.version = datos.version();
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
        // Un solo Comité de Programa emite el concepto: todos los estudiantes son del mismo programa
        Programa programa = estudiantes.get(0).getPrograma();
        if (estudiantes.stream().anyMatch(estudiante -> !estudiante.getPrograma().esMismo(programa)))
            throw new ReglaNegocioException("error.dominio.trabajo_grado.estudiantes_de_otro_programa");
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
     * Registra la descripción general y la línea de investigación del trabajo
     * (ambas opcionales).
     */
    public void describir(String descripcion, String lineaInvestigacion) {
        this.descripcion = CheckString.isBlank(descripcion) ? null : descripcion.trim();
        this.lineaInvestigacion = CheckString.isBlank(lineaInvestigacion) ? null : lineaInvestigacion.trim();
    }

    /**
     * Corrige el título, los objetivos, la descripción y la línea de
     * investigación de la propuesta. Solo mientras se puede radicar una versión:
     * antes de la primera radicación o después de que el Comité no la apruebe.
     * Los participantes no cambian.
     */
    public void actualizarPropuesta(String titulo, List<String> objetivos, String descripcion,
            String lineaInvestigacion) {
        validarEtapa(Proceso.PROPUESTA);
        if (!this.propuesta.puedeRadicar())
            throw new ReglaNegocioException("error.dominio.propuesta.edicion_no_permitida");
        corregirDatos(titulo, objetivos, descripcion, lineaInvestigacion);
    }

    private void corregirDatos(String titulo, List<String> objetivos, String descripcion, String lineaInvestigacion) {
        validarDescripcion(titulo, objetivos);
        this.titulo = titulo;
        this.objetivos = new ArrayList<>(objetivos);
        describir(descripcion, lineaInvestigacion);
    }

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
     * Corrige el título, los objetivos, la descripción y la línea de
     * investigación junto con el anteproyecto. Solo mientras se puede radicar
     * una versión: la primera, después de que los evaluadores soliciten
     * correcciones o después de que el Consejo de Facultad no lo apruebe. Los
     * participantes no cambian.
     */
    public void actualizarAnteproyecto(String titulo, List<String> objetivos, String descripcion,
            String lineaInvestigacion) {
        validarEtapa(Proceso.ANTEPROYECTO);
        if (!this.anteproyecto.puedeRadicar())
            throw new ReglaNegocioException("error.dominio.anteproyecto.edicion_no_permitida");
        corregirDatos(titulo, objetivos, descripcion, lineaInvestigacion);
    }

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
        // Solo se registra el anteproyecto de una propuesta que el Comité consideró pertinente
        if (this.etapa == Proceso.PROPUESTA)
            throw new ReglaNegocioException("error.dominio.anteproyecto.propuesta_no_pertinente");
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
     * Fecha límite que tuvo el evaluador para emitir un concepto: los días
     * hábiles del plazo contados desde que la versión evaluada quedó en manos
     * de los evaluadores, es decir, desde su radicación o, si se radicó antes,
     * desde la asignación de los evaluadores (primer paso a EVALUACION).
     *
     * @param concepto   concepto del evaluador
     * @param calendario calendario para contar los días hábiles
     * @return la fecha límite, vacía si no se conoce la versión o la asignación
     */
    public Optional<LocalDate> fechaLimiteConcepto(ConceptoEvaluacion concepto, CalendarioHabil calendario) {
        Optional<LocalDate> asignacion = this.historial.stream()
                .filter(cambio -> cambio.destino() == Proceso.EVALUACION)
                .findFirst()
                .map(cambio -> cambio.fecha().toLocalDate());
        Optional<LocalDate> radicacion = this.anteproyecto.getVersiones().stream()
                .map(VersionDocumento::documento)
                .filter(documento -> documento.version() == concepto.version())
                .findFirst()
                .map(Documento::fechaCarga);
        if (asignacion.isEmpty() || radicacion.isEmpty())
            return Optional.empty();
        LocalDate inicio = radicacion.get().isAfter(asignacion.get()) ? radicacion.get() : asignacion.get();
        return Optional.of(calendario.sumarDiasHabiles(inicio, Anteproyecto.DIAS_HABILES_EVALUACION));
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
        registrarConceptoEvaluador(concepto, rutaFormato, null, ahora, calendario);
    }

    /**
     * Registra el concepto de un evaluador con el formato B (obligatorio) y,
     * opcionalmente, el documento con sus anotaciones. Con correcciones el
     * trabajo vuelve a anteproyecto; si ambos aprueban sigue en evaluación
     * hasta que el Jefe de departamento lo remita al Consejo con el formato C.
     *
     * @param rutaFormato     ruta del formato B
     * @param rutaAnotaciones ruta del documento de anotaciones, null si no se adjuntó
     */
    public void registrarConceptoEvaluador(Concepto concepto, String rutaFormato, String rutaAnotaciones,
            LocalDateTime ahora, CalendarioHabil calendario) {
        validarEtapa(Proceso.EVALUACION);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        // Se valida todo antes de modificar el anteproyecto
        Estado resultado = this.anteproyecto.estadoTrasConcepto(concepto);
        this.anteproyecto.validarFormatoB(rutaFormato);
        String responsable = concepto.emisor().getCodigo();
        CambioEtapa cambio = resultado == Estado.CON_CORRECCIONES
                ? new CambioEtapa(this.etapa, Proceso.ANTEPROYECTO, ahora, responsable,
                        "Anteproyecto con correcciones de los evaluadores")
                : null;
        this.anteproyecto.registrarConcepto(concepto, rutaFormato, rutaAnotaciones, calendario);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    /**
     * El Jefe de departamento remite al Consejo de Facultad, con el formato C,
     * la versión del anteproyecto que ambos evaluadores aprobaron. Es un paso
     * obligatorio antes de la decisión del Consejo.
     *
     * @param rutaFormatoC ruta del formato C
     * @param responsable  código del Jefe de departamento
     * @param ahora        fecha y hora de la remisión
     */
    public void remitirAnteproyectoAlConsejo(String rutaFormatoC, String responsable, LocalDateTime ahora) {
        validarEtapa(Proceso.EVALUACION);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        // Se valida todo antes de modificar el anteproyecto
        this.anteproyecto.validarRemision(rutaFormatoC, ahora.toLocalDate());
        CambioEtapa cambio = new CambioEtapa(this.etapa, Proceso.APROBACION_CONSEJO, ahora, responsable,
                "Anteproyecto remitido al Consejo de Facultad con el formato C");
        this.anteproyecto.remitir(rutaFormatoC, ahora.toLocalDate());
        aplicarCambio(cambio);
    }

    // ----- Fase 3: aprobación del Consejo de Facultad -----

    /**
     * Registra el concepto del Consejo de Facultad sobre el anteproyecto
     * aprobado por los evaluadores (Art. 14). La decisión adjunta la ruta de
     * la resolución. Si lo aprueba, la fecha de la decisión es la de
     * notificación: el trabajo pasa a en curso y empieza el plazo que fija el
     * Consejo. Si no lo aprueba, vuelve a anteproyecto para radicar una
     * nueva versión que revisan los evaluadores.
     *
     * @param decision    decisión del Consejo: APROBADO o NO_APROBADO
     * @param responsable código del usuario que registra la decisión
     * @param ahora       fecha y hora del registro
     * @param mesesEjecucion plazo de ejecución que fija el Consejo al aprobar (6 o 9 meses en
     *                       investigación; 6, 9 o 12 en práctica profesional); se ignora si no aprueba
     */
    public void registrarDecisionConsejo(Decision decision, String responsable, LocalDateTime ahora,
            Integer mesesEjecucion) {
        validarEtapa(Proceso.APROBACION_CONSEJO);
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.anteproyecto.datos_incompletos");
        // Se valida todo antes de modificar el anteproyecto
        Estado resultado = this.anteproyecto.estadoTrasDecisionConsejo(decision);
        Ejecucion nueva = null;
        CambioEtapa cambio;
        if (resultado == Estado.APROBADO) {
            nueva = Ejecucion.iniciar(decision.fecha(), getModalidad(), mesesEjecucion);
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
        return registrarInformeMensual(rutaArchivo, hoy, null, null);
    }

    /**
     * Registra un informe mensual en cualquiera de las dos modalidades, mientras
     * el trabajo está en curso o en prórroga (no durante una suspensión).
     *
     * @param radicadoPor   código de quien radica (estudiante o director)
     * @param observaciones observaciones opcionales de quien radica
     */
    public InformeMensual registrarInformeMensual(String rutaArchivo, LocalDate hoy, String radicadoPor,
            String observaciones) {
        if (this.etapa != Proceso.EN_CURSO && this.etapa != Proceso.PRORROGA)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
        return this.ejecucion.registrarInformeMensual(rutaArchivo, hoy, radicadoPor, observaciones);
    }

    /**
     * Registra la revisión de un informe mensual por el director, el codirector
     * o el asesor. Es una formalidad: no cambia la etapa ni el flujo del trabajo.
     *
     * @param numero   consecutivo del informe
     * @param revision revisión con observaciones y, opcionalmente, el PDF de notas
     */
    public InformeMensual revisarInformeMensual(int numero, RevisionInforme revision) {
        if (this.ejecucion == null || estaCerrado())
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
        return this.ejecucion.revisarInformeMensual(numero, revision);
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
        reanudar(rutaDocumento, responsable, ahora, fechaDeReanudacion(ahora),
                "Reanudación solicitada por el director");
    }

    /**
     * Fecha desde la que rige la reanudación del director: la del día o, si ya
     * pasó la fecha máxima de la suspensión sin que la verificación diaria la
     * aplicara, esa fecha máxima (el trabajo se reanuda a más tardar ese día).
     */
    private LocalDate fechaDeReanudacion(LocalDateTime ahora) {
        if (ahora == null)
            return null;
        LocalDate hoy = ahora.toLocalDate();
        LocalDate maxima = this.ejecucion == null ? null
                : this.ejecucion.suspensionActiva().map(Suspension::fechaMaximaReanudacion).orElse(null);
        return maxima != null && hoy.isAfter(maxima) ? maxima : hoy;
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
     * @param rutaMonografia     ruta de la monografía
     * @param rutaDocumentoFinal ruta del documento final (formato E)
     * @param rutaConceptoAsesor ruta del concepto favorable del asesor; obligatoria en práctica profesional
     * @param rutaAnexos         ruta de los anexos, opcional
     * @param responsable        código del director
     * @param ahora              fecha y hora de la solicitud
     */
    public void solicitarSustentacion(String rutaMonografia, String rutaDocumentoFinal, String rutaConceptoAsesor,
            String rutaAnexos, String responsable, LocalDateTime ahora) {
        // Decanatura no aprobó el paz y salvo: el director radica una nueva solicitud (sigue en sustentación)
        if (this.etapa == Proceso.SUSTENTACION && this.sustentacion != null && this.sustentacion.solicitudNoAprobada()) {
            validarAhora(ahora);
            this.sustentacion.solicitarDeNuevo(rutaMonografia, rutaDocumentoFinal, rutaConceptoAsesor, rutaAnexos,
                    ahora.toLocalDate(), getModalidad() == Modalidad.PRACTICA_PROFESIONAL);
            return;
        }
        validarEnDesarrollo();
        validarAhora(ahora);
        LocalDate hoy = ahora.toLocalDate();
        if (!this.ejecucion.permiteSolicitarSustentacion(hoy))
            throw new ReglaNegocioException("error.dominio.sustentacion.antes_de_tiempo");
        if (this.ejecucion.vencida(hoy))
            throw new ReglaNegocioException("error.dominio.sustentacion.plazo_vencido");
        if (this.ejecucion.tieneProrrogaPendiente() || this.ejecucion.tieneSuspensionPendiente())
            throw new ReglaNegocioException("error.dominio.sustentacion.decision_pendiente");
        Sustentacion nueva = Sustentacion.solicitar(rutaMonografia, rutaDocumentoFinal, rutaConceptoAsesor,
                rutaAnexos, hoy, getModalidad() == Modalidad.PRACTICA_PROFESIONAL);
        CambioEtapa cambio = new CambioEtapa(this.etapa, Proceso.SUSTENTACION, ahora, responsable,
                "Solicitud de sustentación");
        this.sustentacion = nueva;
        aplicarCambio(cambio);
    }

    /**
     * Registra la revisión del paz y salvo (formato F) de Decanatura sobre la
     * sustentación solicitada: lo aprueba o no, siempre con el documento. Sin
     * paz y salvo aprobado el Consejo no programa la sustentación.
     *
     * @param rutaDocumento ruta del formato F
     * @param aprobado      true si el estudiante está a paz y salvo
     * @param observaciones motivo de la no aprobación (obligatorio si no lo aprueba)
     * @param ahora         fecha y hora de la revisión
     */
    public void revisarPazYSalvo(String rutaDocumento, boolean aprobado, String observaciones, LocalDateTime ahora) {
        validarEtapa(Proceso.SUSTENTACION);
        validarAhora(ahora);
        this.sustentacion.revisarPazYSalvo(
                new RevisionPazYSalvo(rutaDocumento, aprobado, observaciones, ahora.toLocalDate()));
    }

    /**
     * Registra la programación del Consejo de Facultad: el jurado coordinador
     * y el otro jurado (ninguno puede ser director, codirector, asesor,
     * estudiante ni evaluador del anteproyecto de este trabajo), fecha, hora,
     * salón y el formato E. Programa la primera sesión o, después de un
     * aplazamiento, la segunda (máximo 60 días calendario).
     *
     * @param coordinador  jurado coordinador: califica y registra las decisiones de la sesión
     * @param jurado       el otro jurado, que solo consulta
     * @param rutaFormatoE ruta del formato E que adjunta el Consejo
     * @param responsable  código del usuario del Consejo
     * @param ahora        fecha y hora del registro
     */
    public void programarSustentacion(Evaluador coordinador, Evaluador jurado, LocalDateTime fechaHora, String salon,
            String rutaFormatoE, String responsable, LocalDateTime ahora) {
        if ((this.etapa != Proceso.SUSTENTACION && this.etapa != Proceso.APLAZADO) || this.sustentacion == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.etapa_invalida");
        validarAhora(ahora);
        if (Arrays.asList(coordinador, jurado).stream().filter(Objects::nonNull)
                .anyMatch(j -> tieneOtroPapel(j) || esEvaluador(j.getUsuarioId())))
            throw new ReglaNegocioException("error.dominio.sustentacion.jurado_con_conflicto");
        SesionSustentacion sesion = new SesionSustentacion(this.sustentacion.getSesiones().size() + 1,
                Arrays.asList(coordinador, jurado), fechaHora, salon, ahora.toLocalDate(), rutaFormatoE);
        CambioEtapa cambio = this.etapa == Proceso.APLAZADO
                ? new CambioEtapa(this.etapa, Proceso.SUSTENTACION, ahora, responsable,
                        "Programación de la segunda sesión de sustentación")
                : null;
        this.sustentacion.programar(sesion);
        if (cambio != null)
            aplicarCambio(cambio);
    }

    /**
     * Registra la calificación del jurado coordinador de la sesión actual:
     * APROBADO espera la versión definitiva, APROBADO CON OBSERVACIONES abre
     * 10 días hábiles para los ajustes, APLAZADO pasa a aplazado (o cierra en
     * la segunda sesión) y NO APROBADO cierra el trabajo.
     *
     * @param jurado     jurado coordinador que califica
     * @param rutaActa   ruta del acta de sustentación (formato G), obligatoria
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
     * @param jurado        jurado coordinador que la registra
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
     * @param rutaAnexos    ruta de los anexos de los ajustes, opcional
     */
    public void radicarAjustesSustentacion(String rutaDocumento, String rutaAnexos, LocalDateTime ahora) {
        validarEtapaSustentacion();
        validarAhora(ahora);
        this.sustentacion.radicarAjustes(rutaDocumento, rutaAnexos, ahora.toLocalDate());
    }

    /**
     * Registra la verificación del jurado coordinador sobre los ajustes: si
     * cumplen, la sustentación queda aprobada; si no, se trata como APLAZADO.
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
     * Registra la versión definitiva (formato H) que carga el jurado
     * coordinador de la sustentación aprobada: el trabajo finaliza.
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
     * Registra la solicitud del director de una nueva fecha para la sesión
     * programada, antes de la fecha designada. Mientras el Consejo no decida,
     * no se registra la calificación ni la inasistencia.
     *
     * @param rutaSolicitud ruta del documento de la solicitud
     */
    public void solicitarNuevaFechaSustentacion(String rutaSolicitud, LocalDateTime ahora) {
        validarEtapaSustentacion();
        validarAhora(ahora);
        this.sustentacion.solicitarNuevaFecha(rutaSolicitud, ahora.toLocalDate());
    }

    /**
     * Registra la decisión del Consejo sobre la nueva fecha. Si la aprueba, la
     * sesión se reprograma con los mismos jurados; la etapa no cambia.
     *
     * @param decision       decisión del Consejo con la resolución
     * @param nuevaFechaHora nueva fecha y hora, obligatoria si se aprueba
     * @param nuevoSalon     salón; si está vacío se conserva el actual
     */
    public void resolverNuevaFechaSustentacion(Decision decision, LocalDateTime nuevaFechaHora, String nuevoSalon) {
        validarEtapaSustentacion();
        this.sustentacion.resolverNuevaFecha(decision, nuevaFechaHora, nuevoSalon);
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

    /** Etapas en las que el director puede anular el trabajo: antes de la aprobación del Consejo. */
    private static final java.util.Set<Proceso> ETAPAS_ANULABLES = java.util.EnumSet.of(Proceso.PROPUESTA,
            Proceso.ANTEPROYECTO, Proceso.EVALUACION, Proceso.APROBACION_CONSEJO);

    /**
     * Anula el trabajo por decisión del director (borrado lógico): el trabajo se
     * cierra como CANCELADO y queda archivado. Solo antes de que el Consejo
     * apruebe el anteproyecto; después se usa la cancelación con decisión del
     * Consejo.
     *
     * @param justificacion motivo de la anulación (obligatorio)
     * @param responsable   código del director
     * @param ahora         fecha y hora de la anulación
     */
    public void anular(String justificacion, String responsable, LocalDateTime ahora) {
        if (!ETAPAS_ANULABLES.contains(this.etapa))
            throw new ReglaNegocioException("error.dominio.anulacion.etapa_invalida");
        if (CheckString.isBlank(justificacion))
            throw new ReglaNegocioException("error.dominio.anulacion.justificacion_obligatoria");
        if (ahora == null)
            throw new ReglaNegocioException("error.dominio.trabajo_grado.fecha_obligatoria");
        cerrar(MotivoCierre.ANULACION_DIRECTOR, responsable, ahora, justificacion.trim());
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
     * @return true si el usuario es uno de los estudiantes del trabajo
     */
    public boolean esEstudiante(Long usuarioId) {
        return usuarioId != null
                && this.estudiantes.stream().anyMatch(estudiante -> usuarioId.equals(estudiante.getUsuarioId()));
    }

    /**
     * @param usuarioId usuario a verificar
     * @return true si el usuario puede revisar los informes mensuales: director,
     *         codirector o asesor (con usuario) del trabajo
     */
    public boolean esRevisorDeInformes(Long usuarioId) {
        return usuarioId != null && (esDirector(usuarioId)
                || (this.codirector != null && usuarioId.equals(this.codirector.getUsuarioId()))
                || (this.asesor != null && usuarioId.equals(this.asesor.getUsuarioId())));
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

    public Long getVersion() {
        return version;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getObjetivos() {
        return Collections.unmodifiableList(objetivos);
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLineaInvestigacion() {
        return lineaInvestigacion;
    }

    public List<Estudiante> getEstudiantes() {
        return Collections.unmodifiableList(estudiantes);
    }

    /**
     * @return programa académico del trabajo: el de sus estudiantes, que son
     *         todos del mismo programa; su Comité de Programa emite el concepto
     *         sobre la propuesta
     */
    public Programa getPrograma() {
        return estudiantes.get(0).getPrograma();
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
