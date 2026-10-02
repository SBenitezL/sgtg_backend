package com.sgtg.backend.infraestructure.input.perfil.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.perfil.ManagePerfilCUIntPort;
import com.sgtg.backend.infraestructure.input.perfil.dto.input.PerfilDocenteDTORequest;
import com.sgtg.backend.infraestructure.input.perfil.dto.input.PerfilEstudianteDTORequest;
import com.sgtg.backend.infraestructure.input.perfil.dto.output.PerfilDocenteDTOResponse;
import com.sgtg.backend.infraestructure.input.perfil.dto.output.PerfilEstudianteDTOResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Perfiles académicos: el administrador los registra o actualiza (PUT crea o
 * reemplaza); cualquier usuario autenticado los consulta.
 */
@RestController
@RequestMapping("/api/v1/sgtg/perfiles")
@RequiredArgsConstructor
public class PerfilController {

    private final ManagePerfilCUIntPort managePerfil;

    @PutMapping("/docentes/{usuarioId}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<PerfilDocenteDTOResponse> registrarDocente(@PathVariable Long usuarioId,
            @Valid @RequestBody PerfilDocenteDTORequest request) {
        return ResponseEntity.ok(PerfilDocenteDTOResponse.desde(
                managePerfil.registrarPerfilDocente(usuarioId, request.tipoVinculacion(), request.departamentoId())));
    }

    @GetMapping("/docentes/{usuarioId}")
    public ResponseEntity<PerfilDocenteDTOResponse> consultarDocente(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(PerfilDocenteDTOResponse.desde(managePerfil.consultarPerfilDocente(usuarioId)));
    }

    @PutMapping("/estudiantes/{usuarioId}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<PerfilEstudianteDTOResponse> registrarEstudiante(@PathVariable Long usuarioId,
            @Valid @RequestBody PerfilEstudianteDTORequest request) {
        return ResponseEntity.ok(PerfilEstudianteDTOResponse
                .desde(managePerfil.registrarPerfilEstudiante(usuarioId, request.programaId())));
    }

    @GetMapping("/estudiantes/{usuarioId}")
    public ResponseEntity<PerfilEstudianteDTOResponse> consultarEstudiante(@PathVariable Long usuarioId) {
        return ResponseEntity
                .ok(PerfilEstudianteDTOResponse.desde(managePerfil.consultarPerfilEstudiante(usuarioId)));
    }
}
