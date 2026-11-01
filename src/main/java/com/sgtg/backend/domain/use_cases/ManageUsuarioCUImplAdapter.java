package com.sgtg.backend.domain.use_cases;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sgtg.backend.application.input.usuario.CambioComite;
import com.sgtg.backend.application.input.usuario.DatosPerfil;
import com.sgtg.backend.application.input.usuario.FichaUsuario;
import com.sgtg.backend.application.input.usuario.ManageUsuarioCUIntPort;
import com.sgtg.backend.application.input.usuario.NuevoUsuario;
import com.sgtg.backend.application.input.usuario.UsuarioGestionado;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.application.output.ManageTrabajoGradoGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.acceso.PoliticaPassword;
import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.domain.models.notificacion.TipoNotificacion;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Participante;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Casos de uso de administración de usuarios; no existe registro público. El
 * administrador gestiona a todos los usuarios. Los roles administrativos también
 * los gestionan, con límites: Consejo y Decanatura a toda la facultad, el Comité
 * y el jefe de departamento a los docentes de su departamento y a los
 * estudiantes de sus programas; solo asignan los roles Estudiante, Docente,
 * Director y Asesor y no modifican a otros usuarios con roles administrativos.
 * Los asesores externos (solo con el rol Asesor y sin perfil académico) los
 * gestionan todos. El perfil académico (docente o estudiante) se registra junto
 * con el usuario.
 * <p>
 * Cada departamento tiene un solo Comité. Se cambia con {@link #cambiarComite}:
 * el Comité lo cede, o lo nombra el jefe del departamento, el Consejo,
 * Decanatura o el administrador.
 */
public class ManageUsuarioCUImplAdapter implements ManageUsuarioCUIntPort {

    /** Nombres de los roles que existen en el sistema */
    private static final Set<String> ROLES_EXISTENTES = Arrays.stream(RoleAdmited.values()).map(Enum::name)
            .collect(Collectors.toSet());

    /** Roles que administran la facultad: a estos usuarios solo los modifica el administrador */
    private static final Set<String> ROLES_ADMINISTRATIVOS = Set.of(RoleAdmited.ADMINISTRADOR.name(),
            RoleAdmited.COMITE.name(), RoleAdmited.JEFE_DEP.name(), RoleAdmited.CONSEJO.name(),
            RoleAdmited.DECANATURA.name());
    /** Gestores que, sin ser administradores, gestionan a todos los usuarios de la facultad */
    private static final Set<String> GESTORES_DE_LA_FACULTAD = Set.of(RoleAdmited.CONSEJO.name(),
            RoleAdmited.DECANATURA.name());
    /** Gestores que gestionan a los usuarios de su departamento y de sus programas */
    private static final Set<String> GESTORES_DEL_DEPARTAMENTO = Set.of(RoleAdmited.COMITE.name(),
            RoleAdmited.JEFE_DEP.name());
    /** Únicos roles que asigna o quita un gestor que no es el administrador */
    private static final Set<String> ROLES_ASIGNABLES_POR_GESTORES = Set.of(RoleAdmited.ESTUDIANTE.name(),
            RoleAdmited.DOCENTE.name(), RoleAdmited.DIRECTOR.name(), RoleAdmited.ASESOR.name());
    /** Roles de un puesto en un trabajo: no se quitan mientras el usuario lo ocupe en uno en curso */
    private static final Set<String> ROLES_DE_TRABAJO = Set.of(RoleAdmited.DIRECTOR.name(),
            RoleAdmited.CODIRECTOR.name(), RoleAdmited.ASESOR.name());
    private static final String COMITE = RoleAdmited.COMITE.name();

    private final ManageUserGatewayIntPort userGateway;
    private final ManageAuthGatewayIntPort authGateway;
    private final ManageAuthServiceGatewayIntPort authServiceGateway;
    private final ManagePerfilGatewayIntPort perfilGateway;
    private final ManageCatalogoGatewayIntPort catalogoGateway;
    private final ManageTrabajoGradoGatewayIntPort trabajoGateway;
    private final ManageNotificacionGatewayIntPort notificacionGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;
    private final Clock reloj;

    public ManageUsuarioCUImplAdapter(ManageUserGatewayIntPort userGateway, ManageAuthGatewayIntPort authGateway,
            ManageAuthServiceGatewayIntPort authServiceGateway, ManagePerfilGatewayIntPort perfilGateway,
            ManageCatalogoGatewayIntPort catalogoGateway, ManageTrabajoGradoGatewayIntPort trabajoGateway,
            ManageNotificacionGatewayIntPort notificacionGateway, ExceptionFormatterIntPort exceptionFormatter,
            Clock reloj) {
        this.userGateway = userGateway;
        this.authGateway = authGateway;
        this.authServiceGateway = authServiceGateway;
        this.perfilGateway = perfilGateway;
        this.catalogoGateway = catalogoGateway;
        this.trabajoGateway = trabajoGateway;
        this.notificacionGateway = notificacionGateway;
        this.exceptionFormatter = exceptionFormatter;
        this.reloj = reloj;
    }

    @Override
    public FichaUsuario crear(NuevoUsuario datos) {
        List<Role> rolesEncontrados = buscarRoles(datos.roles());
        Usuario usuario = new Usuario(null, datos.codigo(), datos.tipoDocumento(), datos.numeroDocumento(),
                datos.email(), datos.primerNombre(), datos.segundoNombre(), datos.primerApellido(),
                datos.segundoApellido(), rolesEncontrados);
        validarDatos(usuario);
        validarPassword(datos.password(), usuario);
        if (this.authGateway.existsByEmailorCodigo(datos.email(), datos.codigo()))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.ya_existe");
        if (this.authGateway.existsByDocumento(datos.tipoDocumento(), datos.numeroDocumento()))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.documento_existente");
        // El perfil se valida antes de guardar para no dejar un usuario sin el perfil pedido
        PerfilResuelto perfil = resolverPerfil(datos.perfil(), datos.roles());
        if (datos.roles().contains(COMITE))
            validarComiteUnico(perfil.departamento(), null);
        // El servicio de autenticación cifra la contraseña y la guarda en las credenciales
        this.authServiceGateway.register(usuario, datos.password());
        Usuario creado = this.userGateway.findByEmail(datos.email());
        guardarPerfil(creado.getCusuari(), perfil);
        return ficha(creado);
    }

    @Override
    public FichaUsuario crear(Long gestorId, NuevoUsuario datos) {
        Gestor gestor = gestor(gestorId);
        validarCambioDeRoles(gestor, List.of(), datos.roles());
        if (!gestor.todaLaFacultad() && datos.roles() != null)
            validarPerfilEnAlcance(gestor, resolverPerfil(datos.perfil(), datos.roles()), datos.roles());
        return crear(datos);
    }

    @Override
    public FichaUsuario actualizar(Long gestorId, Long usuarioId, NuevoUsuario datos) {
        Gestor gestor = gestor(gestorId);
        if (!gestor.administrador()) {
            FichaUsuario actual = consultarFicha(usuarioId);
            validarModificable(gestor, actual);
            validarCambioDeRoles(gestor, nombresDeRoles(actual.usuario()), datos.roles());
            if (!gestor.todaLaFacultad() && datos.roles() != null)
                validarPerfilEnAlcance(gestor, resolverPerfil(datos.perfil(), datos.roles()), datos.roles());
        }
        return actualizar(usuarioId, datos);
    }

    @Override
    public FichaUsuario actualizar(Long usuarioId, NuevoUsuario datos) {
        Usuario usuario = this.consultar(usuarioId);
        Set<String> rolesActuales = nombresDeRoles(usuario);
        List<Role> rolesEncontrados = buscarRoles(datos.roles());
        validarRolesDeTrabajoLibres(usuarioId, rolesActuales, datos.roles());
        usuario.actualizarDatos(datos.codigo(), datos.tipoDocumento(), datos.numeroDocumento(), datos.email(),
                datos.primerNombre(), datos.segundoNombre(), datos.primerApellido(), datos.segundoApellido());
        usuario.updateRole(rolesEncontrados);
        validarDatos(usuario);
        boolean cambiaPassword = !CheckString.isBlank(datos.password());
        if (cambiaPassword)
            validarPassword(datos.password(), usuario);
        if (this.userGateway.existeEmailOCodigoEnOtro(datos.email(), datos.codigo(), usuarioId))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.ya_existe");
        if (this.userGateway.existeDocumentoEnOtro(datos.tipoDocumento(), datos.numeroDocumento(), usuarioId))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.documento_existente");
        PerfilResuelto perfil = resolverPerfil(datos.perfil(), datos.roles());
        if (datos.roles().contains(COMITE) && !rolesActuales.contains(COMITE))
            validarComiteUnico(perfil.departamento(), usuarioId);
        this.userGateway.actualizarDatos(usuario);
        // Una contraseña asignada por un administrador se debe cambiar en el siguiente ingreso
        if (cambiaPassword)
            this.authServiceGateway.asignarPassword(usuarioId, datos.password());
        guardarPerfil(usuarioId, perfil);
        return ficha(usuario);
    }

    @Override
    public Usuario consultar(Long usuarioId) {
        Usuario usuario = this.userGateway.findById(usuarioId);
        if (usuario == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.usuario.no_encontrado");
        return usuario;
    }

    @Override
    public FichaUsuario consultarFicha(Long usuarioId) {
        return ficha(this.consultar(usuarioId));
    }

    @Override
    public FichaUsuario consultarFicha(Long gestorId, Long usuarioId) {
        Gestor gestor = gestor(gestorId);
        FichaUsuario ficha = consultarFicha(usuarioId);
        if (!gestor.enAlcance(ficha))
            this.exceptionFormatter.returnNoAccess("error.usuario.fuera_de_alcance");
        return ficha;
    }

    @Override
    public List<FichaUsuario> listar() {
        return this.userGateway.listar().stream().map(this::ficha).toList();
    }

    @Override
    public List<UsuarioGestionado> listarGestionados(Long gestorId) {
        Gestor gestor = gestor(gestorId);
        return listar().stream().filter(gestor::enAlcance)
                .map(ficha -> new UsuarioGestionado(ficha, gestor.administrador() || !esAdministrativo(ficha)))
                .toList();
    }

    @Override
    public FichaUsuario asignarRoles(Long gestorId, Long usuarioId, List<String> roles) {
        Gestor gestor = gestor(gestorId);
        if (!gestor.administrador()) {
            FichaUsuario actual = consultarFicha(usuarioId);
            validarModificable(gestor, actual);
            validarCambioDeRoles(gestor, nombresDeRoles(actual.usuario()), roles);
        }
        return asignarRoles(usuarioId, roles);
    }

    @Override
    public FichaUsuario asignarRoles(Long usuarioId, List<String> roles) {
        List<Role> rolesEncontrados = buscarRoles(roles);
        Usuario usuario = this.consultar(usuarioId);
        Set<String> rolesActuales = nombresDeRoles(usuario);
        validarRolesDeTrabajoLibres(usuarioId, rolesActuales, roles);
        if (roles.contains(COMITE) && !rolesActuales.contains(COMITE)) {
            PerfilDocente perfil = this.perfilGateway.buscarPerfilDocente(usuarioId);
            validarComiteUnico(perfil == null ? null : perfil.getDepartamento(), usuarioId);
        }
        usuario.updateRole(rolesEncontrados);
        this.userGateway.actualizarRoles(usuario);
        return ficha(usuario);
    }

    @Override
    public CambioComite cambiarComite(Long gestorId, Long nuevoComiteId) {
        Gestor gestor = gestor(gestorId);
        FichaUsuario nuevo = consultarFicha(nuevoComiteId);
        if (!nuevo.usuario().isActivo())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.comite_inactivo");
        Departamento departamento = nuevo.perfilDocente() == null ? null : nuevo.perfilDocente().getDepartamento();
        if (departamento == null)
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.comite_sin_departamento");
        // El Comité y el jefe solo cambian el de su departamento: nunca pasa a otro departamento
        if (!gestor.delDepartamento(departamento))
            this.exceptionFormatter.returnNoAccess("error.usuario.comite_otro_departamento");
        if (nombresDeRoles(nuevo.usuario()).contains(COMITE))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.ya_es_comite");
        validarNoCodirigeEnElDepartamento(nuevoComiteId, departamento);

        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Usuario> anteriores = comitesDelDepartamento(departamento, nuevoComiteId);
        List<Notificacion> avisos = new ArrayList<>();
        for (Usuario anterior : anteriores) {
            Set<String> roles = new HashSet<>(nombresDeRoles(anterior));
            roles.remove(COMITE);
            // Sigue como docente del departamento
            roles.add(RoleAdmited.DOCENTE.name());
            anterior.updateRole(buscarRoles(List.copyOf(roles)));
            this.userGateway.actualizarRoles(anterior);
            // Sus sesiones abiertas todavía muestran la gestión del Comité
            this.authServiceGateway.cerrarSesionesDe(anterior.getCusuari());
            avisos.add(Notificacion.nueva(anterior.getCusuari(), TipoNotificacion.ALERTA, "Ya no eres el Comité",
                    "El rol de Comité de programa del " + departamento.nombre() + " pasó a "
                            + nombre(nuevo.usuario()) + ". Sigues como docente.", null, null, null, null, ahora));
        }
        Set<String> rolesNuevos = new HashSet<>(nombresDeRoles(nuevo.usuario()));
        rolesNuevos.add(COMITE);
        Usuario comite = nuevo.usuario();
        comite.updateRole(buscarRoles(List.copyOf(rolesNuevos)));
        this.userGateway.actualizarRoles(comite);
        avisos.add(Notificacion.nueva(comite.getCusuari(), TipoNotificacion.INFO, "Ahora eres el Comité de programa",
                "Recibiste el rol de Comité de programa del " + departamento.nombre()
                        + ". Vuelve a iniciar sesión si no ves las opciones del Comité.", null, null, null, null, ahora));
        this.notificacionGateway.guardarTodas(avisos);
        return new CambioComite(ficha(comite), departamento, anteriores.stream().map(Usuario::getCusuari).toList());
    }

    @Override
    public String cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva, String sesion) {
        Usuario usuario = this.consultar(usuarioId);
        // 400 y no 401: el usuario sigue autenticado aunque se equivoque de contraseña
        if (CheckString.isBlank(passwordActual)
                || !this.authServiceGateway.verificarCredenciales(usuario.getEmail(), passwordActual))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_actual_incorrecta");
        validarPassword(passwordNueva, usuario);
        if (passwordNueva.equals(passwordActual))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_igual");
        if (this.authServiceGateway.esPasswordReciente(usuarioId, passwordNueva))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_reciente");
        return this.authServiceGateway.cambiarPassword(usuarioId, passwordNueva, sesion);
    }

    @Override
    public FichaUsuario cambiarEstado(Long administradorId, Long usuarioId, boolean activo) {
        if (!activo && usuarioId != null && usuarioId.equals(administradorId))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.no_puede_inactivarse");
        Gestor gestor = gestor(administradorId);
        if (!gestor.administrador())
            validarModificable(gestor, consultarFicha(usuarioId));
        Usuario usuario = this.consultar(usuarioId);
        usuario.cambiarEstado(activo);
        this.userGateway.actualizarEstado(usuario);
        // Un usuario inactivo no puede seguir en sus sesiones abiertas
        if (!activo)
            this.authServiceGateway.cerrarSesionesDe(usuarioId);
        return ficha(usuario);
    }

    // ----- Gestores -----

    /**
     * Quien gestiona usuarios: el administrador, sin restricciones; Consejo y
     * Decanatura, toda la facultad; el Comité y el jefe de departamento, los
     * docentes de su departamento y los estudiantes de sus programas.
     *
     * @param departamento departamento del Comité o del jefe; null si gestiona toda la facultad
     * @param programas    programas de ese departamento
     */
    private record Gestor(boolean administrador, Departamento departamento, Set<Long> programas) {

        boolean todaLaFacultad() {
            return administrador || departamento == null;
        }

        boolean delDepartamento(Departamento otro) {
            return todaLaFacultad() || departamento.esMismo(otro);
        }

        boolean delPrograma(Programa programa) {
            return todaLaFacultad() || (programa != null && programas.contains(programa.id()));
        }

        /** ¿Es un docente de su departamento, un estudiante de sus programas o un asesor externo? */
        boolean enAlcance(FichaUsuario ficha) {
            return todaLaFacultad() || esAsesorExterno(ficha)
                    || (ficha.perfilDocente() != null && delDepartamento(ficha.perfilDocente().getDepartamento()))
                    || (ficha.perfilEstudiante() != null && delPrograma(ficha.perfilEstudiante().getPrograma()));
        }
    }

    private Gestor gestor(Long gestorId) {
        Set<String> roles = nombresDeRoles(this.consultar(gestorId));
        if (roles.contains(RoleAdmited.ADMINISTRADOR.name()))
            return new Gestor(true, null, Set.of());
        if (roles.stream().anyMatch(GESTORES_DE_LA_FACULTAD::contains))
            return new Gestor(false, null, Set.of());
        PerfilDocente perfil = roles.stream().anyMatch(GESTORES_DEL_DEPARTAMENTO::contains)
                ? this.perfilGateway.buscarPerfilDocente(gestorId)
                : null;
        if (perfil == null || perfil.getDepartamento() == null) {
            this.exceptionFormatter.returnNoAccess("error.usuario.sin_permiso_gestion");
            return null;
        }
        Departamento departamento = perfil.getDepartamento();
        Set<Long> programas = this.catalogoGateway.listarProgramas().stream()
                .filter(programa -> programa.esDelDepartamento(departamento))
                .map(Programa::id)
                .collect(Collectors.toSet());
        return new Gestor(false, departamento, programas);
    }

    private static Set<String> nombresDeRoles(Usuario usuario) {
        return usuario.getRole() == null ? Set.of()
                : usuario.getRole().stream().map(Role::getTrole).collect(Collectors.toSet());
    }

    /** Asesor sin perfil académico (de una empresa u otra institución): no es de ningún departamento */
    private static boolean esAsesorExterno(FichaUsuario ficha) {
        return ficha.perfilDocente() == null && ficha.perfilEstudiante() == null
                && soloAsesor(nombresDeRoles(ficha.usuario()));
    }

    private static boolean soloAsesor(Collection<String> roles) {
        return roles != null && !roles.isEmpty() && roles.stream().allMatch(RoleAdmited.ASESOR.name()::equals);
    }

    private static boolean esAdministrativo(FichaUsuario ficha) {
        return nombresDeRoles(ficha.usuario()).stream().anyMatch(ROLES_ADMINISTRATIVOS::contains);
    }

    /** Un gestor que no es el administrador modifica a los usuarios de su alcance sin roles administrativos. */
    private void validarModificable(Gestor gestor, FichaUsuario ficha) {
        if (gestor.administrador())
            return;
        if (esAdministrativo(ficha))
            this.exceptionFormatter.returnNoAccess("error.usuario.usuario_administrativo");
        if (!gestor.enAlcance(ficha))
            this.exceptionFormatter.returnNoAccess("error.usuario.fuera_de_alcance");
    }

    /** Un gestor que no es el administrador solo asigna o quita los roles Estudiante, Docente, Director y Asesor. */
    private void validarCambioDeRoles(Gestor gestor, Collection<String> actuales, Collection<String> nuevos) {
        if (gestor.administrador() || nuevos == null)
            return;
        Set<String> cambiados = new HashSet<>(nuevos);
        cambiados.removeAll(actuales);
        Set<String> quitados = new HashSet<>(actuales);
        quitados.removeAll(nuevos);
        cambiados.addAll(quitados);
        if (!ROLES_ASIGNABLES_POR_GESTORES.containsAll(cambiados))
            this.exceptionFormatter.returnNoAccess("error.usuario.rol_no_asignable");
    }

    /**
     * El perfil pedido por el Comité o el jefe queda en su alcance: al menos un
     * perfil, el docente de su departamento y el de estudiante de uno de sus
     * programas. Un asesor externo (solo el rol Asesor) no lleva perfil.
     */
    private void validarPerfilEnAlcance(Gestor gestor, PerfilResuelto perfil, Collection<String> roles) {
        boolean conPerfil = perfil.departamento() != null || perfil.programa() != null;
        if (!conPerfil && soloAsesor(roles))
            return;
        boolean docente = perfil.departamento() == null || gestor.delDepartamento(perfil.departamento());
        boolean estudiante = perfil.programa() == null || gestor.delPrograma(perfil.programa());
        if (!conPerfil || !docente || !estudiante)
            this.exceptionFormatter.returnNoAccess("error.usuario.fuera_de_alcance");
    }

    /**
     * No se quita el rol Director, Codirector o Asesor a quien ocupa ese puesto
     * en un trabajo en curso: perdería el acceso al trabajo.
     */
    private void validarRolesDeTrabajoLibres(Long usuarioId, Collection<String> actuales, Collection<String> nuevos) {
        if (nuevos == null)
            return;
        Set<String> quitados = actuales.stream().filter(ROLES_DE_TRABAJO::contains)
                .filter(rol -> !nuevos.contains(rol)).collect(Collectors.toSet());
        if (quitados.isEmpty())
            return;
        for (TrabajoGrado trabajo : this.trabajoGateway.listarPorParticipante(usuarioId)) {
            if (trabajo.estaCerrado())
                continue;
            if ((quitados.contains(RoleAdmited.DIRECTOR.name()) && ocupa(trabajo.getDirector(), usuarioId))
                    || (quitados.contains(RoleAdmited.CODIRECTOR.name()) && ocupa(trabajo.getCodirector(), usuarioId))
                    || (quitados.contains(RoleAdmited.ASESOR.name()) && ocupa(trabajo.getAsesor(), usuarioId)))
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.rol_de_trabajo_en_uso");
        }
    }

    private static boolean ocupa(Participante participante, Long usuarioId) {
        return participante != null && usuarioId.equals(participante.getUsuarioId());
    }

    /** Cada departamento tiene un solo Comité: para cambiarlo está {@link #cambiarComite}. */
    private void validarComiteUnico(Departamento departamento, Long usuarioId) {
        if (departamento != null && !comitesDelDepartamento(departamento, usuarioId).isEmpty())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.comite_existente");
    }

    /** Usuarios activos con el rol Comité del departamento, salvo {@code excepto} */
    private List<Usuario> comitesDelDepartamento(Departamento departamento, Long excepto) {
        return this.userGateway.listar().stream()
                .filter(Usuario::isActivo)
                .filter(usuario -> !usuario.getCusuari().equals(excepto))
                .filter(usuario -> nombresDeRoles(usuario).contains(COMITE))
                .filter(usuario -> {
                    PerfilDocente perfil = this.perfilGateway.buscarPerfilDocente(usuario.getCusuari());
                    return perfil != null && departamento.esMismo(perfil.getDepartamento());
                })
                .toList();
    }

    /**
     * El Comité no puede codirigir los trabajos de su propio programa (Art. 11):
     * quien codirige uno en curso de este departamento no puede recibir el rol.
     */
    private void validarNoCodirigeEnElDepartamento(Long usuarioId, Departamento departamento) {
        for (TrabajoGrado trabajo : this.trabajoGateway.listarPorParticipante(usuarioId)) {
            if (trabajo.estaCerrado() || !ocupa(trabajo.getCodirector(), usuarioId) || trabajo.getEstudiantes().isEmpty())
                continue;
            Programa programa = this.catalogoGateway.buscarPrograma(trabajo.getEstudiantes().get(0).getPrograma().id());
            if (programa != null && programa.esDelDepartamento(departamento))
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.comite_codirector");
        }
    }

    private static String nombre(Usuario usuario) {
        return String.join(" ", usuario.getPrimerNombre(), usuario.getPrimerApellido());
    }

    // ----- Apoyo -----

    private void validarDatos(Usuario usuario) {
        if (!usuario.isNumeroDocumentoValido())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.numero_documento_invalido");
        if (!usuario.isValidRegister())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.datos_incompletos");
    }

    /** Valida una contraseña nueva contra la política, con los datos del usuario al que se asigna. */
    private void validarPassword(String password, Usuario usuario) {
        switch (PoliticaPassword.revisar(password, usuario.getEmail(), usuario.getNumeroDocumento())) {
            case INVALIDA -> this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_invalida");
            case PREDECIBLE ->
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_predecible");
            case VALIDA -> {
                // Cumple la política
            }
        }
    }

    /**
     * Valida que se indique al menos un rol y que todos existan, y los carga con su
     * identificador.
     */
    private List<Role> buscarRoles(List<String> roles) {
        if (roles == null || roles.isEmpty())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.roles_obligatorios");
        List<String> sinRepetir = roles.stream().distinct().toList();
        if (!ROLES_EXISTENTES.containsAll(sinRepetir))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.roles_invalidos");
        List<Role> encontrados = this.authGateway.findRolesByTrole(sinRepetir);
        if (encontrados.size() != sinRepetir.size())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.roles_invalidos");
        return encontrados;
    }

    /** Perfil validado, con el departamento y el programa ya cargados del catálogo. */
    private record PerfilResuelto(TipoVinculacion tipoVinculacion, Departamento departamento, Programa programa) {
    }

    /**
     * Valida el perfil pedido contra los roles y los catálogos: el perfil docente
     * exige vinculación y departamento, y un rol docente; el de estudiante, el
     * rol ESTUDIANTE.
     */
    private PerfilResuelto resolverPerfil(DatosPerfil perfil, List<String> roles) {
        Departamento departamento = null;
        Programa programa = null;
        if (perfil.tienePerfilDocente()) {
            if (perfil.tipoVinculacion() == null || perfil.departamentoId() == null)
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.perfil_docente_incompleto");
            if (roles.stream().noneMatch(ManagePerfilCUImplAdapter.ROLES_DOCENTE::contains))
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.perfil.rol_no_corresponde");
            departamento = this.catalogoGateway.buscarDepartamento(perfil.departamentoId());
            if (departamento == null)
                this.exceptionFormatter.returnResponseErrorEntityNotFound("error.catalogo.departamento_no_encontrado");
        }
        if (perfil.tienePerfilEstudiante()) {
            if (roles.stream().noneMatch(ManagePerfilCUImplAdapter.ROLES_ESTUDIANTE::contains))
                this.exceptionFormatter.returnResponseBusinessRuleViolated("error.perfil.rol_no_corresponde");
            programa = this.catalogoGateway.buscarPrograma(perfil.programaId());
            if (programa == null)
                this.exceptionFormatter.returnResponseErrorEntityNotFound("error.catalogo.programa_no_encontrado");
        }
        return new PerfilResuelto(perfil.tipoVinculacion(), departamento, programa);
    }

    /** Registra o actualiza los perfiles validados del usuario. */
    private void guardarPerfil(Long usuarioId, PerfilResuelto perfil) {
        if (perfil.departamento() != null) {
            PerfilDocente docente = this.perfilGateway.buscarPerfilDocente(usuarioId);
            if (docente == null) {
                docente = new PerfilDocente(usuarioId, perfil.tipoVinculacion(), perfil.departamento());
            } else {
                docente.actualizarVinculacion(perfil.tipoVinculacion());
                docente.cambiarDepartamento(perfil.departamento());
            }
            this.perfilGateway.guardarPerfilDocente(docente);
        }
        if (perfil.programa() != null) {
            PerfilEstudiante estudiante = this.perfilGateway.buscarPerfilEstudiante(usuarioId);
            if (estudiante == null)
                estudiante = new PerfilEstudiante(usuarioId, perfil.programa());
            else
                estudiante.cambiarPrograma(perfil.programa());
            this.perfilGateway.guardarPerfilEstudiante(estudiante);
        }
    }

    private FichaUsuario ficha(Usuario usuario) {
        Long id = usuario.getCusuari();
        return new FichaUsuario(usuario, this.perfilGateway.buscarPerfilDocente(id),
                this.perfilGateway.buscarPerfilEstudiante(id));
    }
}
