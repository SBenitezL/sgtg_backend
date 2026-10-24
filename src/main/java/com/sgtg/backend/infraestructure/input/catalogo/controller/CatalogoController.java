package com.sgtg.backend.infraestructure.input.catalogo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.catalogo.ManageCatalogoCUIntPort;
import com.sgtg.backend.infraestructure.input.catalogo.dto.input.NombreDTORequest;
import com.sgtg.backend.infraestructure.input.catalogo.dto.input.ProgramaDTORequest;
import com.sgtg.backend.infraestructure.input.catalogo.dto.output.CatalogoDTOResponse;
import com.sgtg.backend.infraestructure.input.catalogo.dto.output.ProgramaDTOResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Catálogos de departamentos y programas: cualquier usuario consulta, el administrador registra. */
@RestController
@RequestMapping("/api/v1/sgtg/catalogos")
@RequiredArgsConstructor
public class CatalogoController {

    private final ManageCatalogoCUIntPort manageCatalogo;

    @GetMapping("/departamentos")
    public ResponseEntity<List<CatalogoDTOResponse>> listarDepartamentos() {
        return ResponseEntity
                .ok(manageCatalogo.listarDepartamentos().stream().map(CatalogoDTOResponse::desde).toList());
    }

    @PostMapping("/departamentos")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<CatalogoDTOResponse> crearDepartamento(@Valid @RequestBody NombreDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CatalogoDTOResponse.desde(manageCatalogo.crearDepartamento(request.nombre())));
    }

    @GetMapping("/programas")
    public ResponseEntity<List<ProgramaDTOResponse>> listarProgramas() {
        return ResponseEntity.ok(manageCatalogo.listarProgramas().stream().map(ProgramaDTOResponse::desde).toList());
    }

    /** Registra un programa adscrito a un departamento. */
    @PostMapping("/programas")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<ProgramaDTOResponse> crearPrograma(@Valid @RequestBody ProgramaDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProgramaDTOResponse
                .desde(manageCatalogo.crearPrograma(request.nombre(), request.departamentoId())));
    }
}
