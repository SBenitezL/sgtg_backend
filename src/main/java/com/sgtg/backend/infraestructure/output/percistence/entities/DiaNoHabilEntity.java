package com.sgtg.backend.infraestructure.output.percistence.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Día no hábil institucional, identificado por su fecha. */
@Entity
@Table(name = "dias_no_habiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaNoHabilEntity {
    @Id
    private LocalDate fecha;

    @Column(nullable = false)
    private String descripcion;
}
