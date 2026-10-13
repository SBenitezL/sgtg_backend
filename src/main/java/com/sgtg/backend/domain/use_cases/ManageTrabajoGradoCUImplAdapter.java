package com.sgtg.backend.domain.use_cases;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;
import com.sgtg.backend.application.input.trabajo_grado.DatosAsesor;
import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;
import com.sgtg.backend.application.input.trabajo_grado.NuevoTrabajoGrado;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageArchivoGatewayIntPort;
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
import com.sgtg.backend.domain.models.trabajo_grado.Calificacion;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.Modalidad;
import com.sgtg.backend.domain.models.trabajo_grado.RevisionInforme;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoInvestigacion;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Casos de uso del trabajo de grado (Fases 1 a 8). Carga los participantes con
 * sus perfiles, almacena los archivos recibidos, delega las reglas al agregado
 * (que solo guarda la ruta de cada archivo) y guarda el resultado. Si el
 * agregado rechaza la operación, los archivos almacenados se eliminan.
 */
public class ManageTrabajoGradoCUImplAdapter implements ManageTrabajoGradoCUIntPort {

    /** Roles que pueden consultar cualquier trabajo de grado sin participar en él */
    private static final Set<String> ROLES_CONSULTA_GENERAL = Set.of(RoleAdmited.COMITE.name(),
            RoleAdmited.JEFE_DEP.name(), RoleAdmited.CONSEJO.name(), RoleAdmited.ADMINISTRADOR.name());

    /** Firma con la que inicia todo archivo PDF */
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final ManageTrabajoGradoGatewayIntPort trabajoGateway;
    private final ManageUserGatewayIntPort userGateway;
    private final ManagePerfilGatewayIntPort perfilGateway;
    private final ManageArchivoGatewayIntPort archivoGateway;
    private final CalendarioHabil calendario;
    private final Clock clock;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageTrabajoGradoCUImplAdapter(ManageTrabajoGradoGatewayIntPort trabajoGateway,
            ManageUserGatewayIntPort userGateway, ManagePerfilGatewayIntPort perfilGateway,
            ManageArchivoGatewayIntPort archivoGateway, CalendarioHabil calendario, Clock clock,
            ExceptionFormatterIntPort exceptionFormatter) {
        this.trabajoGateway = trabajoGateway;
        this.userGateway = userGateway;
        this.perfilGateway = perfilGateway;
        this.archivoGateway = archivoGateway;
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

        if (datos.modalidad() == Modalidad.TRABAJO_DE_INVESTIGACION) {
            TrabajoGrado investigacion = new TrabajoInvestigacion(null, datos.titulo(), datos.objetivos(),
                    estudiantes, director, codirector, asesor);
            investigacion.describir(datos.descripcion(), datos.lineaInvestigacion());
            return asignarRolesDeDireccion(this.trabajoGateway.guardar(investigacion));
        }

        if (estudiantes.size() > 1)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.trabajo_grado.practica_individual");
        // La carta se carga antes de que el trabajo tenga identificador: queda en una
        // ubicación temporal y se mueve a la carpeta del trabajo una vez guardado
        List<String> rutas = new ArrayList<>();
        String rutaTemporal = null;
        if (hayArchivo(datos.cartaAceptacion())) {
            validarPdf(datos.cartaAceptacion());
            rutaTemporal = this.archivoGateway.guardarTemporal(datos.cartaAceptacion());
            rutas.add(rutaTemporal);
        }
        Documento carta = rutaTemporal == null ? null
                : Documento.inicial(TipoDocumento.CARTA_ACEPTACION, rutaTemporal, hoy());
        TrabajoGrado guardado = conArchivos(rutas, () -> {
            PracticaProfesional nueva = new PracticaProfesional(null, datos.titulo(), datos.objetivos(),
                    estudiantes.isEmpty() ? null : estudiantes.get(0), director, codirector, asesor, carta);
            nueva.describir(datos.descripcion(), datos.lineaInvestigacion());
            return this.trabajoGateway.guardar(nueva);
        });
        if (!(guardado instanceof PracticaProfesional practica))
            return asignarRolesDeDireccion(guardado);
        String rutaDefinitiva = this.archivoGateway.reubicar(rutaTemporal, practica.getId(),
                TipoDocumento.CARTA_ACEPTACION.carpeta(Modalidad.PRACTICA_PROFESIONAL));
        practica.reubicarCartaAceptacion(rutaDefinitiva);
        return asignarRolesDeDireccion(this.trabajoGateway.guardar(practica));
    }

    /**
     * Los participantes ganan el rol de su papel en el trabajo (el front muestra
     * sus pantallas según los roles): DIRECTOR, CODIRECTOR y, si tiene usuario,
     * ASESOR.
     */
    private TrabajoGrado asignarRolesDeDireccion(TrabajoGrado trabajo) {
        this.userGateway.agregarRol(trabajo.getDirector().getUsuarioId(), RoleAdmited.DIRECTOR.name());
        if (trabajo.getCodirector() != null)
            this.userGateway.agregarRol(trabajo.getCodirector().getUsuarioId(), RoleAdmited.CODIRECTOR.name());
        if (trabajo.getAsesor() != null && trabajo.getAsesor().getUsuarioId() != null)
            this.userGateway.agregarRol(trabajo.getAsesor().getUsuarioId(), RoleAdmited.ASESOR.name());
        return trabajo;
    }

    @Override
    public TrabajoGrado actualizarPropuesta(Long trabajoId, Long usuarioId, String titulo, List<String> objetivos,
            String descripcion, String lineaInvestigacion) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        trabajo.actualizarPropuesta(titulo, objetivos, descripcion, lineaInvestigacion);
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado consultar(Long trabajoId, Long usuarioId) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        if (!puedeConsultar(trabajo, usuarioId))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.sin_acceso");
        return trabajo;
    }

    @Override
    public ArchivoAdjunto obtenerArchivo(Long trabajoId, Long usuarioId, String ruta) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        if (!puedeConsultar(trabajo, usuarioId))
            this.exceptionFormatter.returnNoAccess("error.archivo.sin_acceso");
        // Solo se entregan archivos de la carpeta de este trabajo
        if (!perteneceAlTrabajo(ruta, trabajoId))
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.archivo.no_encontrado");
        ArchivoAdjunto archivo = this.archivoGateway.obtener(ruta);
        if (archivo == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.archivo.no_encontrado");
        return archivo;
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
    public List<TrabajoGrado> listarTodos() {
        return this.trabajoGateway.listarTodos();
    }

    @Override
    public TrabajoGrado radicarPropuesta(Long trabajoId, Long usuarioId, ArchivoAdjunto documento) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.PROPUESTA, documento, rutas);
        return conArchivos(rutas, () -> {
            trabajo.radicarPropuesta(ruta, hoy(), this.calendario);
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado registrarDecisionComite(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String acta, ArchivoAdjunto soporte, String observaciones, LocalDate fecha) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario responsable = cargarUsuario(usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.ACTA_COMITE, soporte, rutas);
        return conArchivos(rutas, () -> {
            Decision decision = new Decision(Organo.COMITE_PROGRAMA, resultado, acta, ruta, observaciones,
                    fecha == null ? hoy() : fecha);
            trabajo.registrarDecisionComite(decision, responsable.getCodigo(), LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado radicarAnteproyecto(Long trabajoId, Long usuarioId, ArchivoAdjunto documento) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.ANTEPROYECTO, documento, rutas);
        return conArchivos(rutas, () -> {
            trabajo.radicarAnteproyecto(ruta, trabajo.getDirector().getCodigo(), LocalDateTime.now(this.clock),
                    this.calendario);
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado asignarEvaluadores(Long trabajoId, Long usuarioId, Long evaluador1Id, Long evaluador2Id) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario jefe = cargarUsuario(usuarioId);
        validarJefeDelDepartamento(trabajo, usuarioId);
        Evaluador evaluador1 = evaluador1Id == null ? null : new Evaluador(cargarUsuario(evaluador1Id));
        Evaluador evaluador2 = evaluador2Id == null ? null : new Evaluador(cargarUsuario(evaluador2Id));
        trabajo.asignarEvaluadores(evaluador1, evaluador2, jefe.getCodigo(), LocalDateTime.now(this.clock),
                this.calendario);
        TrabajoGrado guardado = this.trabajoGateway.guardar(trabajo);
        // Rol de contexto: los evaluadores acceden a "Evaluar anteproyecto"
        this.userGateway.agregarRol(evaluador1Id, RoleAdmited.EVALUADOR.name());
        this.userGateway.agregarRol(evaluador2Id, RoleAdmited.EVALUADOR.name());
        return guardado;
    }

    @Override
    public TrabajoGrado registrarConceptoEvaluador(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String observaciones, ArchivoAdjunto formato, ArchivoAdjunto anotaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Optional<Evaluador> evaluador = trabajo.getAnteproyecto().evaluador(usuarioId);
        if (evaluador.isEmpty())
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_evaluador");
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.EVALUACION_ANTEPROYECTO, formato, rutas);
        String rutaAnotaciones = almacenar(trabajo, trabajoId, TipoDocumento.ANOTACIONES_EVALUACION, anotaciones,
                rutas);
        return conArchivos(rutas, () -> {
            Concepto concepto = new Concepto(evaluador.get(), resultado, observaciones, hoy());
            trabajo.registrarConceptoEvaluador(concepto, ruta, rutaAnotaciones, LocalDateTime.now(this.clock),
                    this.calendario);
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado remitirAnteproyectoAlConsejo(Long trabajoId, Long usuarioId, ArchivoAdjunto formatoC) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario jefe = cargarUsuario(usuarioId);
        validarJefeDelDepartamento(trabajo, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.REMISION_CONSEJO, formatoC, rutas);
        return conArchivos(rutas, () -> {
            trabajo.remitirAnteproyectoAlConsejo(ruta, jefe.getCodigo(), LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    /** El Departamento al que está adscrito el director es el que asigna evaluadores y remite (Art. 13). */
    private void validarJefeDelDepartamento(TrabajoGrado trabajo, Long usuarioId) {
        PerfilDocente perfilJefe = this.perfilGateway.buscarPerfilDocente(usuarioId);
        if (perfilJefe == null)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.trabajo_grado.jefe_sin_perfil");
        if (!perfilJefe.getDepartamento().esMismo(trabajo.getDirector().getDepartamento()))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.otro_departamento");
    }

    @Override
    public TrabajoGrado registrarDecisionConsejo(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario responsable = cargarUsuario(usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.RESOLUCION, resolucion, rutas);
        return conArchivos(rutas, () -> {
            trabajo.registrarDecisionConsejo(decisionConsejo(resultado, ruta, observaciones),
                    responsable.getCodigo(), LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado solicitarCambioObjetivos(Long trabajoId, Long usuarioId, String titulo,
            List<String> objetivos, String justificacion) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        trabajo.solicitarCambioObjetivos(titulo, objetivos, justificacion, hoy());
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado resolverCambioObjetivos(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.RESOLUCION, resolucion, rutas);
        return conArchivos(rutas, () -> {
            trabajo.resolverCambioObjetivos(decisionConsejo(resultado, ruta, observaciones));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado registrarInformeMensual(Long trabajoId, Long usuarioId, String observaciones,
            ArchivoAdjunto documento) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        boolean directorDePractica = trabajo.esDirector(usuarioId)
                && trabajo.getModalidad() == Modalidad.PRACTICA_PROFESIONAL;
        if (!trabajo.esEstudiante(usuarioId) && !directorDePractica)
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_radicador_informe");
        Usuario usuario = cargarUsuario(usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.INFORME_MENSUAL, documento, rutas);
        return conArchivos(rutas, () -> {
            trabajo.registrarInformeMensual(ruta, hoy(), usuario.getCodigo(), observaciones);
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado revisarInformeMensual(Long trabajoId, int numero, Long usuarioId, String observaciones,
            ArchivoAdjunto notas) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        if (!trabajo.esRevisorDeInformes(usuarioId))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_revisor_informe");
        Usuario revisor = cargarUsuario(usuarioId);
        String nombre = String.join(" ", java.util.stream.Stream.of(revisor.getPrimerNombre(),
                revisor.getSegundoNombre(), revisor.getPrimerApellido(), revisor.getSegundoApellido())
                .filter(parte -> parte != null && !parte.isBlank()).toList());
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.REVISION_INFORME, notas, rutas);
        return conArchivos(rutas, () -> {
            trabajo.revisarInformeMensual(numero, new RevisionInforme(usuarioId, nombre, observaciones, ruta, hoy()));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado solicitarProrroga(Long trabajoId, Long usuarioId, int meses, ArchivoAdjunto solicitud) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.SOLICITUD_PRORROGA, solicitud, rutas);
        return conArchivos(rutas, () -> {
            trabajo.solicitarProrroga(meses, ruta, hoy());
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado resolverProrroga(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario responsable = cargarUsuario(usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.RESOLUCION, resolucion, rutas);
        return conArchivos(rutas, () -> {
            trabajo.resolverProrroga(decisionConsejo(resultado, ruta, observaciones), responsable.getCodigo(),
                    LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado solicitarSuspension(Long trabajoId, Long usuarioId, ArchivoAdjunto solicitud) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.SOLICITUD_SUSPENSION, solicitud, rutas);
        return conArchivos(rutas, () -> {
            trabajo.solicitarSuspension(ruta, hoy());
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado resolverSuspension(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario responsable = cargarUsuario(usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.RESOLUCION, resolucion, rutas);
        return conArchivos(rutas, () -> {
            trabajo.resolverSuspension(decisionConsejo(resultado, ruta, observaciones), responsable.getCodigo(),
                    LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado reanudar(Long trabajoId, Long usuarioId, ArchivoAdjunto documento) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.REANUDACION, documento, rutas);
        return conArchivos(rutas, () -> {
            trabajo.reanudar(ruta, trabajo.getDirector().getCodigo(), LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado solicitarSustentacion(Long trabajoId, Long usuarioId, ArchivoAdjunto documentoFinal,
            ArchivoAdjunto pazYSalvo, ArchivoAdjunto conceptoAsesor) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        // Se validan los tres archivos antes de almacenar cualquiera
        Arrays.asList(documentoFinal, pazYSalvo, conceptoAsesor).stream().filter(this::hayArchivo)
                .forEach(this::validarPdf);
        List<String> rutas = new ArrayList<>();
        String rutaFinal = almacenar(trabajo, trabajoId, TipoDocumento.DOCUMENTO_FINAL, documentoFinal, rutas);
        String rutaPazYSalvo = almacenar(trabajo, trabajoId, TipoDocumento.PAZ_Y_SALVO, pazYSalvo, rutas);
        String rutaConcepto = almacenar(trabajo, trabajoId, TipoDocumento.CONCEPTO_ASESOR, conceptoAsesor, rutas);
        return conArchivos(rutas, () -> {
            trabajo.solicitarSustentacion(rutaFinal, rutaPazYSalvo, rutaConcepto, trabajo.getDirector().getCodigo(),
                    LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado programarSustentacion(Long trabajoId, Long usuarioId, Long jurado1Id, Long jurado2Id,
            LocalDateTime fechaHora, String salon) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario responsable = cargarUsuario(usuarioId);
        Evaluador jurado1 = jurado1Id == null ? null : new Evaluador(cargarUsuario(jurado1Id));
        Evaluador jurado2 = jurado2Id == null ? null : new Evaluador(cargarUsuario(jurado2Id));
        trabajo.programarSustentacion(jurado1, jurado2, fechaHora, salon, responsable.getCodigo(),
                LocalDateTime.now(this.clock));
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado calificarSustentacion(Long trabajoId, Long usuarioId, Calificacion calificacion,
            String observaciones, ArchivoAdjunto acta) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Evaluador jurado = juradoDe(trabajo, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.ACTA_SUSTENTACION, acta, rutas);
        return conArchivos(rutas, () -> {
            trabajo.calificarSustentacion(jurado, calificacion, observaciones, ruta, LocalDateTime.now(this.clock),
                    this.calendario);
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado registrarInasistencia(Long trabajoId, Long usuarioId, String observaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        trabajo.registrarInasistencia(juradoDe(trabajo, usuarioId), observaciones, LocalDateTime.now(this.clock));
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado radicarAjustesSustentacion(Long trabajoId, Long usuarioId, ArchivoAdjunto documento) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.AJUSTES_SUSTENTACION, documento, rutas);
        return conArchivos(rutas, () -> {
            trabajo.radicarAjustesSustentacion(ruta, LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado verificarAjustesSustentacion(Long trabajoId, Long usuarioId, boolean cumple,
            String observaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        trabajo.verificarAjustesSustentacion(juradoDe(trabajo, usuarioId), cumple, observaciones,
                LocalDateTime.now(this.clock));
        return this.trabajoGateway.guardar(trabajo);
    }

    @Override
    public TrabajoGrado cargarVersionDefinitiva(Long trabajoId, Long usuarioId, ArchivoAdjunto documento) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Evaluador jurado = juradoDe(trabajo, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.VERSION_DEFINITIVA, documento, rutas);
        return conArchivos(rutas, () -> {
            trabajo.cargarVersionDefinitiva(jurado, ruta, LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado solicitarCancelacion(Long trabajoId, Long usuarioId, ArchivoAdjunto solicitud) {
        TrabajoGrado trabajo = cargarTrabajoDelDirector(trabajoId, usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.SOLICITUD_CANCELACION, solicitud, rutas);
        return conArchivos(rutas, () -> {
            trabajo.solicitarCancelacion(ruta, hoy());
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public TrabajoGrado resolverCancelacion(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        Usuario responsable = cargarUsuario(usuarioId);
        List<String> rutas = new ArrayList<>();
        String ruta = almacenar(trabajo, trabajoId, TipoDocumento.RESOLUCION, resolucion, rutas);
        return conArchivos(rutas, () -> {
            trabajo.resolverCancelacion(decisionConsejo(resultado, ruta, observaciones), responsable.getCodigo(),
                    LocalDateTime.now(this.clock));
            return this.trabajoGateway.guardar(trabajo);
        });
    }

    @Override
    public int verificarVencimientos() {
        LocalDateTime ahora = LocalDateTime.now(this.clock);
        int actualizados = 0;
        for (Proceso etapa : List.of(Proceso.SUSPENDIDO, Proceso.EN_CURSO, Proceso.PRORROGA, Proceso.SUSTENTACION,
                Proceso.APLAZADO)) {
            for (TrabajoGrado trabajo : this.trabajoGateway.listarPorEtapa(etapa)) {
                if (trabajo.verificarVencimientos(ahora)) {
                    this.trabajoGateway.guardar(trabajo);
                    actualizados++;
                }
            }
        }
        return actualizados;
    }

    // ----- Archivos -----

    private boolean hayArchivo(ArchivoAdjunto archivo) {
        return archivo != null && !archivo.estaVacio();
    }

    /** Solo se aceptan documentos PDF: extensión .pdf y contenido que inicia con la firma del formato. */
    private void validarPdf(ArchivoAdjunto archivo) {
        String nombre = archivo.nombre() == null ? "" : archivo.nombre().toLowerCase(Locale.ROOT);
        byte[] contenido = archivo.contenido();
        boolean firmaPdf = contenido.length >= FIRMA_PDF.length
                && Arrays.equals(Arrays.copyOf(contenido, FIRMA_PDF.length), FIRMA_PDF);
        if (!nombre.endsWith(".pdf") || !firmaPdf)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.archivo.formato_invalido");
    }

    /**
     * Valida y almacena el archivo en la carpeta de su tipo dentro del trabajo y
     * registra la ruta en {@code rutas} para poder eliminarlo si la operación
     * falla. Si no se envió archivo devuelve null: el dominio decide si era
     * obligatorio.
     */
    private String almacenar(TrabajoGrado trabajo, Long trabajoId, TipoDocumento tipo, ArchivoAdjunto archivo,
            List<String> rutas) {
        if (!hayArchivo(archivo))
            return null;
        validarPdf(archivo);
        String ruta = this.archivoGateway.guardar(trabajoId, tipo.carpeta(trabajo.getModalidad()), archivo);
        rutas.add(ruta);
        return ruta;
    }

    /** Ejecuta la operación; si falla, elimina los archivos que se almacenaron para ella. */
    private TrabajoGrado conArchivos(List<String> rutas, Supplier<TrabajoGrado> operacion) {
        try {
            return operacion.get();
        } catch (RuntimeException e) {
            rutas.forEach(this.archivoGateway::eliminar);
            throw e;
        }
    }

    /** @return true si la ruta está dentro de la carpeta del trabajo y no sale de ella */
    private static boolean perteneceAlTrabajo(String ruta, Long trabajoId) {
        if (CheckString.isBlank(ruta))
            return false;
        String normalizada = ruta.replace('\\', '/');
        return normalizada.startsWith(trabajoId + "/")
                && Arrays.stream(normalizada.split("/")).noneMatch(".."::equals);
    }

    // ----- Apoyo -----

    private Decision decisionConsejo(ResultadoConcepto resultado, String rutaResolucion, String observaciones) {
        return new Decision(Organo.CONSEJO_FACULTAD, resultado, null, rutaResolucion, observaciones, hoy());
    }

    private boolean puedeConsultar(TrabajoGrado trabajo, Long usuarioId) {
        return trabajo.participa(usuarioId) || tieneRolDeConsultaGeneral(usuarioId);
    }

    /** El jurado de la sesión actual que corresponde al usuario; si no lo es, sin acceso. */
    private Evaluador juradoDe(TrabajoGrado trabajo, Long usuarioId) {
        Optional<Evaluador> jurado = trabajo.getSustentacion() == null ? Optional.empty()
                : trabajo.getSustentacion().jurado(usuarioId);
        if (jurado.isEmpty())
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_jurado");
        return jurado.get();
    }

    private TrabajoGrado cargarTrabajoDelDirector(Long trabajoId, Long usuarioId) {
        TrabajoGrado trabajo = cargarTrabajo(trabajoId);
        if (!trabajo.esDirector(usuarioId))
            this.exceptionFormatter.returnNoAccess("error.trabajo_grado.solo_director");
        return trabajo;
    }

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
