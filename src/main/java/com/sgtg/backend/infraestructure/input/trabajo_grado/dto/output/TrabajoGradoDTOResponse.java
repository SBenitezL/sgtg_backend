package com.sgtg.backend.infraestructure.input.trabajo_grado.dto.output;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.Anteproyecto;
import com.sgtg.backend.domain.models.trabajo_grado.Calificacion;
import com.sgtg.backend.domain.models.trabajo_grado.CalificacionJurado;
import com.sgtg.backend.domain.models.trabajo_grado.CambioEtapa;
import com.sgtg.backend.domain.models.trabajo_grado.Cancelacion;
import com.sgtg.backend.domain.models.trabajo_grado.CambioObjetivos;
import com.sgtg.backend.domain.models.trabajo_grado.ConceptoEvaluacion;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.Ejecucion;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.EstadoSustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.InformeMensual;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.MotivoCierre;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Prorroga;
import com.sgtg.backend.domain.models.trabajo_grado.RegistroJurado;
import com.sgtg.backend.domain.models.trabajo_grado.SesionSustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.Suspension;
import com.sgtg.backend.domain.models.trabajo_grado.Sustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.Propuesta;
import com.sgtg.backend.domain.models.trabajo_grado.VersionPropuesta;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.infraestructure.input.catalogo.dto.output.CatalogoDTOResponse;

/** Vista completa de un trabajo de grado. */
public record TrabajoGradoDTOResponse(Long id, Modalidad modalidad, String titulo, List<String> objetivos,
        String descripcion, String lineaInvestigacion, Proceso etapa, MotivoCierre motivoCierre, List<EstudianteDTO> estudiantes, DirectorDTO director,
        DirectorDTO codirector, AsesorDTO asesor, DocumentoDTO cartaAceptacion, PropuestaDTO propuesta,
        AnteproyectoDTO anteproyecto, EjecucionDTO ejecucion, SustentacionDTO sustentacion,
        List<CancelacionDTO> cancelaciones, List<CambioEtapaDTO> historial) {

    public record EstudianteDTO(Long usuarioId, String codigo, String nombreCompleto, String email,
            CatalogoDTOResponse programa) {
    }

    public record DirectorDTO(Long usuarioId, String codigo, String nombreCompleto, String email,
            TipoVinculacion tipoVinculacion, CatalogoDTOResponse departamento) {
    }

    public record AsesorDTO(Long usuarioId, String codigo, String nombreCompleto, String email, String organizacion,
            boolean externo) {
    }

    /**
     * @param formato código del formato del reglamento (ej. TI-A), null si no aplica
     * @param ruta    ruta del archivo; el archivo se obtiene con
     *                {@code GET /api/v1/sgtg/trabajos-grado/{trabajoId}/archivos?ruta=...}
     */
    public record DocumentoDTO(String tipo, String formato, String ruta, int version, LocalDate fechaCarga) {
    }

    public record DecisionDTO(Organo organo, ResultadoConcepto resultado, String acta, String rutaArchivo,
            String observaciones,
            LocalDate fecha) {
    }

    /**
     * Versión de la propuesta con el estado de su revisión: EN_REVISION al
     * radicarla, APROBADO o NO_APROBADO según el concepto del Comité.
     */
    public record VersionPropuestaDTO(String tipo, String formato, String ruta, int version, LocalDate fechaCarga,
            Estado estado) {
    }

    /**
     * @param versiones versiones radicadas; el estado de la propuesta es el de la última
     */
    public record PropuestaDTO(LocalDate fechaLimiteConcepto, List<VersionPropuestaDTO> versiones,
            List<DecisionDTO> decisiones) {
    }

    public record EvaluadorDTO(Long usuarioId, String codigo, String nombreCompleto, String email) {
    }

    /** @param formato formato B adjunto, null si no se cargó */
    public record ConceptoEvaluacionDTO(int version, Long evaluadorUsuarioId, String evaluador,
            ResultadoConcepto resultado, String observaciones, LocalDate fecha, DocumentoDTO formato) {
    }

    public record AnteproyectoDTO(Estado estado, LocalDate fechaLimiteEvaluacion, LocalDate fechaLimiteCorrecciones,
            List<DocumentoDTO> versiones, List<EvaluadorDTO> evaluadores, List<ConceptoEvaluacionDTO> conceptos,
            List<DecisionDTO> decisionesConsejo) {
    }

    /** @param decision decisión del Consejo, null mientras esté pendiente */
    public record CambioObjetivosDTO(String tituloAnterior, List<String> objetivosAnteriores, String tituloPropuesto,
            List<String> objetivosPropuestos, String justificacion, LocalDate fechaSolicitud, DecisionDTO decision) {
    }

    public record InformeMensualDTO(int numero, String rutaArchivo, LocalDate fecha) {
    }

    /**
     * @param decision               decisión del Consejo, null mientras esté pendiente
     * @param fechaInicio            desde cuándo rige (fecha de la decisión), null si no se aprobó
     * @param fechaMaximaReanudacion fecha en que se reanuda a más tardar, null si no se aprobó
     * @param fechaReanudacion       null mientras siga suspendido
     * @param reanudacionAutomatica  true si la reanudó el sistema al llegar la fecha máxima
     */
    public record SuspensionDTO(String rutaSolicitud, LocalDate fechaSolicitud, DecisionDTO decision,
            LocalDate fechaInicio, Proceso etapaPrevia, Long diasRestantes, LocalDate fechaMaximaReanudacion,
            LocalDate fechaReanudacion, String rutaReanudacion, boolean reanudacionAutomatica) {
    }

    /** @param decision decisión del Consejo, null mientras esté pendiente */
    public record ProrrogaDTO(int meses, String rutaSolicitud, LocalDate fechaSolicitud, DecisionDTO decision) {
    }

    /**
     * @param fechaNotificacion          inicio del plazo: fecha de la aprobación del Consejo
     * @param fechaFin                   fin del plazo, incluida la prórroga si se aprobó
     * @param fechaLimiteCambioObjetivos último día para solicitar el cambio de título u objetivos
     * @param fechaLimiteSolicitudProrroga último día para solicitar la prórroga
     * @param prorroga                   prórroga solicitada, null si no se ha solicitado
     * @param suspensiones               solicitudes de suspensión, en orden
     */
    public record EjecucionDTO(LocalDate fechaNotificacion, LocalDate fechaFin, LocalDate fechaMinimaSustentacion,
            LocalDate fechaLimiteCambioObjetivos, LocalDate fechaLimiteSolicitudProrroga,
            List<CambioObjetivosDTO> cambiosObjetivos, List<InformeMensualDTO> informesMensuales,
            ProrrogaDTO prorroga, List<SuspensionDTO> suspensiones) {
    }

    public record SesionSustentacionDTO(int numero, List<EvaluadorDTO> jurados, LocalDateTime fechaHora,
            String salon, LocalDate fechaProgramacion) {
    }

    public record CalificacionJuradoDTO(int sesion, Long juradoUsuarioId, String jurado, Calificacion calificacion,
            String observaciones, String rutaActa, LocalDate fecha) {
    }

    /** Inasistencia o verificación de ajustes; {@code favorable} indica si los ajustes cumplen. */
    public record RegistroJuradoDTO(int sesion, Long juradoUsuarioId, String jurado, boolean favorable,
            String observaciones, LocalDate fecha) {
    }

    /** @param motivoNoAprobacion motivo del cierre si no se aprobó */
    public record SustentacionDTO(EstadoSustentacion estado, String rutaDocumentoFinal, String rutaPazYSalvo,
            String rutaConceptoAsesor, LocalDate fechaSolicitud, List<SesionSustentacionDTO> sesiones,
            List<CalificacionJuradoDTO> calificaciones, List<RegistroJuradoDTO> inasistencias,
            List<RegistroJuradoDTO> verificaciones, String rutaAjustes, LocalDate fechaAjustes,
            LocalDate fechaLimiteAjustes, LocalDate fechaLimiteSegundaSesion, String rutaVersionDefinitiva,
            LocalDate fechaVersionDefinitiva, MotivoCierre motivoNoAprobacion) {
    }

    /** @param decision decisión del Consejo, null mientras esté pendiente */
    public record CancelacionDTO(String rutaSolicitud, LocalDate fechaSolicitud, Proceso etapaAlSolicitar,
            DecisionDTO decision) {
    }

    public record CambioEtapaDTO(Proceso origen, Proceso destino, LocalDateTime fecha, String responsable,
            String observacion) {
    }

    public static TrabajoGradoDTOResponse desde(TrabajoGrado trabajo) {
        Modalidad modalidad = trabajo.getModalidad();
        Propuesta propuesta = trabajo.getPropuesta();
        DocumentoDTO carta = trabajo instanceof PracticaProfesional practica
                ? documento(practica.getCartaAceptacion(), modalidad)
                : null;
        return new TrabajoGradoDTOResponse(trabajo.getId(), modalidad, trabajo.getTitulo(), trabajo.getObjetivos(),
                trabajo.getDescripcion(), trabajo.getLineaInvestigacion(), trabajo.getEtapa(), trabajo.getMotivoCierre(),
                trabajo.getEstudiantes().stream().map(TrabajoGradoDTOResponse::estudiante).toList(),
                director(trabajo.getDirector()), director(trabajo.getCodirector()), asesor(trabajo.getAsesor()), carta,
                new PropuestaDTO(propuesta.getFechaLimiteConcepto(),
                        propuesta.getVersiones().stream().map(v -> versionPropuesta(v, modalidad)).toList(),
                        propuesta.getDecisiones().stream().map(TrabajoGradoDTOResponse::decision).toList()),
                anteproyecto(trabajo.getAnteproyecto(), modalidad), ejecucion(trabajo.getEjecucion()),
                sustentacion(trabajo.getSustentacion()),
                trabajo.getCancelaciones().stream().map(TrabajoGradoDTOResponse::cancelacion).toList(),
                trabajo.getHistorial().stream().map(TrabajoGradoDTOResponse::cambio).toList());
    }

    private static EstudianteDTO estudiante(Estudiante e) {
        return new EstudianteDTO(e.getUsuarioId(), e.getCodigo(), e.getNombreCompleto(), e.getEmail(),
                CatalogoDTOResponse.desde(e.getPrograma()));
    }

    private static DirectorDTO director(Director d) {
        if (d == null)
            return null;
        return new DirectorDTO(d.getUsuarioId(), d.getCodigo(), d.getNombreCompleto(), d.getEmail(),
                d.getTipoVinculacion(), CatalogoDTOResponse.desde(d.getDepartamento()));
    }

    private static AsesorDTO asesor(Asesor a) {
        if (a == null)
            return null;
        return new AsesorDTO(a.getUsuarioId(), a.getCodigo(), a.getNombreCompleto(), a.getEmail(),
                a.getOrganizacion(), a.isExterno());
    }

    private static VersionPropuestaDTO versionPropuesta(VersionPropuesta v, Modalidad modalidad) {
        Documento d = v.documento();
        return new VersionPropuestaDTO(d.tipo().name(), d.tipo().formato(modalidad).orElse(null), d.ruta(),
                d.version(), d.fechaCarga(), v.estado());
    }

    private static DocumentoDTO documento(Documento d, Modalidad modalidad) {
        if (d == null)
            return null;
        return new DocumentoDTO(d.tipo().name(), d.tipo().formato(modalidad).orElse(null), d.ruta(), d.version(),
                d.fechaCarga());
    }

    private static DecisionDTO decision(Decision d) {
        return new DecisionDTO(d.organo(), d.resultado(), d.acta(), d.rutaArchivo(), d.observaciones(),
                d.fecha());
    }

    private static AnteproyectoDTO anteproyecto(Anteproyecto a, Modalidad modalidad) {
        return new AnteproyectoDTO(a.getEstado(), a.getFechaLimiteEvaluacion(), a.getFechaLimiteCorrecciones(),
                a.getVersiones().stream().map(v -> documento(v, modalidad)).toList(),
                a.getEvaluadores().stream().map(TrabajoGradoDTOResponse::evaluador).toList(),
                a.getConceptos().stream().map(c -> concepto(c, modalidad)).toList(),
                a.getDecisionesConsejo().stream().map(TrabajoGradoDTOResponse::decision).toList());
    }

    private static EjecucionDTO ejecucion(Ejecucion e) {
        if (e == null)
            return null;
        return new EjecucionDTO(e.getFechaNotificacion(), e.getFechaFin(), e.getFechaMinimaSustentacion(),
                e.fechaLimiteCambioObjetivos(), e.fechaLimiteSolicitudProrroga(),
                e.getCambiosObjetivos().stream().map(TrabajoGradoDTOResponse::cambioObjetivos).toList(),
                e.getInformesMensuales().stream()
                        .map(i -> new InformeMensualDTO(i.numero(), i.rutaArchivo(), i.fecha())).toList(),
                prorroga(e.getProrroga()),
                e.getSuspensiones().stream().map(TrabajoGradoDTOResponse::suspension).toList());
    }

    private static CancelacionDTO cancelacion(Cancelacion c) {
        return new CancelacionDTO(c.rutaSolicitud(), c.fechaSolicitud(), c.etapaAlSolicitar(),
                c.decision() == null ? null : decision(c.decision()));
    }

    private static SustentacionDTO sustentacion(Sustentacion s) {
        if (s == null)
            return null;
        return new SustentacionDTO(s.getEstado(), s.getRutaDocumentoFinal(), s.getRutaPazYSalvo(),
                s.getRutaConceptoAsesor(), s.getFechaSolicitud(),
                s.getSesiones().stream().map(TrabajoGradoDTOResponse::sesion).toList(),
                s.getCalificaciones().stream().map(TrabajoGradoDTOResponse::calificacion).toList(),
                s.getInasistencias().stream().map(TrabajoGradoDTOResponse::registro).toList(),
                s.getVerificaciones().stream().map(TrabajoGradoDTOResponse::registro).toList(),
                s.getRutaAjustes(), s.getFechaAjustes(), s.getFechaLimiteAjustes(), s.getFechaLimiteSegundaSesion(),
                s.getRutaVersionDefinitiva(), s.getFechaVersionDefinitiva(), s.getMotivoNoAprobacion());
    }

    private static SesionSustentacionDTO sesion(SesionSustentacion s) {
        return new SesionSustentacionDTO(s.numero(), s.jurados().stream().map(TrabajoGradoDTOResponse::evaluador)
                .toList(), s.fechaHora(), s.salon(), s.fechaProgramacion());
    }

    private static CalificacionJuradoDTO calificacion(CalificacionJurado c) {
        return new CalificacionJuradoDTO(c.sesion(), c.jurado().getUsuarioId(), c.jurado().getNombreCompleto(),
                c.calificacion(), c.observaciones(), c.rutaActa(), c.fecha());
    }

    private static RegistroJuradoDTO registro(RegistroJurado r) {
        return new RegistroJuradoDTO(r.sesion(), r.jurado().getUsuarioId(), r.jurado().getNombreCompleto(),
                r.favorable(), r.observaciones(), r.fecha());
    }

    private static SuspensionDTO suspension(Suspension s) {
        return new SuspensionDTO(s.rutaSolicitud(), s.fechaSolicitud(),
                s.decision() == null ? null : decision(s.decision()), s.fechaInicio(), s.etapaPrevia(),
                s.diasRestantes(), s.fechaMaximaReanudacion(), s.fechaReanudacion(), s.rutaReanudacion(),
                s.fueReanudadaAutomaticamente());
    }

    private static ProrrogaDTO prorroga(Prorroga p) {
        if (p == null)
            return null;
        return new ProrrogaDTO(p.meses(), p.rutaSolicitud(), p.fechaSolicitud(),
                p.decision() == null ? null : decision(p.decision()));
    }

    private static CambioObjetivosDTO cambioObjetivos(CambioObjetivos c) {
        return new CambioObjetivosDTO(c.tituloAnterior(), c.objetivosAnteriores(), c.tituloPropuesto(),
                c.objetivosPropuestos(), c.justificacion(), c.fechaSolicitud(),
                c.decision() == null ? null : decision(c.decision()));
    }

    private static EvaluadorDTO evaluador(Evaluador e) {
        return new EvaluadorDTO(e.getUsuarioId(), e.getCodigo(), e.getNombreCompleto(), e.getEmail());
    }

    private static ConceptoEvaluacionDTO concepto(ConceptoEvaluacion c, Modalidad modalidad) {
        return new ConceptoEvaluacionDTO(c.version(), c.concepto().emisor().getUsuarioId(),
                c.concepto().emisor().getNombreCompleto(), c.concepto().resultado(), c.concepto().observaciones(),
                c.concepto().fecha(), documento(c.formato(), modalidad));
    }

    private static CambioEtapaDTO cambio(CambioEtapa c) {
        return new CambioEtapaDTO(c.origen(), c.destino(), c.fecha(), c.responsable(), c.observacion());
    }
}
