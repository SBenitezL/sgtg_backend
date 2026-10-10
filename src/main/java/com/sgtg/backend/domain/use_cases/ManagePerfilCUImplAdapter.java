package com.sgtg.backend.domain.use_cases;

import java.util.Set;

import com.sgtg.backend.application.input.perfil.ManagePerfilCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageCatalogoGatewayIntPort;
import com.sgtg.backend.application.output.ManagePerfilGatewayIntPort;
import com.sgtg.backend.application.output.ManageUserGatewayIntPort;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.PerfilDocente;
import com.sgtg.backend.domain.models.participantes.PerfilEstudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

/**
 * Casos de uso de los perfiles académicos. Registrar un perfil que ya existe lo
 * actualiza con los nuevos datos.
 */
public class ManagePerfilCUImplAdapter implements ManagePerfilCUIntPort {

    /**
     * Roles que pueden tener perfil docente (dirigir, codirigir o evaluar). El
     * jefe de departamento lo necesita para indicar a qué departamento pertenece.
     */
    static final Set<String> ROLES_DOCENTE = Set.of(RoleAdmited.DOCENTE.name(), RoleAdmited.DIRECTOR.name(),
            RoleAdmited.CODIRECTOR.name(), RoleAdmited.EVALUADOR.name(), RoleAdmited.JURADO.name(),
            RoleAdmited.JEFE_DEP.name());
    /** Roles que pueden tener perfil de estudiante */
    static final Set<String> ROLES_ESTUDIANTE = Set.of(RoleAdmited.ESTUDIANTE.name());

    private final ManagePerfilGatewayIntPort perfilGateway;
    private final ManageCatalogoGatewayIntPort catalogoGateway;
    private final ManageUserGatewayIntPort userGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManagePerfilCUImplAdapter(ManagePerfilGatewayIntPort perfilGateway,
            ManageCatalogoGatewayIntPort catalogoGateway, ManageUserGatewayIntPort userGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        this.perfilGateway = perfilGateway;
        this.catalogoGateway = catalogoGateway;
        this.userGateway = userGateway;
        this.exceptionFormatter = exceptionFormatter;
    }

    @Override
    public PerfilDocente registrarPerfilDocente(Long usuarioId, TipoVinculacion tipoVinculacion,
            Long departamentoId) {
        validarUsuarioConRol(usuarioId, ROLES_DOCENTE);
        Departamento departamento = this.catalogoGateway.buscarDepartamento(departamentoId);
        if (departamento == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.catalogo.departamento_no_encontrado");
        PerfilDocente perfil = this.perfilGateway.buscarPerfilDocente(usuarioId);
        if (perfil == null) {
            perfil = new PerfilDocente(usuarioId, tipoVinculacion, departamento);
        } else {
            perfil.actualizarVinculacion(tipoVinculacion);
            perfil.cambiarDepartamento(departamento);
        }
        return this.perfilGateway.guardarPerfilDocente(perfil);
    }

    @Override
    public PerfilDocente consultarPerfilDocente(Long usuarioId) {
        PerfilDocente perfil = this.perfilGateway.buscarPerfilDocente(usuarioId);
        if (perfil == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.perfil.docente_no_encontrado");
        return perfil;
    }

    @Override
    public PerfilEstudiante registrarPerfilEstudiante(Long usuarioId, Long programaId) {
        validarUsuarioConRol(usuarioId, ROLES_ESTUDIANTE);
        Programa programa = this.catalogoGateway.buscarPrograma(programaId);
        if (programa == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.catalogo.programa_no_encontrado");
        PerfilEstudiante perfil = this.perfilGateway.buscarPerfilEstudiante(usuarioId);
        if (perfil == null)
            perfil = new PerfilEstudiante(usuarioId, programa);
        else
            perfil.cambiarPrograma(programa);
        return this.perfilGateway.guardarPerfilEstudiante(perfil);
    }

    @Override
    public PerfilEstudiante consultarPerfilEstudiante(Long usuarioId) {
        PerfilEstudiante perfil = this.perfilGateway.buscarPerfilEstudiante(usuarioId);
        if (perfil == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.perfil.estudiante_no_encontrado");
        return perfil;
    }

    private void validarUsuarioConRol(Long usuarioId, Set<String> rolesPermitidos) {
        Usuario usuario = this.userGateway.findById(usuarioId);
        if (usuario == null)
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.usuario.no_encontrado");
        if (usuario.getRole().stream().noneMatch(role -> rolesPermitidos.contains(role.getTrole())))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.perfil.rol_no_corresponde");
    }
}
