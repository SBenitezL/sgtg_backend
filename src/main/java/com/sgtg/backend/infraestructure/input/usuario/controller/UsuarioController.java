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
import com.sgtg.backend.infraestructure.input.usuario.dto.input.ActualizarUsuarioDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.AsignarRolesDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.CambiarEstadoDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.CambiarPasswordDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.CrearUsuarioDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.output.UsuarioDTOResponse;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Creación, edición, consulta, asignación de roles y activación de usuarios por
 * el administrador (con su perfil académico); listado para los usuarios
 * autenticados; consulta y cambio de contraseña del usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/sgtg/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final ManageUsuarioCUIntPort manageUsuario;

    /** Crea un usuario con sus roles. No hay registro público: solo el administrador. */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTOResponse> crear(@Valid @RequestBody CrearUsuarioDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioDTOResponse.desde(manageUsuario.crear(request.toNuevoUsuario())));
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
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTOResponse> consultar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(UsuarioDTOResponse.desde(manageUsuario.consultarFicha(usuarioId)));
    }

    /** Actualiza datos, roles y perfil. Una contraseña vacía conserva la actual. */
    @PutMapping("/{usuarioId}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTOResponse> actualizar(@PathVariable Long usuarioId,
            @Valid @RequestBody ActualizarUsuarioDTORequest request) {
        return ResponseEntity
                .ok(UsuarioDTOResponse.desde(manageUsuario.actualizar(usuarioId, request.toDatosUsuario())));
    }

    @PutMapping("/{usuarioId}/roles")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTOResponse> asignarRoles(@PathVariable Long usuarioId,
            @Valid @RequestBody AsignarRolesDTORequest request) {
        return ResponseEntity.ok(UsuarioDTOResponse.desde(manageUsuario.asignarRoles(usuarioId, request.roles())));
    }

    /** Cambia la contraseña del usuario autenticado; exige la contraseña actual. */
    @PutMapping("/me/password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal UserEntity usuario,
            @Valid @RequestBody CambiarPasswordDTORequest request) {
        manageUsuario.cambiarPassword(usuario.getCusuari(), request.passwordActual(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    /** Activa o inactiva un usuario. Un usuario inactivo no puede iniciar sesión. */
    @PutMapping("/{usuarioId}/estado")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTOResponse> cambiarEstado(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long usuarioId, @Valid @RequestBody CambiarEstadoDTORequest request) {
        return ResponseEntity.ok(UsuarioDTOResponse
                .desde(manageUsuario.cambiarEstado(usuario.getCusuari(), usuarioId, request.activo())));
    }
}
