package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.EstadoSustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.MotivoCierre;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trabajo de grado persistido. El agregado completo se guarda en esta tabla y
 * en colecciones de valores (estudiantes, dirección, propuesta, anteproyecto,
 * decisiones del Consejo, historial), de modo que se guarda y se lee como una unidad.
 */
@Entity
@Table(name = "trabajos_grado")
@Data
@NoArgsConstructor
public class TrabajoGradoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalidad modalidad;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Proceso etapa;

    @Enumerated(EnumType.STRING)
    private MotivoCierre motivoCierre;

    @ElementCollection
    @CollectionTable(name = "trabajo_objetivos", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    @Column(name = "objetivo", length = 1000, nullable = false)
    private List<String> objetivos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "trabajo_estudiantes", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<EstudianteTrabajoEmbeddable> estudiantes = new ArrayList<>();

    /** Director y, si existe, codirector (distinguidos por su rol) */
    @ElementCollection
    @CollectionTable(name = "trabajo_direccion", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<DirectorTrabajoEmbeddable> direccion = new ArrayList<>();

    @Embedded
    private AsesorEmbeddable asesor;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "tipo", column = @Column(name = "carta_tipo")),
            @AttributeOverride(name = "url", column = @Column(name = "carta_url", length = 1000)),
            @AttributeOverride(name = "version", column = @Column(name = "carta_version")),
            @AttributeOverride(name = "fechaCarga", column = @Column(name = "carta_fecha_carga")) })
    private DocumentoEmbeddable cartaAceptacion;

    // ----- Propuesta -----

    @Enumerated(EnumType.STRING)
    private Estado propuestaEstado;

    private LocalDate propuestaFechaLimiteConcepto;

    @ElementCollection
    @CollectionTable(name = "propuesta_versiones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<DocumentoEmbeddable> propuestaVersiones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "propuesta_decisiones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<DecisionEmbeddable> propuestaDecisiones = new ArrayList<>();

    // ----- Anteproyecto y evaluación -----

    @Enumerated(EnumType.STRING)
    private Estado anteproyectoEstado;

    private LocalDate anteproyectoFechaLimiteEvaluacion;

    private LocalDate anteproyectoFechaLimiteCorrecciones;

    @ElementCollection
    @CollectionTable(name = "anteproyecto_versiones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<DocumentoEmbeddable> anteproyectoVersiones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "anteproyecto_evaluadores", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<EvaluadorTrabajoEmbeddable> anteproyectoEvaluadores = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "anteproyecto_conceptos", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<ConceptoEvaluacionEmbeddable> anteproyectoConceptos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "anteproyecto_decisiones_consejo", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<DecisionEmbeddable> anteproyectoDecisionesConsejo = new ArrayList<>();

    // ----- Ejecución (null hasta la aprobación del Consejo) -----

    private LocalDate ejecucionFechaNotificacion;

    private LocalDate ejecucionFechaFin;

    private LocalDate ejecucionFechaMinimaSustentacion;

    @ElementCollection
    @CollectionTable(name = "ejecucion_cambios_objetivos", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<CambioObjetivosEmbeddable> ejecucionCambiosObjetivos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "ejecucion_informes_mensuales", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<InformeMensualEmbeddable> ejecucionInformesMensuales = new ArrayList<>();

    @Embedded
    private ProrrogaEmbeddable prorroga;

    @ElementCollection
    @CollectionTable(name = "ejecucion_suspensiones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<SuspensionEmbeddable> ejecucionSuspensiones = new ArrayList<>();

    // ----- Sustentación (columnas nulas hasta que se solicite) -----

    @Column(length = 500)
    private String sustentacionRutaDocumentoFinal;

    @Column(length = 500)
    private String sustentacionRutaPazYSalvo;

    @Column(length = 500)
    private String sustentacionRutaConceptoAsesor;

    private LocalDate sustentacionFechaSolicitud;

    @Enumerated(EnumType.STRING)
    private EstadoSustentacion sustentacionEstado;

    @Column(length = 500)
    private String sustentacionRutaAjustes;

    private LocalDate sustentacionFechaAjustes;

    private LocalDate sustentacionFechaLimiteAjustes;

    private LocalDate sustentacionFechaLimiteSegundaSesion;

    @Column(length = 500)
    private String sustentacionRutaVersionDefinitiva;

    private LocalDate sustentacionFechaVersionDefinitiva;

    /** Usuario del jurado que cargó la versión definitiva (jurado de la última sesión) */
    private Long sustentacionJuradoVersionDefinitivaId;

    @Enumerated(EnumType.STRING)
    private MotivoCierre sustentacionMotivoNoAprobacion;

    @ElementCollection
    @CollectionTable(name = "sustentacion_sesiones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<SesionSustentacionEmbeddable> sustentacionSesiones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "sustentacion_calificaciones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<CalificacionJuradoEmbeddable> sustentacionCalificaciones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "sustentacion_inasistencias", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<RegistroJuradoEmbeddable> sustentacionInasistencias = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "sustentacion_verificaciones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<RegistroJuradoEmbeddable> sustentacionVerificaciones = new ArrayList<>();

    // ----- Cancelación -----

    @ElementCollection
    @CollectionTable(name = "trabajo_cancelaciones", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<CancelacionEmbeddable> cancelaciones = new ArrayList<>();

    // ----- Historial -----

    @ElementCollection
    @CollectionTable(name = "trabajo_historial_etapas", joinColumns = @JoinColumn(name = "trabajo_id"))
    @OrderColumn(name = "orden")
    private List<CambioEtapaEmbeddable> historial = new ArrayList<>();
}
