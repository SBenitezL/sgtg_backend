package com.sgtg.backend.domain.use_cases;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sgtg.backend.application.input.usuario.ManageUsuarioCUIntPort;
import com.sgtg.backend.application.input.usuario.NuevoUsuario;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageAuthGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;

/**
 * Casos de uso de administración de usuarios. Solo un administrador crea
 * usuarios y les asigna roles; no existe registro público.
 */
public class ManageUsuarioCUImplAdapter implements ManageUsuarioCUIntPort {

    /** Nombres de los roles que existen en el sistema */
    private static final Set<String> ROLES_EXISTENTES = Arrays.stream(RoleAdmited.values()).map(Enum::name)
            .collect(Collectors.toSet());

    private final ManageUserGatewayIntPort userGateway;
    private final ManageAuthGatewayIntPort authGateway;
    private final ManageAuthServiceGatewayIntPort authServiceGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageUsuarioCUImplAdapter(ManageUserGatewayIntPort userGateway, ManageAuthGatewayIntPort authGateway,
            ManageAuthServiceGatewayIntPort authServiceGateway, ExceptionFormatterIntPort exceptionFormatter) {
        this.userGateway = userGateway;
        this.authGateway = authGateway;
        this.authServiceGateway = authServiceGateway;
        this.exceptionFormatter = exceptionFormatter;
    }

    @Override
    public Usuario crear(NuevoUsuario datos) {
        List<Role> rolesEncontrados = buscarRoles(datos.roles());
        Usuario usuario = new Usuario(null, datos.codigo(), datos.tipoDocumento(), datos.numeroDocumento(),
                datos.email(), datos.password(), datos.primerNombre(), datos.segundoNombre(), datos.primerApellido(),
                datos.segundoApellido(), rolesEncontrados);
        if (!usuario.isNumeroDocumentoValido())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.numero_documento_invalido");
        if (!usuario.isValidRegister())
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.usuario.datos_incompletos");
        if (this.authGateway.existsByEmailorCodigo(datos.email(), datos.codigo()))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.ya_existe");
        if (this.authGateway.existsByDocumento(datos.tipoDocumento(), datos.numeroDocumento()))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.usuario.documento_existente");
        // El servicio de autenticación cifra la contraseña antes de guardar
        this.authServiceGateway.register(usuario);
        return this.userGateway.findByEmail(datos.email());
    }

    @Override
    public Usuario consultar(Long usuarioId) {
        Usuario usuario = this.userGateway.findById(usuarioId);
        if (usuario == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.usuario.no_encontrado");
        return usuario;
    }

    @Override
    public Usuario asignarRoles(Long usuarioId, List<String> roles) {
        List<Role> rolesEncontrados = buscarRoles(roles);
        Usuario usuario = this.consultar(usuarioId);
        usuario.updateRole(rolesEncontrados);
        this.userGateway.actualizarRoles(usuario);
        return usuario;
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
}
