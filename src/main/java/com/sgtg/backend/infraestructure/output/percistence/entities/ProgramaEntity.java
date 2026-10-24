package com.sgtg.backend.infraestructure.output.percistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "programas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    /**
     * Departamento al que está adscrito. La columna admite null para que
     * {@code ddl-auto=update} pueda agregarla a los programas ya registrados;
     * los nuevos lo exigen en el caso de uso.
     */
    @ManyToOne
    @JoinColumn(name = "departamento_id")
    private DepartamentoEntity departamento;
}
