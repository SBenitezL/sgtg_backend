package com.sgtg.backend.infraestructure.input.usuario.controller;

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
import com.sgtg.backend.infraestructure.input.usuario.dto.input.AsignarRolesDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.input.CrearUsuarioDTORequest;
import com.sgtg.backend.infraestructure.input.usuario.dto.output.UsuarioDTOResponse;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Creación de usuarios, consulta y asignación de roles por el administrador. */
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

    /** Datos del usuario autenticado. */
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTOResponse> consultarActual(@AuthenticationPrincipal UserEntity usuario) {
        return ResponseEntity.ok(UsuarioDTOResponse.desde(manageUsuario.consultar(usuario.getCusuari())));
    }

    @GetMapping("/{usuarioId}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTOResponse> consultar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(UsuarioDTOResponse.desde(manageUsuario.consultar(usuarioId)));
    }

    @PutMapping("/{usuarioId}/roles")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTOResponse> asignarRoles(@PathVariable Long usuarioId,
            @Valid @RequestBody AsignarRolesDTORequest request) {
        return ResponseEntity.ok(UsuarioDTOResponse.desde(manageUsuario.asignarRoles(usuarioId, request.roles())));
    }
}
