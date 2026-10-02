package com.sgtg.backend.infraestructure.output.percistence.entities;

import com.sgtg.backend.domain.models.participantes.TipoVinculacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Perfil docente: uno por usuario, identificado por el id del usuario. */
@Entity
@Table(name = "perfiles_docente")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilDocenteEntity {
    @Id
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVinculacion tipoVinculacion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "departamento_id")
    private DepartamentoEntity departamento;
}
