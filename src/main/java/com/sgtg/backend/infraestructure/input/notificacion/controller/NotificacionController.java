package com.sgtg.backend.infraestructure.input.notificacion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.notificacion.ManageNotificacionCUIntPort;
import com.sgtg.backend.infraestructure.input.notificacion.dto.NotificacionDTOResponse;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

import lombok.RequiredArgsConstructor;

/** Buzón de notificaciones del usuario autenticado. */
@RestController
@RequestMapping("/api/v1/sgtg/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final ManageNotificacionCUIntPort manageNotificacion;

    /** Notificaciones del usuario, de la más reciente a la más antigua. */
    @GetMapping
    public ResponseEntity<List<NotificacionDTOResponse>> listarMias(@AuthenticationPrincipal UserEntity usuario) {
        return ResponseEntity.ok(manageNotificacion.listarMias(usuario.getCusuari()).stream()
                .map(NotificacionDTOResponse::desde).toList());
    }

    @PatchMapping("/{notificacionId}/leida")
    public ResponseEntity<NotificacionDTOResponse> marcarLeida(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long notificacionId) {
        return ResponseEntity.ok(NotificacionDTOResponse.desde(
                manageNotificacion.marcarLeida(notificacionId, usuario.getCusuari())));
    }

    @PatchMapping("/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal UserEntity usuario) {
        manageNotificacion.marcarTodasLeidas(usuario.getCusuari());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificacionId}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long notificacionId) {
        manageNotificacion.eliminar(notificacionId, usuario.getCusuari());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminarTodas(@AuthenticationPrincipal UserEntity usuario) {
        manageNotificacion.eliminarTodas(usuario.getCusuari());
        return ResponseEntity.noContent().build();
    }
}
