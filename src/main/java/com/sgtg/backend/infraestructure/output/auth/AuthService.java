package com.sgtg.backend.infraestructure.output.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.mapper.MapperUsuarioAuth;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementación del servicio para la autenticación de usuarios, proporciona
 * funcionalidades para
 * el inicio de sesión y registro
 */
@Service
@RequiredArgsConstructor
public class AuthService implements ManageAuthServiceGatewayIntPort {
    // Acceso a datos y servicios necesarios para la autenticación
    private final UserRepository userRepository;
    // Servicio para generar tokens JWT
    private final JwtService jwtService;
    // Codificador de contraseñas para asegurar el almacenamiento de las mismas
    private final PasswordEncoder passwordEncoder;
    // Componente de Spring Security para manejar la autenticación
    private final AuthenticationManager authenticationManager;

    public String login(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        UserDetails user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return jwtService.getToken(user);
    }

    public boolean register(Usuario registerRequest) {
        UserEntity user = MapperUsuarioAuth.toEntity(registerRequest);
        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user) != null;
    }

}
