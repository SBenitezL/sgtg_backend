package com.sgtg.backend.infraestructure.configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.domain.models.TipoDocumentoIdentidad;
import com.sgtg.backend.infraestructure.output.percistence.entities.CredencialEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity.RoleType;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.CredencialRepository;
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
 * Se ejecuta después de la precarga de roles. Como la contraseña la define quien
 * despliega el sistema, no se exige cambiarla en el primer ingreso.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${sgtg.admin.email:}")
    private String email;

    @Value("${sgtg.admin.password:}")
    private String password;

    /** Número de cédula del administrador inicial */
    @Value("${sgtg.admin.documento:1000000000}")
    private String documento;

    /** En una transacción: las credenciales se guardan con el usuario recién creado */
    @Override
    @Transactional
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
                .primerNombre("Administrador")
                .primerApellido("SGTG")
                .role(List.of(roleRepository.findByTrole(RoleType.ADMINISTRADOR).orElseThrow()))
                .build();
        admin = userRepository.save(admin);
        credencialRepository.save(CredencialEntity.builder()
                .usuario(admin)
                .passwordHash(passwordEncoder.encode(password))
                .fechaCambio(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .debeCambiar(false)
                .version(0)
                .build());
        log.info("Administrador inicial creado: {}", email);
    }
}
