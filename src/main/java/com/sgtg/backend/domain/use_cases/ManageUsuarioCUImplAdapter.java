package com.sgtg.backend.domain.use_cases;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sgtg.backend.application.input.usuario.DatosPerfil;
import com.sgtg.backend.application.input.usuario.FichaUsuario;
import com.sgtg.backend.application.input.usuario.ManageUsuarioCUIntPort;
import com.sgtg.backend.application.input.usuario.NuevoUsuario;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Casos de uso de administración de usuarios. Solo un administrador crea,
 * edita, activa o inactiva usuarios y les asigna roles; no existe registro
 * público. El perfil académico (docente o estudiante) se registra junto con el
 * usuario.
 */
public class ManageUsuarioCUImplAdapter implements ManageUsuarioCUIntPort {

    /** Nombres de los roles que existen en el sistema */
    private static final Set<String> ROLES_EXISTENTES = Arrays.stream(RoleAdmited.values()).map(Enum::name)
            .collect(Collectors.toSet());

    private final ManageUserGatewayIntPort userGateway;
    private final ManageAuthGatewayIntPort authGateway;
    private final ManageAuthServiceGatewayIntPort authServiceGateway;
    private final ManagePerfilGatewayIntPort perfilGateway;
    private final ManageCatalogoGatewayIntPort catalogoGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageUsuarioCUImplAdapter(ManageUserGatewayIntPort userGateway, ManageAuthGatewayIntPort authGateway,
            ManageAuthServiceGatewayIntPort authServiceGateway, ManagePerfilGatewayIntPort perfilGateway,
            ManageCatalogoGatewayIntPort catalogoGateway, ExceptionFormatterIntPort exceptionFormatter) {
        this.userGateway = userGateway;
        this.authGateway = authGateway;
        this.authServiceGateway = authServiceGateway;
        this.perfilGateway = perfilGateway;
        this.catalogoGateway = catalogoGateway;
        this.exceptionFormatter = exceptionFormatter;
    }

    @Override
    public FichaUsuario crear(NuevoUsuario datos) {
        List<Role> rolesEncontrados = buscarRoles(datos.roles());
        Usuario usuario = new Usuario(null, datos.codigo(), datos.tipoDocumento(), datos.numeroDocumento(),
                datos.email(), datos.password(), datos.primerNombre(), datos.segundoNombre(), datos.primerApellido(),
                datos.segundoApellido(), rolesEncontrados);
        validarDatos(usuario);
        if (!Usuario.isPasswordNuevaValida(datos.password()))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_invalida");
        if (this.authGateway.existsByEmailorCodigo(datos.email(), datos.codigo()))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.ya_existe");
        if (this.authGateway.existsByDocumento(datos.tipoDocumento(), datos.numeroDocumento()))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.documento_existente");
        // El perfil se valida antes de guardar para no dejar un usuario sin el perfil pedido
        PerfilResuelto perfil = resolverPerfil(datos.perfil(), datos.roles());
        // El servicio de autenticación cifra la contraseña antes de guardar
        this.authServiceGateway.register(usuario);
        Usuario creado = this.userGateway.findByEmail(datos.email());
        guardarPerfil(creado.getCusuari(), perfil);
        return ficha(creado);
    }

    @Override
    public FichaUsuario actualizar(Long usuarioId, NuevoUsuario datos) {
        Usuario usuario = this.consultar(usuarioId);
        List<Role> rolesEncontrados = buscarRoles(datos.roles());
        usuario.actualizarDatos(datos.codigo(), datos.tipoDocumento(), datos.numeroDocumento(), datos.email(),
                datos.primerNombre(), datos.segundoNombre(), datos.primerApellido(), datos.segundoApellido());
        usuario.updateRole(rolesEncontrados);
        validarDatos(usuario);
        boolean cambiaPassword = !CheckString.isBlank(datos.password());
        if (cambiaPassword && !Usuario.isPasswordNuevaValida(datos.password()))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_invalida");
        if (this.userGateway.existeEmailOCodigoEnOtro(datos.email(), datos.codigo(), usuarioId))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.ya_existe");
        if (this.userGateway.existeDocumentoEnOtro(datos.tipoDocumento(), datos.numeroDocumento(), usuarioId))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.documento_existente");
        PerfilResuelto perfil = resolverPerfil(datos.perfil(), datos.roles());
        this.userGateway.actualizarDatos(usuario);
        if (cambiaPassword)
            this.authServiceGateway.actualizarPassword(usuarioId, datos.password());
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
    public List<FichaUsuario> listar() {
        return this.userGateway.listar().stream().map(this::ficha).toList();
    }

    @Override
    public FichaUsuario asignarRoles(Long usuarioId, List<String> roles) {
        List<Role> rolesEncontrados = buscarRoles(roles);
        Usuario usuario = this.consultar(usuarioId);
        usuario.updateRole(rolesEncontrados);
        this.userGateway.actualizarRoles(usuario);
        return ficha(usuario);
    }

    @Override
    public void cambiarPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = this.consultar(usuarioId);
        // 400 y no 401: el usuario sigue autenticado aunque se equivoque de contraseña
        if (CheckString.isBlank(passwordActual)
                || !this.authServiceGateway.verificarCredenciales(usuario.getEmail(), passwordActual))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_actual_incorrecta");
        if (!Usuario.isPasswordNuevaValida(passwordNueva))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_invalida");
        if (passwordNueva.equals(passwordActual))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.password_igual");
        this.authServiceGateway.actualizarPassword(usuarioId, passwordNueva);
    }

    @Override
    public FichaUsuario cambiarEstado(Long administradorId, Long usuarioId, boolean activo) {
        if (!activo && usuarioId != null && usuarioId.equals(administradorId))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.no_puede_inactivarse");
        Usuario usuario = this.consultar(usuarioId);
        usuario.cambiarEstado(activo);
        this.userGateway.actualizarEstado(usuario);
        return ficha(usuario);
    }

    // ----- Apoyo -----

    private void validarDatos(Usuario usuario) {
        if (!usuario.isNumeroDocumentoValido())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.numero_documento_invalido");
        if (!usuario.isValidRegister())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.datos_incompletos");
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
