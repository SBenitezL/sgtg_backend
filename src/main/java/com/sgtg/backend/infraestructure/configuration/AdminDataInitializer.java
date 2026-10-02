package com.sgtg.backend.infraestructure.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity.RoleType;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.RoleRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;
import com.sgtg.backend.domain.utils.CheckString;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea el usuario administrador inicial a partir de las variables de entorno
 * SGTG_ADMIN_EMAIL y SGTG_ADMIN_PASSWORD (y opcionalmente SGTG_ADMIN_DOCUMENTO,
 * su número de cédula). Como no hay registro público y solo un administrador
 * crea usuarios, es la forma de obtener el primer administrador.
 * Si las variables no están definidas o el usuario ya existe, no hace nada.
 * Se ejecuta después de la precarga de roles.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sgtg.admin.email:}")
    private String email;

    @Value("${sgtg.admin.password:}")
    private String password;

    /** Número de cédula del administrador inicial */
    @Value("${sgtg.admin.documento:1000000000}")
    private String documento;

    @Override
    public void run(String... args) {
        if (CheckString.isBlank(email) || CheckString.isBlank(password)) {
            log.info("No se definió SGTG_ADMIN_EMAIL / SGTG_ADMIN_PASSWORD: no se crea administrador inicial");
            return;
        }
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("El administrador inicial ya existe");
            return;
        }
        UserEntity admin = UserEntity.builder()
                .codigo("ADMIN")
                .tipoDocumento(TipoDocumentoIdentidad.CC)
                .numeroDocumento(documento)
                .email(email)
                .password(passwordEncoder.encode(password))
                .primerNombre("Administrador")
                .primerApellido("SGTG")
                .role(List.of(roleRepository.findByTrole(RoleType.ADMINISTRADOR).orElseThrow()))
                .build();
        userRepository.save(admin);
        log.info("Administrador inicial creado: {}", email);
    }
}
