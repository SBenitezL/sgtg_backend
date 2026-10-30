package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Sesión cerrada por su usuario: su token deja de ser aceptado aunque no haya
 * vencido. Se conserva hasta que el token vence; después ya no hace falta.
 */
@Entity
@Table(name = "sesiones_cerradas")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SesionCerradaEntity {

    /** Identificador de la sesión (jti del token) */
    @Id
    @Column(length = 36)
    private String sesion;

    /** Vencimiento del token de la sesión */
    @Column(nullable = false)
    private LocalDateTime expira;
}
