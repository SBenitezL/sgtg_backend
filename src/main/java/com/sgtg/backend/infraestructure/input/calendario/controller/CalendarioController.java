package com.sgtg.backend.infraestructure.input.calendario.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.calendario.ManageCalendarioCUIntPort;
import com.sgtg.backend.infraestructure.input.calendario.dto.input.DiaNoHabilDTORequest;
import com.sgtg.backend.infraestructure.input.calendario.dto.output.DiaNoHabilDTOResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Calendario de días no hábiles: festivos de ley (calculados) y días no
 * hábiles institucionales (registrados por el administrador).
 */
@RestController
@RequestMapping("/api/v1/sgtg/calendario")
@RequiredArgsConstructor
public class CalendarioController {

    private final ManageCalendarioCUIntPort manageCalendario;

    @GetMapping("/festivos")
    public ResponseEntity<List<LocalDate>> festivos(@RequestParam int anio) {
        return ResponseEntity.ok(manageCalendario.festivos(anio).stream().sorted().toList());
    }

    @GetMapping("/dias-no-habiles")
    public ResponseEntity<List<DiaNoHabilDTOResponse>> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(manageCalendario.listarDiasNoHabiles(desde, hasta).stream()
                .map(DiaNoHabilDTOResponse::desde).toList());
    }

    @PostMapping("/dias-no-habiles")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<DiaNoHabilDTOResponse> registrar(@Valid @RequestBody DiaNoHabilDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DiaNoHabilDTOResponse
                .desde(manageCalendario.registrarDiaNoHabil(request.fecha(), request.descripcion())));
    }

    @DeleteMapping("/dias-no-habiles/{fecha}")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        manageCalendario.eliminarDiaNoHabil(fecha);
        return ResponseEntity.noContent().build();
    }
}
