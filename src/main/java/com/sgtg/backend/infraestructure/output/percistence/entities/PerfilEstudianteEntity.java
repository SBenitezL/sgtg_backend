package com.sgtg.backend.infraestructure.output.percistence.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Perfil de estudiante: uno por usuario, identificado por el id del usuario. */
@Entity
@Table(name = "perfiles_estudiante")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilEstudianteEntity {
    @Id
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "programa_id")
    private ProgramaEntity programa;
}
