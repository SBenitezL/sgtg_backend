package com.sgtg.backend.infraestructure.input.reloj.controller;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;
import com.sgtg.backend.infraestructure.configuration.RelojAjustable;
import com.sgtg.backend.infraestructure.input.reloj.dto.RelojDTO.AvanzarDTORequest;
import com.sgtg.backend.infraestructure.input.reloj.dto.RelojDTO.RelojDTOResponse;
import com.sgtg.backend.infraestructure.input.reloj.dto.RelojDTO.VencimientosDTOResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Reloj de desarrollo: permite adelantar la fecha de la aplicación y ejecutar
 * la verificación diaria de vencimientos para probar (ej. con Thunder Client)
 * flujos que dependen del paso del tiempo. Solo existe con
 * {@code sgtg.reloj.ajustable=true}; nunca debe activarse en producción. Solo
 * el ADMINISTRADOR lo usa.
 */
@RestController
@RequestMapping("/api/v1/sgtg/reloj")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sgtg.reloj.ajustable", havingValue = "true")
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class RelojController {

    private final RelojAjustable reloj;
    private final ManageTrabajoGradoCUIntPort manageTrabajoGrado;

    @GetMapping
    public ResponseEntity<RelojDTOResponse> consultar() {
        return ResponseEntity.ok(estado());
    }

    /** Adelanta el reloj la cantidad de días indicada. */
    @PostMapping("/avanzar")
    public ResponseEntity<RelojDTOResponse> avanzar(@Valid @RequestBody AvanzarDTORequest request) {
        reloj.avanzarDias(request.dias());
        return ResponseEntity.ok(estado());
    }

    /** Vuelve a la fecha real. */
    @DeleteMapping
    public ResponseEntity<RelojDTOResponse> restablecer() {
        reloj.restablecer();
        return ResponseEntity.ok(estado());
    }

    /** Ejecuta ahora la verificación diaria de vencimientos (la misma de la tarea programada). */
    @PostMapping("/tareas/vencimientos")
    public ResponseEntity<VencimientosDTOResponse> verificarVencimientos() {
        return ResponseEntity.ok(new VencimientosDTOResponse(manageTrabajoGrado.verificarVencimientos()));
    }

    private RelojDTOResponse estado() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return new RelojDTOResponse(ahora.toLocalDate(), ahora, reloj.getDesplazamientoDias());
    }
}
