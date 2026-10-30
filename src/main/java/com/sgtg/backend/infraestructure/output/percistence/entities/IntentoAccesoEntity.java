package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Inicios de sesión fallidos de un correo (exista o no la cuenta) y su bloqueo. */
@Entity
@Table(name = "intentos_acceso")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IntentoAccesoEntity {

    @Id
    @Column(length = 255)
    private String correo;

    @Column(nullable = false)
    private int fallos;

    private LocalDateTime ultimoFallo;

    private LocalDateTime bloqueadoHasta;
}
