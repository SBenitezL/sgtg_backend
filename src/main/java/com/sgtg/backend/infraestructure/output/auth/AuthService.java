package com.sgtg.backend.infraestructure.output.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.infraestructure.mapper.MapperUsuarioAuth;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementación del servicio para la autenticación de usuarios: verificación
 * de credenciales, generación de tokens, cambio de contraseña y registro.
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
    /**
     * Hash con el que se compara la contraseña cuando el correo no existe, para
     * que la respuesta tarde lo mismo y no revele qué correos están registrados
     */
    private volatile String hashSinUsuario;

    @Override
    public boolean verificarCredenciales(String email, String password) {
        Optional<UserEntity> user = userRepository.findByEmail(email);
        boolean coincide = passwordEncoder.matches(password,
                user.map(UserEntity::getPassword).orElseGet(this::hashSinUsuario));
        return user.isPresent() && coincide;
    }

    private String hashSinUsuario() {
        if (hashSinUsuario == null)
            hashSinUsuario = passwordEncoder.encode("usuario-inexistente");
        return hashSinUsuario;
    }

    @Override
    public String generarToken(String email) {
        return jwtService.getToken(userRepository.findByEmail(email).orElseThrow());
    }

    @Override
    @Transactional
    public void actualizarPassword(Long usuarioId, String password) {
        UserEntity user = userRepository.findById(usuarioId).orElseThrow();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Override
    public boolean register(Usuario registerRequest) {
        UserEntity user = MapperUsuarioAuth.toEntity(registerRequest);
        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user) != null;
    }

}
