package com.sgtg.backend.infraestructure.output.auth;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageAuthServiceGatewayIntPort;
import com.sgtg.backend.domain.models.Usuario;
import com.sgtg.backend.domain.models.acceso.PoliticaPassword;
import com.sgtg.backend.infraestructure.mapper.MapperUsuarioAuth;
import com.sgtg.backend.infraestructure.output.percistence.entities.CredencialEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.HistorialPasswordEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.CredencialRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.HistorialPasswordRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implementación del servicio para la autenticación de usuarios: verificación
 * de credenciales, generación de tokens, cambio de contraseña y registro. Las
 * contraseñas se guardan en {@code credenciales}, separadas de los datos del
 * usuario, y las anteriores en {@code historial_passwords}.
 */
@Service
@RequiredArgsConstructor
public class AuthService implements ManageAuthServiceGatewayIntPort {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    // Acceso a datos y servicios necesarios para la autenticación
    private final UserRepository userRepository;
    private final CredencialRepository credencialRepository;
    private final HistorialPasswordRepository historialRepository;
    // Servicio para generar tokens JWT
    private final JwtService jwtService;
    // Codificador de contraseñas para asegurar el almacenamiento de las mismas
    private final PasswordEncoder passwordEncoder;

    /**
     * Si la contraseña que asigna un administrador debe cambiarse en el primer
     * ingreso. Solo las pruebas lo desactivan.
     */
    @Value("${sgtg.seguridad.cambio-password-obligatorio:true}")
    private boolean cambioObligatorio;

    /**
     * Hash con el que se compara la contraseña cuando el correo no existe, para
     * que la respuesta tarde lo mismo y no revele qué correos están registrados
     */
    private volatile String hashSinUsuario;

    /**
     * Un hash con un costo de BCrypt menor al configurado (de antes de subirlo)
     * se reemplaza por uno nuevo cuando la contraseña resulta correcta.
     */
    @Override
    @Transactional
    public boolean verificarCredenciales(String email, String password) {
        Optional<CredencialEntity> credencial = userRepository.findByEmail(email)
                .flatMap(user -> credencialRepository.findById(user.getCusuari()));
        boolean coincide = passwordEncoder.matches(password,
                credencial.map(CredencialEntity::getPasswordHash).orElseGet(this::hashSinUsuario));
        if (credencial.isEmpty() || !coincide)
            return false;
        if (passwordEncoder.upgradeEncoding(credencial.get().getPasswordHash()))
            credencial.get().setPasswordHash(passwordEncoder.encode(password));
        return true;
    }

    private String hashSinUsuario() {
        if (hashSinUsuario == null)
            hashSinUsuario = passwordEncoder.encode("usuario-inexistente");
        return hashSinUsuario;
    }

    @Override
    public String generarToken(String email) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        CredencialEntity credencial = credencialRepository.findById(user.getCusuari()).orElseThrow();
        return jwtService.getToken(user, credencial.getVersion(), credencial.isDebeCambiar());
    }

    @Override
    public void cerrarSesion(String token) {
        jwtService.cerrarSesion(token);
    }

    @Override
    public int depurarSesionesCerradas() {
        return jwtService.depurarSesionesCerradas();
    }

    @Override
    public boolean esPasswordReciente(Long usuarioId, String password) {
        CredencialEntity credencial = credencialRepository.findById(usuarioId).orElseThrow();
        if (passwordEncoder.matches(password, credencial.getPasswordHash()))
            return true;
        return historialRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuarioId).stream()
                .limit(PoliticaPassword.CONTRASENAS_RECORDADAS - 1L)
                .anyMatch(anterior -> passwordEncoder.matches(password, anterior.getPasswordHash()));
    }

    @Override
    @Transactional
    public String cambiarPassword(Long usuarioId, String password) {
        guardarPassword(usuarioId, password, false);
        return generarToken(userRepository.findById(usuarioId).orElseThrow().getEmail());
    }

    @Override
    @Transactional
    public void asignarPassword(Long usuarioId, String password) {
        guardarPassword(usuarioId, password, cambioObligatorio);
    }

    /**
     * Guarda la contraseña nueva, pasa la anterior al historial (sin guardar más
     * de las necesarias) y aumenta la versión de las credenciales para que los
     * tokens emitidos antes dejen de servir.
     */
    private void guardarPassword(Long usuarioId, String password, boolean debeCambiar) {
        CredencialEntity credencial = credencialRepository.findById(usuarioId).orElseThrow();
        LocalDateTime ahora = LocalDateTime.now(ZONA);
        historialRepository.save(new HistorialPasswordEntity(usuarioId, credencial.getPasswordHash(), ahora));
        List<HistorialPasswordEntity> historial = historialRepository
                .findByUsuarioIdOrderByFechaDescIdDesc(usuarioId);
        if (historial.size() >= PoliticaPassword.CONTRASENAS_RECORDADAS)
            historialRepository.deleteAll(
                    historial.subList(PoliticaPassword.CONTRASENAS_RECORDADAS - 1, historial.size()));
        credencial.setPasswordHash(passwordEncoder.encode(password));
        credencial.setFechaCambio(ahora);
        credencial.setDebeCambiar(debeCambiar);
        credencial.setVersion(credencial.getVersion() + 1);
        credencialRepository.save(credencial);
    }

    @Override
    @Transactional
    public boolean register(Usuario registerRequest, String password) {
        UserEntity user = userRepository.save(MapperUsuarioAuth.toEntity(registerRequest));
        credencialRepository.save(CredencialEntity.builder()
                .usuario(user)
                .passwordHash(passwordEncoder.encode(password))
                .fechaCambio(LocalDateTime.now(ZONA))
                .debeCambiar(cambioObligatorio)
                .version(0)
                .build());
        return true;
    }

}
