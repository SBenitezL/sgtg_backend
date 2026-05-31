package com.sgtg.backend.infraestructure.output.auth;

import org.springframework.stereotype.Service;

import com.sgtg.backend.infraestructure.input.auth.dto.input.LoginDTORequest;
import com.sgtg.backend.infraestructure.input.auth.dto.input.RegisterDTORequest;
import com.sgtg.backend.infraestructure.input.auth.dto.output.AuthDTOResponse;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Servicio para la autenticación de usuarios, proporciona funcionalidades para
 * el inicio de sesión y registro
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthDTOResponse login(LoginDTORequest loginRequest) {
        return null;
    }

    public AuthDTOResponse register(RegisterDTORequest registerRequest) {
        UserEntity user = UserEntity.builder()
                .codigo(registerRequest.getCodigo())
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .role(RoleEntity.valueOf(registerRequest.getRole()))
                .nombres(registerRequest.getNombres())
                .apellidos(registerRequest.getApellidos())
                .build();
        userRepository.save(user);
        return AuthDTOResponse.builder().token(jwtService.getToken(user)).build();
    }

}
