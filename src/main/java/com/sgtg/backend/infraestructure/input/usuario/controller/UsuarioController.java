package com.sgtg.backend.infraestructure.input.usuario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.usuario.ManageUsuarioCUIntPort;
import com.sgtg.backend.domain.models.Role.RoleAdmited;
import com.sgtg.backend.infraestructure.input.auth.dto.output.AuthDTOResponse;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.ActualizarUsuarioDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.AsignarRolesDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.CambiarEstadoDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.CambiarPasswordDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.CrearUsuarioDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.output.UsuarioDTOResponse;
import com.sgtg.backend.infraestructure.input.usuario.dto.output.UsuarioGestionadoDTOResponse;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión de usuarios (creación, edición, consulta, asignación de roles y
 * activación, con su perfil académico) por el administrador y los roles
 * administrativos, cada uno en su alcance (ver {@link ManageUsuarioCUIntPort});
 * listado para los usuarios autenticados; consulta y cambio de contraseña del
 * usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/sgtg/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    /** Roles que gestionan usuarios; el caso de uso limita lo que puede hacer cada uno */
    private static final String GESTORES = "hasAnyAuthority('ADMINISTRADOR', 'COMITE', 'JEFE_DEP', 'CONSEJO', 'DECANATURA')";

    private final ManageUsuarioCUIntPort manageUsuario;

    /** Crea un usuario con sus roles. No hay registro público: lo crea un gestor. */
    @PostMapping
    @PreAuthorize(GESTORES)
    public ResponseEntity<UsuarioDTOResponse> crear(@AuthenticationPrincipal UserEntity usuario,
            @Valid @RequestBody CrearUsuarioDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                UsuarioDTOResponse.desde(manageUsuario.crear(usuario.getCusuari(), request.toNuevoUsuario())));
    }

    /** Usuarios que gestiona el usuario autenticado, con su documento y si los puede modificar. */
    @GetMapping("/gestion")
    @PreAuthorize(GESTORES)
    public ResponseEntity<List<UsuarioGestionadoDTOResponse>> listarGestionados(
            @AuthenticationPrincipal UserEntity usuario) {
        return ResponseEntity.ok(manageUsuario.listarGestionados(usuario.getCusuari()).stream()
                .map(UsuarioGestionadoDTOResponse::desde).toList());
    }

    /**
     * Lista los usuarios con sus perfiles. Cualquier usuario autenticado la usa
     * para elegir estudiantes, directores o evaluadores; solo el administrador
     * recibe el documento de identidad.
     */
    @GetMapping
    public ResponseEntity<List<UsuarioDTOResponse>> listar(@AuthenticationPrincipal UserEntity usuario) {
        boolean esAdministrador = usuario.getAuthorities().stream()
                .anyMatch(rol -> RoleAdmited.ADMINISTRADOR.name().equals(rol.getAuthority()));
        return ResponseEntity.ok(manageUsuario.listar().stream().map(UsuarioDTOResponse::desde)
                .map(dto -> esAdministrador ? dto : dto.sinDocumento()).toList());
    }

    /** Datos del usuario autenticado. */
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTOResponse> consultarActual(@AuthenticationPrincipal UserEntity usuario) {
        return ResponseEntity.ok(UsuarioDTOResponse.desde(manageUsuario.consultarFicha(usuario.getCusuari())));
    }

    @GetMapping("/{usuarioId}")
    @PreAuthorize(GESTORES)
    public ResponseEntity<UsuarioDTOResponse> consultar(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(UsuarioDTOResponse.desde(manageUsuario.consultarFicha(usuario.getCusuari(), usuarioId)));
    }

    /** Actualiza datos, roles y perfil. Una contraseña vacía conserva la actual. */
    @PutMapping("/{usuarioId}")
    @PreAuthorize(GESTORES)
    public ResponseEntity<UsuarioDTOResponse> actualizar(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long usuarioId, @Valid @RequestBody ActualizarUsuarioDTORequest request) {
        return ResponseEntity.ok(UsuarioDTOResponse
                .desde(manageUsuario.actualizar(usuario.getCusuari(), usuarioId, request.toDatosUsuario())));
    }

    @PutMapping("/{usuarioId}/roles")
    @PreAuthorize(GESTORES)
    public ResponseEntity<UsuarioDTOResponse> asignarRoles(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long usuarioId, @Valid @RequestBody AsignarRolesDTORequest request) {
        return ResponseEntity.ok(UsuarioDTOResponse
                .desde(manageUsuario.asignarRoles(usuario.getCusuari(), usuarioId, request.roles())));
    }

    /**
     * Cambia la contraseña del usuario autenticado; exige la contraseña actual.
     * Las demás sesiones del usuario se cierran y se devuelve un token nuevo para
     * seguir en esta.
     */
    @PutMapping("/me/password")
    public ResponseEntity<AuthDTOResponse> cambiarPassword(@AuthenticationPrincipal UserEntity usuario,
            @Valid @RequestBody CambiarPasswordDTORequest request) {
        return ResponseEntity.ok(new AuthDTOResponse(
                manageUsuario.cambiarPassword(usuario.getCusuari(), request.passwordActual(), request.passwordNueva())));
    }

    /** Activa o inactiva un usuario. Un usuario inactivo no puede iniciar sesión. */
    @PutMapping("/{usuarioId}/estado")
    @PreAuthorize(GESTORES)
    public ResponseEntity<UsuarioDTOResponse> cambiarEstado(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long usuarioId, @Valid @RequestBody CambiarEstadoDTORequest request) {
        return ResponseEntity.ok(UsuarioDTOResponse
                .desde(manageUsuario.cambiarEstado(usuario.getCusuari(), usuarioId, request.activo())));
    }
}
