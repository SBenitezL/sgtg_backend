package com.sgtg.backend.infraestructure.mapper;

import java.util.List;

import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.trabajo_grado.Anteproyecto;
import com.sgtg.backend.domain.models.trabajo_grado.CalificacionJurado;
import com.sgtg.backend.domain.models.trabajo_grado.CambioEtapa;
import com.sgtg.backend.domain.models.trabajo_grado.Cancelacion;
import com.sgtg.backend.domain.models.trabajo_grado.CambioObjetivos;
import com.sgtg.backend.domain.models.trabajo_grado.ConceptoEvaluacion;
import com.sgtg.backend.domain.models.trabajo_grado.DatosAnteproyecto;
import com.sgtg.backend.domain.models.trabajo_grado.DatosEjecucion;
import com.sgtg.backend.domain.models.trabajo_grado.DatosSustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.Ejecucion;
import com.sgtg.backend.domain.models.trabajo_grado.DatosPropuesta;
import com.sgtg.backend.domain.models.trabajo_grado.DatosTrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.InformeMensual;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Prorroga;
import com.sgtg.backend.domain.models.trabajo_grado.RegistroJurado;
import com.sgtg.backend.domain.models.trabajo_grado.SesionSustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.Sustentacion;
import com.sgtg.backend.domain.models.trabajo_grado.Suspension;
import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;
import com.sgtg.backend.domain.models.trabajo_grado.Propuesta;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.AsesorEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.CalificacionJuradoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.CambioEtapaEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.CancelacionEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.CambioObjetivosEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.ConceptoEvaluacionEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DecisionEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DirectorTrabajoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DirectorTrabajoEmbeddable.RolDireccion;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DocumentoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.EstudianteTrabajoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.EvaluadorTrabajoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.InformeMensualEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.ProrrogaEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.RegistroJuradoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.SesionSustentacionEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.SuspensionEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.TrabajoGradoEntity;

/**
 * Conversión entre el agregado {@link TrabajoGrado} y su entidad. La lectura
 * usa {@link TrabajoGrado#reconstruir}, que no vuelve a aplicar las reglas de
 * creación.
 */
public abstract class MapperTrabajoGrado {

    // ----- Dominio → entidad -----

    public static TrabajoGradoEntity toEntity(TrabajoGrado trabajo) {
        TrabajoGradoEntity entity = new TrabajoGradoEntity();
        entity.setId(trabajo.getId());
        entity.setModalidad(trabajo.getModalidad());
        entity.setTitulo(trabajo.getTitulo());
        entity.setDescripcion(trabajo.getDescripcion());
        entity.setLineaInvestigacion(trabajo.getLineaInvestigacion());
        entity.setEtapa(trabajo.getEtapa());
        entity.setMotivoCierre(trabajo.getMotivoCierre());
        entity.getObjetivos().addAll(trabajo.getObjetivos());
        trabajo.getEstudiantes().forEach(e -> entity.getEstudiantes().add(toEmbeddable(e)));
        entity.getDireccion().add(toEmbeddable(trabajo.getDirector(), RolDireccion.DIRECTOR));
        if (trabajo.getCodirector() != null)
            entity.getDireccion().add(toEmbeddable(trabajo.getCodirector(), RolDireccion.CODIRECTOR));
        entity.setAsesor(toEmbeddable(trabajo.getAsesor()));
        if (trabajo instanceof PracticaProfesional practica)
            entity.setCartaAceptacion(toEmbeddable(practica.getCartaAceptacion()));

        Propuesta propuesta = trabajo.getPropuesta();
        entity.setPropuestaEstado(propuesta.getEstado());
        entity.setPropuestaFechaLimiteConcepto(propuesta.getFechaLimiteConcepto());
        propuesta.getVersiones().forEach(v -> entity.getPropuestaVersiones().add(toEmbeddable(v)));
        propuesta.getDecisiones().forEach(d -> entity.getPropuestaDecisiones().add(toEmbeddable(d)));

        Anteproyecto anteproyecto = trabajo.getAnteproyecto();
        entity.setAnteproyectoEstado(anteproyecto.getEstado());
        entity.setAnteproyectoFechaLimiteEvaluacion(anteproyecto.getFechaLimiteEvaluacion());
        entity.setAnteproyectoFechaLimiteCorrecciones(anteproyecto.getFechaLimiteCorrecciones());
        anteproyecto.getVersiones().forEach(v -> entity.getAnteproyectoVersiones().add(toEmbeddable(v)));
        anteproyecto.getEvaluadores().forEach(e -> entity.getAnteproyectoEvaluadores().add(toEmbeddable(e)));
        anteproyecto.getConceptos().forEach(c -> entity.getAnteproyectoConceptos().add(toEmbeddable(c)));
        anteproyecto.getDecisionesConsejo()
                .forEach(d -> entity.getAnteproyectoDecisionesConsejo().add(toEmbeddable(d)));

        Ejecucion ejecucion = trabajo.getEjecucion();
        if (ejecucion != null) {
            entity.setEjecucionFechaNotificacion(ejecucion.getFechaNotificacion());
            entity.setEjecucionFechaFin(ejecucion.getFechaFin());
            entity.setEjecucionFechaMinimaSustentacion(ejecucion.getFechaMinimaSustentacion());
            ejecucion.getCambiosObjetivos().forEach(c -> entity.getEjecucionCambiosObjetivos().add(toEmbeddable(c)));
            ejecucion.getInformesMensuales()
                    .forEach(i -> entity.getEjecucionInformesMensuales().add(toEmbeddable(i)));
            entity.setProrroga(toEmbeddable(ejecucion.getProrroga()));
            ejecucion.getSuspensiones().forEach(s -> entity.getEjecucionSuspensiones().add(toEmbeddable(s)));
        }

        if (trabajo.getSustentacion() != null)
            copiarSustentacion(trabajo.getSustentacion(), entity);

        trabajo.getCancelaciones().forEach(c -> entity.getCancelaciones().add(toEmbeddable(c)));
        trabajo.getHistorial().forEach(c -> entity.getHistorial().add(toEmbeddable(c)));
        return entity;
    }

    private static EstudianteTrabajoEmbeddable toEmbeddable(Estudiante e) {
        NombrePersona n = e.getNombre();
        return new EstudianteTrabajoEmbeddable(e.getUsuarioId(), e.getCodigo(), n.primerNombre(), n.segundoNombre(),
                n.primerApellido(), n.segundoApellido(), e.getEmail(), e.getPrograma().id(), e.getPrograma().nombre());
    }

    private static DirectorTrabajoEmbeddable toEmbeddable(Director d, RolDireccion rol) {
        NombrePersona n = d.getNombre();
        return new DirectorTrabajoEmbeddable(rol, d.getUsuarioId(), d.getCodigo(), n.primerNombre(),
                n.segundoNombre(), n.primerApellido(), n.segundoApellido(), d.getEmail(), d.getTipoVinculacion(),
                d.getDepartamento().id(), d.getDepartamento().nombre());
    }

    private static AsesorEmbeddable toEmbeddable(Asesor a) {
        if (a == null)
            return null;
        NombrePersona n = a.getNombre();
        return new AsesorEmbeddable(a.getUsuarioId(), a.getCodigo(), n.primerNombre(), n.segundoNombre(),
                n.primerApellido(), n.segundoApellido(), a.getEmail(), a.getOrganizacion(), a.isExterno());
    }

    private static EvaluadorTrabajoEmbeddable toEmbeddable(Evaluador e) {
        NombrePersona n = e.getNombre();
        return new EvaluadorTrabajoEmbeddable(e.getUsuarioId(), e.getCodigo(), n.primerNombre(), n.segundoNombre(),
                n.primerApellido(), n.segundoApellido(), e.getEmail());
    }

    private static ConceptoEvaluacionEmbeddable toEmbeddable(ConceptoEvaluacion c) {
        Concepto concepto = c.concepto();
        return new ConceptoEvaluacionEmbeddable(c.version(), concepto.emisor().getUsuarioId(), concepto.resultado(),
                concepto.observaciones(), concepto.fecha(), c.formato() == null ? null : c.formato().ruta());
    }

    private static CambioObjetivosEmbeddable toEmbeddable(CambioObjetivos c) {
        return new CambioObjetivosEmbeddable(c.tituloAnterior(), c.objetivosAnteriores(), c.tituloPropuesto(),
                c.objetivosPropuestos(), c.justificacion(), c.fechaSolicitud(),
                c.decision() == null ? null : toEmbeddable(c.decision()));
    }

    private static ProrrogaEmbeddable toEmbeddable(Prorroga p) {
        if (p == null)
            return null;
        return new ProrrogaEmbeddable(p.meses(), p.rutaSolicitud(), p.fechaSolicitud(),
                p.decision() == null ? null : toEmbeddable(p.decision()));
    }

    private static void copiarSustentacion(Sustentacion s, TrabajoGradoEntity entity) {
        entity.setSustentacionRutaDocumentoFinal(s.getRutaDocumentoFinal());
        entity.setSustentacionRutaPazYSalvo(s.getRutaPazYSalvo());
        entity.setSustentacionRutaConceptoAsesor(s.getRutaConceptoAsesor());
        entity.setSustentacionFechaSolicitud(s.getFechaSolicitud());
        entity.setSustentacionEstado(s.getEstado());
        entity.setSustentacionRutaAjustes(s.getRutaAjustes());
        entity.setSustentacionFechaAjustes(s.getFechaAjustes());
        entity.setSustentacionFechaLimiteAjustes(s.getFechaLimiteAjustes());
        entity.setSustentacionFechaLimiteSegundaSesion(s.getFechaLimiteSegundaSesion());
        entity.setSustentacionRutaVersionDefinitiva(s.getRutaVersionDefinitiva());
        entity.setSustentacionFechaVersionDefinitiva(s.getFechaVersionDefinitiva());
        entity.setSustentacionJuradoVersionDefinitivaId(
                s.getJuradoVersionDefinitiva() == null ? null : s.getJuradoVersionDefinitiva().getUsuarioId());
        entity.setSustentacionMotivoNoAprobacion(s.getMotivoNoAprobacion());
        s.getSesiones().forEach(sesion -> entity.getSustentacionSesiones().add(new SesionSustentacionEmbeddable(
                sesion.numero(), sesion.fechaHora(), sesion.salon(), sesion.fechaProgramacion(),
                toEmbeddable(sesion.jurados().get(0)), toEmbeddable(sesion.jurados().get(1)))));
        s.getCalificaciones().forEach(c -> entity.getSustentacionCalificaciones()
                .add(new CalificacionJuradoEmbeddable(c.sesion(), c.jurado().getUsuarioId(), c.calificacion(),
                        c.observaciones(), c.rutaActa(), c.fecha())));
        s.getInasistencias().forEach(r -> entity.getSustentacionInasistencias().add(toEmbeddable(r)));
        s.getVerificaciones().forEach(r -> entity.getSustentacionVerificaciones().add(toEmbeddable(r)));
    }

    private static CancelacionEmbeddable toEmbeddable(Cancelacion c) {
        return new CancelacionEmbeddable(c.rutaSolicitud(), c.fechaSolicitud(), c.etapaAlSolicitar(),
                c.decision() == null ? null : toEmbeddable(c.decision()));
    }

    private static Cancelacion toDomain(CancelacionEmbeddable c) {
        DecisionEmbeddable decision = c.getDecision();
        return new Cancelacion(c.getRutaSolicitud(), c.getFechaSolicitud(), c.getEtapaAlSolicitar(),
                decision == null || decision.getOrgano() == null ? null : toDomain(decision));
    }

    private static RegistroJuradoEmbeddable toEmbeddable(RegistroJurado r) {
        return new RegistroJuradoEmbeddable(r.sesion(), r.jurado().getUsuarioId(), r.favorable(), r.observaciones(),
                r.fecha());
    }

    private static SuspensionEmbeddable toEmbeddable(Suspension s) {
        return new SuspensionEmbeddable(s.rutaSolicitud(), s.fechaSolicitud(),
                s.decision() == null ? null : toEmbeddable(s.decision()), s.etapaPrevia(), s.diasRestantes(),
                s.fechaMaximaReanudacion(), s.fechaReanudacion(), s.rutaReanudacion());
    }

    private static InformeMensualEmbeddable toEmbeddable(InformeMensual i) {
        return new InformeMensualEmbeddable(i.numero(), i.rutaArchivo(), i.fecha());
    }

    private static DocumentoEmbeddable toEmbeddable(Documento d) {
        if (d == null)
            return null;
        return new DocumentoEmbeddable(d.tipo(), d.ruta(), d.version(), d.fechaCarga());
    }

    private static DecisionEmbeddable toEmbeddable(Decision d) {
        return new DecisionEmbeddable(d.organo(), d.resultado(), d.acta(), d.rutaArchivo(), d.observaciones(),
                d.fecha());
    }

    private static CambioEtapaEmbeddable toEmbeddable(CambioEtapa c) {
        return new CambioEtapaEmbeddable(c.origen(), c.destino(), c.fecha(), c.responsable(), c.observacion());
    }

    // ----- Entidad → dominio -----

    public static TrabajoGrado toDomain(TrabajoGradoEntity entity) {
        if (entity == null)
            return null;
        Director director = buscarDireccion(entity.getDireccion(), RolDireccion.DIRECTOR);
        Director codirector = buscarDireccion(entity.getDireccion(), RolDireccion.CODIRECTOR);
        DatosPropuesta propuesta = new DatosPropuesta(
                entity.getPropuestaVersiones().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getPropuestaDecisiones().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getPropuestaEstado(), entity.getPropuestaFechaLimiteConcepto());
        return TrabajoGrado.reconstruir(new DatosTrabajoGrado(
                entity.getId(), entity.getModalidad(), entity.getTitulo(), List.copyOf(entity.getObjetivos()),
                entity.getEstudiantes().stream().map(MapperTrabajoGrado::toDomain).toList(),
                director, codirector, toDomain(entity.getAsesor()), toDomain(entity.getCartaAceptacion()),
                entity.getEtapa(), propuesta, toDatosAnteproyecto(entity), toDatosEjecucion(entity),
                toDatosSustentacion(entity),
                entity.getCancelaciones().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getHistorial().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getMotivoCierre(), entity.getDescripcion(), entity.getLineaInvestigacion()));
    }

    private static DatosAnteproyecto toDatosAnteproyecto(TrabajoGradoEntity entity) {
        List<Evaluador> evaluadores = entity.getAnteproyectoEvaluadores().stream().map(MapperTrabajoGrado::toDomain)
                .toList();
        return new DatosAnteproyecto(
                entity.getAnteproyectoVersiones().stream().map(MapperTrabajoGrado::toDomain).toList(),
                evaluadores,
                entity.getAnteproyectoConceptos().stream().map(c -> toDomain(c, evaluadores)).toList(),
                entity.getAnteproyectoDecisionesConsejo().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getAnteproyectoEstado(), entity.getAnteproyectoFechaLimiteEvaluacion(),
                entity.getAnteproyectoFechaLimiteCorrecciones());
    }

    private static DatosEjecucion toDatosEjecucion(TrabajoGradoEntity entity) {
        if (entity.getEjecucionFechaNotificacion() == null)
            return null;
        return new DatosEjecucion(entity.getEjecucionFechaNotificacion(), entity.getEjecucionFechaFin(),
                entity.getEjecucionFechaMinimaSustentacion(),
                entity.getEjecucionCambiosObjetivos().stream().map(MapperTrabajoGrado::toDomain).toList(),
                entity.getEjecucionInformesMensuales().stream().map(MapperTrabajoGrado::toDomain).toList(),
                toDomain(entity.getProrroga()),
                entity.getEjecucionSuspensiones().stream().map(MapperTrabajoGrado::toDomain).toList());
    }

    private static Suspension toDomain(SuspensionEmbeddable s) {
        DecisionEmbeddable decision = s.getDecision();
        return new Suspension(s.getRutaSolicitud(), s.getFechaSolicitud(),
                decision == null || decision.getOrgano() == null ? null : toDomain(decision), s.getEtapaPrevia(),
                s.getDiasRestantes(), s.getFechaMaximaReanudacion(), s.getFechaReanudacion(),
                s.getRutaReanudacion());
    }

    private static Prorroga toDomain(ProrrogaEmbeddable p) {
        if (p == null || p.getMeses() == null)
            return null;
        DecisionEmbeddable decision = p.getDecision();
        return new Prorroga(p.getMeses(), p.getRutaSolicitud(), p.getFechaSolicitud(),
                decision == null || decision.getOrgano() == null ? null : toDomain(decision));
    }

    private static CambioObjetivos toDomain(CambioObjetivosEmbeddable c) {
        DecisionEmbeddable decision = c.getDecision();
        return new CambioObjetivos(c.getTituloAnterior(), c.getObjetivosAnteriores(), c.getTituloPropuesto(),
                c.getObjetivosPropuestos(), c.getJustificacion(), c.getFechaSolicitud(),
                decision == null || decision.getOrgano() == null ? null : toDomain(decision));
    }

    private static InformeMensual toDomain(InformeMensualEmbeddable i) {
        return new InformeMensual(i.getNumero(), i.getRutaArchivo(), i.getFecha());
    }

    private static DatosSustentacion toDatosSustentacion(TrabajoGradoEntity entity) {
        if (entity.getSustentacionEstado() == null)
            return null;
        List<SesionSustentacion> sesiones = entity.getSustentacionSesiones().stream()
                .map(s -> new SesionSustentacion(s.getNumero(), List.of(toDomain(s.getJurado1()), toDomain(s.getJurado2())),
                        s.getFechaHora(), s.getSalon(), s.getFechaProgramacion()))
                .toList();
        Long juradoDefinitiva = entity.getSustentacionJuradoVersionDefinitivaId();
        return new DatosSustentacion(entity.getSustentacionRutaDocumentoFinal(),
                entity.getSustentacionRutaPazYSalvo(), entity.getSustentacionRutaConceptoAsesor(),
                entity.getSustentacionFechaSolicitud(), entity.getSustentacionEstado(), sesiones,
                entity.getSustentacionCalificaciones().stream()
                        .map(c -> new CalificacionJurado(c.getSesion(), jurado(sesiones, c.getSesion(),
                                c.getJuradoUsuarioId()), c.getCalificacion(), c.getObservaciones(), c.getRutaActa(),
                                c.getFecha()))
                        .toList(),
                entity.getSustentacionInasistencias().stream().map(r -> toDomain(r, sesiones)).toList(),
                entity.getSustentacionVerificaciones().stream().map(r -> toDomain(r, sesiones)).toList(),
                entity.getSustentacionRutaAjustes(), entity.getSustentacionFechaAjustes(),
                entity.getSustentacionFechaLimiteAjustes(), entity.getSustentacionFechaLimiteSegundaSesion(),
                entity.getSustentacionRutaVersionDefinitiva(), entity.getSustentacionFechaVersionDefinitiva(),
                juradoDefinitiva == null ? null : jurado(sesiones, sesiones.size(), juradoDefinitiva),
                entity.getSustentacionMotivoNoAprobacion());
    }

    private static RegistroJurado toDomain(RegistroJuradoEmbeddable r, List<SesionSustentacion> sesiones) {
        return new RegistroJurado(r.getSesion(), jurado(sesiones, r.getSesion(), r.getJuradoUsuarioId()),
                Boolean.TRUE.equals(r.getFavorable()), r.getObservaciones(), r.getFecha());
    }

    /** Jurado de la sesión indicada que corresponde al usuario. */
    private static Evaluador jurado(List<SesionSustentacion> sesiones, int numero, Long usuarioId) {
        return sesiones.stream().filter(s -> s.numero() == numero).flatMap(s -> s.jurados().stream())
                .filter(j -> j.getUsuarioId().equals(usuarioId)).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Registro de un jurado no asignado a la sesión " + numero + ": usuario " + usuarioId));
    }

    private static Evaluador toDomain(EvaluadorTrabajoEmbeddable e) {
        return new Evaluador(e.getUsuarioId(), e.getCodigo(),
                new NombrePersona(e.getPrimerNombre(), e.getSegundoNombre(), e.getPrimerApellido(),
                        e.getSegundoApellido()),
                e.getEmail());
    }

    /** El emisor del concepto es uno de los evaluadores asignados, identificado por su usuario. */
    private static ConceptoEvaluacion toDomain(ConceptoEvaluacionEmbeddable c, List<Evaluador> evaluadores) {
        Evaluador emisor = evaluadores.stream().filter(e -> e.getUsuarioId().equals(c.getEvaluadorUsuarioId()))
                .findFirst().orElseThrow(() -> new IllegalStateException(
                        "Concepto de un evaluador no asignado: usuario " + c.getEvaluadorUsuarioId()));
        Documento formato = c.getFormatoUrl() == null ? null
                : new Documento(TipoDocumento.EVALUACION_ANTEPROYECTO, c.getFormatoUrl(), c.getVersion(), c.getFecha());
        return new ConceptoEvaluacion(c.getVersion(),
                new Concepto(emisor, c.getResultado(), c.getObservaciones(), c.getFecha()), formato);
    }

    private static Director buscarDireccion(List<DirectorTrabajoEmbeddable> direccion, RolDireccion rol) {
        return direccion.stream()
                .filter(d -> d.getRol() == rol)
                .findFirst()
                .map(d -> new Director(d.getUsuarioId(), d.getCodigo(),
                        new NombrePersona(d.getPrimerNombre(), d.getSegundoNombre(), d.getPrimerApellido(),
                                d.getSegundoApellido()),
                        d.getEmail(), d.getTipoVinculacion(),
                        new Departamento(d.getDepartamentoId(), d.getDepartamentoNombre())))
                .orElse(null);
    }

    private static Estudiante toDomain(EstudianteTrabajoEmbeddable e) {
        return new Estudiante(e.getUsuarioId(), e.getCodigo(),
                new NombrePersona(e.getPrimerNombre(), e.getSegundoNombre(), e.getPrimerApellido(),
                        e.getSegundoApellido()),
                e.getEmail(), new Programa(e.getProgramaId(), e.getProgramaNombre()));
    }

    private static Asesor toDomain(AsesorEmbeddable a) {
        if (a == null || a.getCodigo() == null)
            return null;
        return new Asesor(a.getUsuarioId(), a.getCodigo(),
                new NombrePersona(a.getPrimerNombre(), a.getSegundoNombre(), a.getPrimerApellido(),
                        a.getSegundoApellido()),
                a.getEmail(), a.getOrganizacion(), Boolean.TRUE.equals(a.getExterno()));
    }

    private static Documento toDomain(DocumentoEmbeddable d) {
        if (d == null || d.getTipo() == null)
            return null;
        return new Documento(d.getTipo(), d.getUrl(), d.getVersion(), d.getFechaCarga());
    }

    private static Decision toDomain(DecisionEmbeddable d) {
        return new Decision(d.getOrgano(), d.getResultado(), d.getActa(), d.getRutaArchivo(), d.getObservaciones(),
                d.getFecha());
    }

    private static CambioEtapa toDomain(CambioEtapaEmbeddable c) {
        return new CambioEtapa(c.getOrigen(), c.getDestino(), c.getFecha(), c.getResponsable(), c.getObservacion());
    }
}
