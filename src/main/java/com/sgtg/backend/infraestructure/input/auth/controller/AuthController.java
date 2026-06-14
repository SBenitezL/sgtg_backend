package com.sgtg.backend.infraestructure.input.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgtg.backend.infraestructure.input.auth.dto.input.LoginDTORequest;
import com.sgtg.backend.infraestructure.input.auth.dto.output.AuthDTOResponse;
import com.sgtg.backend.infraestructure.output.auth.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/sgtg/auth/")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("login")
    public ResponseEntity<AuthDTOResponse> login(@RequestBody LoginDTORequest loginRequest) {
        return ResponseEntity
                .ok(new AuthDTOResponse(authService.login(loginRequest.getEmail(), loginRequest.getPassword())));
    }

    @GetMapping("")
    public ResponseEntity<String> helloWorld() {
        return new ResponseEntity<>("Hello, World!", HttpStatus.OK);
    }
}
