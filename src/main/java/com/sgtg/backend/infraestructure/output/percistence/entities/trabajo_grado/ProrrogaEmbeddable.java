package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Prórroga del trabajo de grado con la decisión del Consejo (null mientras
 * esté pendiente). Se guarda en columnas {@code prorroga_*} de la tabla del
 * trabajo; todas nulas si no se ha solicitado.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProrrogaEmbeddable {
    @Column(name = "prorroga_meses")
    private Integer meses;
    @Column(name = "prorroga_ruta_solicitud", length = 1000)
    private String rutaSolicitud;
    @Column(name = "prorroga_fecha_solicitud")
    private LocalDate fechaSolicitud;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "organo", column = @Column(name = "prorroga_decision_organo")),
            @AttributeOverride(name = "resultado", column = @Column(name = "prorroga_decision_resultado")),
            @AttributeOverride(name = "acta", column = @Column(name = "prorroga_decision_acta")),
            @AttributeOverride(name = "rutaArchivo",
                    column = @Column(name = "prorroga_decision_ruta_archivo", length = 1000)),
            @AttributeOverride(name = "observaciones",
                    column = @Column(name = "prorroga_decision_observaciones", length = 2000)),
            @AttributeOverride(name = "fecha", column = @Column(name = "prorroga_decision_fecha")) })
    private DecisionEmbeddable decision;
}
