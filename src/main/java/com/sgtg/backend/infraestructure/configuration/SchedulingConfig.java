package com.sgtg.backend.infraestructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (ej. verificación diaria de vencimientos). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
