package com.sgtg.backend.infraestructure.input.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.infraestructure.input.auditoria.AuditoriaFilter;
import com.sgtg.backend.infraestructure.input.auth.dto.input.LoginDTORequest;
import com.sgtg.backend.infraestructure.input.auth.dto.output.AuthDTOResponse;
import com.sgtg.backend.infraestructure.jwt.JwtAuthenticationFilter;
import com.sgtg.backend.infraestructure.output.auth.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Autenticación pública. No hay registro público: los usuarios los crea un
 * administrador en {@code POST /api/v1/sgtg/usuarios}.
 */
@RestController
@RequestMapping("/api/v1/sgtg/auth/")
@RequiredArgsConstructor
public class AuthController {
    private final ManageAuthCUIntPort manageAuth;
    private final JwtService jwtService;

    /**
     * Inicia sesión y devuelve el token. Credenciales incorrectas, usuario
     * inactivo o correo bloqueado por intentos fallidos: 401 con el mensaje
     * correspondiente. La sesión del token nuevo queda en la auditoría.
     */
    @PostMapping("login")
    public ResponseEntity<AuthDTOResponse> login(@Valid @RequestBody LoginDTORequest loginRequest,
            HttpServletRequest request) {
        String token = manageAuth.login(loginRequest.getEmail(), loginRequest.getPassword());
        request.setAttribute(AuditoriaFilter.ATRIBUTO_SESION, jwtService.getSesion(token));
        return ResponseEntity.ok(new AuthDTOResponse(token));
    }

    /**
     * Cierra la sesión del token enviado: deja de ser aceptado aunque no haya
     * vencido. Responde 204 aunque no haya token o ya no sea válido.
     */
    @PostMapping("logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        manageAuth.logout(JwtAuthenticationFilter.tokenDe(request));
        return ResponseEntity.noContent().build();
    }
}
