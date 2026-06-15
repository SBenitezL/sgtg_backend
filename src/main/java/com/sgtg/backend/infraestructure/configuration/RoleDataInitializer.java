package com.sgtg.backend.infraestructure.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.RoleEntity.RoleType;
import com.sgtg.backend.infraestructure.output.repositories.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando precarga de roles...");

        for (RoleType roleType : RoleType.values()) {
            if (roleRepository.findByTrole(roleType).isEmpty()) {
                RoleEntity role = RoleEntity.builder()
                        .trole(roleType)
                        .build();
                roleRepository.save(role);
                log.info("Rol creado: {}", roleType);
            } else {
                log.info("Rol ya existe: {}", roleType);
            }
        }

        log.info("Precarga de roles completada");
    }
}
