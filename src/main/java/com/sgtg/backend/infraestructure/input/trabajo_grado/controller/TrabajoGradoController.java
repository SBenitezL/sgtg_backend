package com.sgtg.backend.infraestructure.input.trabajo_grado.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.CrearPracticaProfesionalDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.CrearTrabajoInvestigacionDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.DecisionComiteDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.RadicarDocumentoDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.output.TrabajoGradoDTOResponse;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Trabajos de grado (Fase 1: creación y propuesta). Los permisos por rol se
 * declaran aquí; los que dependen del trabajo (ser su director o participante)
 * los valida el caso de uso.
 */
@RestController
@RequestMapping("/api/v1/sgtg/trabajos-grado")
@RequiredArgsConstructor
public class TrabajoGradoController {

    private final ManageTrabajoGradoCUIntPort manageTrabajoGrado;

    /** Crea un trabajo de investigación; el docente autenticado queda como director. */
    @PostMapping("/investigacion")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<TrabajoGradoDTOResponse> crearInvestigacion(@AuthenticationPrincipal UserEntity usuario,
            @Valid @RequestBody CrearTrabajoInvestigacionDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TrabajoGradoDTOResponse
                .desde(manageTrabajoGrado.crear(usuario.getCusuari(), request.toNuevoTrabajo())));
    }

    /** Crea una práctica profesional; el docente autenticado queda como director. */
    @PostMapping("/practica-profesional")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<TrabajoGradoDTOResponse> crearPracticaProfesional(
            @AuthenticationPrincipal UserEntity usuario,
            @Valid @RequestBody CrearPracticaProfesionalDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TrabajoGradoDTOResponse
                .desde(manageTrabajoGrado.crear(usuario.getCusuari(), request.toNuevoTrabajo())));
    }

    /** Trabajos en los que participa el usuario autenticado. */
    @GetMapping("/mis-trabajos")
    public ResponseEntity<List<TrabajoGradoDTOResponse>> listarMisTrabajos(
            @AuthenticationPrincipal UserEntity usuario) {
        return ResponseEntity.ok(manageTrabajoGrado.listarMisTrabajos(usuario.getCusuari()).stream()
                .map(TrabajoGradoDTOResponse::desde).toList());
    }

    @GetMapping("/{trabajoId}")
    public ResponseEntity<TrabajoGradoDTOResponse> consultar(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId) {
        return ResponseEntity
                .ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.consultar(trabajoId, usuario.getCusuari())));
    }

    /** Radica una versión de la propuesta (formato A). Solo el director del trabajo. */
    @PostMapping("/{trabajoId}/propuesta")
    public ResponseEntity<TrabajoGradoDTOResponse> radicarPropuesta(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestBody RadicarDocumentoDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse
                .desde(manageTrabajoGrado.radicarPropuesta(trabajoId, usuario.getCusuari(), request.url())));
    }

    /** Registra el concepto del Comité de Programa sobre la propuesta. */
    @PostMapping("/{trabajoId}/propuesta/decisiones")
    @PreAuthorize("hasAuthority('COMITE')")
    public ResponseEntity<TrabajoGradoDTOResponse> registrarDecisionComite(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestBody DecisionComiteDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.registrarDecisionComite(trabajoId,
                usuario.getCusuari(), request.resultado(), request.acta(), request.rutaArchivo(),
                request.observaciones(), request.fecha())));
    }
}
