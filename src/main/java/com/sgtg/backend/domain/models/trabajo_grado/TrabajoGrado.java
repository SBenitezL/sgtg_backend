package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Participante;
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
        this.historial = new ArrayList<>();
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
     * @param url        ubicación del documento en el servidor de archivos
     * @param hoy        fecha de radicación
     * @param calendario calendario para calcular el plazo del Comité
     */
    public void radicarPropuesta(String url, LocalDate hoy, CalendarioHabil calendario) {
        validarEtapa(Proceso.PROPUESTA);
        this.propuesta.radicar(url, hoy, calendario);
    }

    /**
     * Registra el concepto del Comité de Programa. Si es aprobatorio, el trabajo
     * avanza a anteproyecto; con correcciones o rechazo permanece en propuesta
     * a la espera de una nueva versión.
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

    public List<CambioEtapa> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public MotivoCierre getMotivoCierre() {
        return motivoCierre;
    }
}
