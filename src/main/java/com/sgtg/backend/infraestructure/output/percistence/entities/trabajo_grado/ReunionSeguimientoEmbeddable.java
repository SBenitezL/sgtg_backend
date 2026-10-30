package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reunión de seguimiento de un mes del plazo, agendada por el director. */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionSeguimientoEmbeddable {
    @Column(nullable = false)
    private Integer numero;
    @Column(nullable = false)
    private LocalDateTime fechaHora;
    @Column(nullable = false, length = 500)
    private String lugar;
    /** Código del director que la agendó */
    private String agendadaPor;
    @Column(nullable = false)
    private boolean realizada;
    @Column(length = 2000)
    private String observaciones;
}
