package com.sgtg.backend.infraestructure.output.percistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long crole;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleType trole;

    public enum RoleType {
        ADMINISTRADOR,
        DOCENTE,
        ESTUDIANTE,
        DIRECTOR,
        CODIRECTOR,
        ASESOR,
        JEFE_DEP,
        COMITE,
        EVALUADOR,
        CONSEJO,
        JURADO,
        DECANATURA
    }

}
