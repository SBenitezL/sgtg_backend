package com.sgtg.backend.domain.use_cases;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sgtg.backend.application.input.trabajo_grado.DatosAsesor;
import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;
import com.sgtg.backend.application.input.trabajo_grado.NuevoTrabajoGrado;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageTrabajoGradoGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.comun.Concepto;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Evaluador;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoInvestigacion;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Casos de uso del trabajo de grado (Fases 1 y 2). Carga los participantes con
 * sus perfiles, delega las reglas al agregado y guarda el resultado.
 */
public class ManageTrabajoGradoCUImplAdapter implements ManageTrabajoGradoCUIntPort {

    /** Roles que pueden consultar cualquier trabajo de grado sin participar en él */
    private static final Set<String> ROLES_CONSULTA_GENERAL = Set.of(RoleAdmited.COMITE.name(),
            RoleAdmited.JEFE_DEP.name(), RoleAdmited.CONSEJO.name(), RoleAdmited.ADMINISTRADOR.name());

    private final ManageTrabajoGradoGatewayIntPort trabajoGateway;
    private final ManageUserGatewayIntPort userGateway;
    private final ManagePerfilGatewayIntPort perfilGateway;
    private final CalendarioHabil calendario;
    private final Clock clock;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageTrabajoGradoCUImplAdapter(ManageTrabajoGradoGatewayIntPort trabajoGateway,
            ManageUserGatewayIntPort userGateway, ManagePerfilGatewayIntPort perfilGateway,
            CalendarioHabil calendario, Clock clock, ExceptionFormatterIntPort exceptionFormatter) {
        this.trabajoGateway = trabajoGateway;
        this.userGateway = userGateway;
        this.perfilGateway = perfilGateway;
        this.calendario = calendario;
        this.clock = clock;
        this.exceptionFormatter = exceptionFormatter;
    }

    @Override
    public TrabajoGrado crear(Long directorUsuarioId, NuevoTrabajoGrado datos) {
        if (datos == null || datos.modalidad() == null)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.trabajo_grado.modalidad_obligatoria");
        Director director = cargarDirector(directorUsuarioId, "error.trabajo_grado.director_sin_perfil");
        Director codirector = datos.codirectorId() == null ? null
                : cargarDirector(datos.codirectorId(), "error.trabajo_grado.codirector_sin_perfil");
        List<Estudiante> estudiantes = cargarEstudiantes(datos.estudiantesIds());
        Asesor asesor = crearAsesor(datos.asesor());

        TrabajoGrado trabajo;
        if (datos.modalidad() == Modalidad.TRABAJO_DE_INVESTIGACION) {
            trabajo = new TrabajoInvestigacion(null, datos.titulo(), datos.objetivos(), estudiantes, director,
                    codirector, asesor);
        } else {
            if (estudiantes.size() > 1)
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.trabajo_grado.practica_individual");
            Documento carta = CheckString.isBlank(datos.urlCartaAceptacion()) ? null
                    : Documento.inicial(TipoDocumento.CARTA_ACEPTACION, datos.urlCartaAceptacion(), hoy());
            trabajo = new PracticaProfesional(null, datos.titulo(), datos.objetivos(),
                    estudiantes.isEmpty() ? null : estudiantes.get(0), director, codirector, asesor, carta);
        }
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado consultar(Long trabajoId, Long usuarioId) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        if (!trabajo.participa(usuarioId) && !tieneRolDeConsultaGeneral(usuarioId))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.sin_acceso");
        return trabajo;
    }

    @Override
    public List<TrabajoGrado> listarMisTrabajos(Long usuarioId) {
        return this.trabajoGateway.listarPorParticipante(usuarioId);
    }

    @Override
    public List<TrabajoGrado> listarPorEtapa(Proceso etapa) {
        if (etapa == null)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.trabajo_grado.etapa_obligatoria");
        return this.trabajoGateway.listarPorEtapa(etapa);
    }

    @Override
    public TrabajoGrado radicarPropuesta(Long trabajoId, Long usuarioId, String url) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        if (!trabajo.esDirector(usuarioId))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_director");
        trabajo.radicarPropuesta(url, hoy(), this.calendario);
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado registrarDecisionComite(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String acta, String rutaArchivo, String observaciones, LocalDate fecha) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario responsable = cargarUsuario(usuarioId);
        Decision decision = new Decision(Organo.COMITE_PROGRAMA, resultado, acta, rutaArchivo, observaciones,
                fecha == null ? hoy() : fecha);
        trabajo.registrarDecisionComite(decision, responsable.getCodigo(), LocalDateTime.now(this.clock));
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado radicarAnteproyecto(Long trabajoId, Long usuarioId, String url) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        if (!trabajo.esDirector(usuarioId))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_director");
        trabajo.radicarAnteproyecto(url, trabajo.getDirector().getCodigo(), LocalDateTime.now(this.clock),
                this.calendario);
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado asignarEvaluadores(Long trabajoId, Long usuarioId, Long evaluador1Id, Long evaluador2Id) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario jefe = cargarUsuario(usuarioId);
        // El Departamento al que está adscrito el director es el que asigna (Art. 13)
        PerfilDocente perfilJefe = this.perfilGateway.buscarPerfilDocente(usuarioId);
        if (perfilJefe == null)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.trabajo_grado.jefe_sin_perfil");
        if (!perfilJefe.getDepartamento().esMismo(trabajo.getDirector().getDepartamento()))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.otro_departamento");
        Evaluador evaluador1 = evaluador1Id == null ? null : new Evaluador(cargarUsuario(evaluador1Id));
        Evaluador evaluador2 = evaluador2Id == null ? null : new Evaluador(cargarUsuario(evaluador2Id));
        trabajo.asignarEvaluadores(evaluador1, evaluador2, jefe.getCodigo(), LocalDateTime.now(this.clock),
                this.calendario);
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado registrarConceptoEvaluador(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String observaciones, String urlFormato) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Optional<Evaluador> evaluador = trabajo.getAnteproyecto().evaluador(usuarioId);
        if (evaluador.isEmpty())
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_evaluador");
        Concepto concepto = new Concepto(evaluador.get(), resultado, observaciones, hoy());
        trabajo.registrarConceptoEvaluador(concepto, urlFormato, LocalDateTime.now(this.clock), this.calendario);
        return this.trabajoGateway.guardar(trabajo);
    }

    // ----- Apoyo -----

    private LocalDate hoy() {
        return LocalDate.now(this.clock);
    }

    private TrabajoGrado cargarTrabajo(Long trabajoId) {
        TrabajoGrado trabajo = this.trabajoGateway.buscarPorId(trabajoId);
        if (trabajo == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.trabajo_grado.no_encontrado");
        return trabajo;
    }

    private Usuario cargarUsuario(Long usuarioId) {
        Usuario usuario = this.userGateway.findById(usuarioId);
        if (usuario == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.usuario.no_encontrado");
        return usuario;
    }

    private Director cargarDirector(Long usuarioId, String claveSinPerfil) {
        Usuario usuario = cargarUsuario(usuarioId);
        PerfilDocente perfil = this.perfilGateway.buscarPerfilDocente(usuarioId);
        if (perfil == null)
            this.exceptionFormatter.returnResponseBusinessRuleViolated(claveSinPerfil);
        return new Director(usuario, perfil);
    }

    private List<Estudiante> cargarEstudiantes(List<Long> estudiantesIds) {
        List<Estudiante> estudiantes = new ArrayList<>();
        if (estudiantesIds == null)
            return estudiantes;
        for (Long estudianteId : estudiantesIds) {
            Usuario usuario = cargarUsuario(estudianteId);
            PerfilEstudiante perfil = this.perfilGateway.buscarPerfilEstudiante(estudianteId);
            if (perfil == null)
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.trabajo_grado.estudiante_sin_perfil");
            estudiantes.add(new Estudiante(usuario, perfil));
        }
        return estudiantes;
    }

    private Asesor crearAsesor(DatosAsesor datos) {
        if (datos == null)
            return null;
        if (datos.usuarioId() != null)
            return new Asesor(cargarUsuario(datos.usuarioId()), datos.organizacion(), datos.externo());
        return new Asesor(null, datos.codigo(), new NombrePersona(datos.primerNombre(), datos.segundoNombre(),
                datos.primerApellido(), datos.segundoApellido()), datos.email(), datos.organizacion(),
                datos.externo());
    }

    private boolean tieneRolDeConsultaGeneral(Long usuarioId) {
        Usuario usuario = this.userGateway.findById(usuarioId);
        return usuario != null
                && usuario.getRole().stream().anyMatch(role -> ROLES_CONSULTA_GENERAL.contains(role.getTrole()));
    }
}
