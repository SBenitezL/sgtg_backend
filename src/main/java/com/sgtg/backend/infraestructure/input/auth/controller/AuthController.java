package com.sgtg.backend.infraestructure.input.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.application.input.auth.ManageAuthCUIntPort;
import com.sgtg.backend.infraestructure.input.auth.dto.input.LoginDTORequest;
import com.sgtg.backend.infraestructure.input.auth.dto.output.AuthDTOResponse;

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

    /**
     * Inicia sesión y devuelve el token. Credenciales incorrectas o usuario
     * inactivo: 401 con el mensaje correspondiente.
     */
    @PostMapping("login")
    public ResponseEntity<AuthDTOResponse> login(@Valid @RequestBody LoginDTORequest loginRequest) {
        return ResponseEntity
                .ok(new AuthDTOResponse(manageAuth.login(loginRequest.getEmail(), loginRequest.getPassword())));
    }

    @GetMapping("")
    public ResponseEntity<String> helloWorld() {
        return new ResponseEntity<>("Hello, World!", HttpStatus.OK);
    }
}
