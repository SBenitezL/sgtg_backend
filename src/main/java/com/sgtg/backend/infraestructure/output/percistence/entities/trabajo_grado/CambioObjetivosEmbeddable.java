package com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de cambio de título u objetivos con la decisión del Consejo (null
 * mientras esté pendiente). Los objetivos se guardan como JSON en columnas
 * de texto largo (@Lob), que no cuentan para el límite de tamaño de fila de
 * MySQL.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioObjetivosEmbeddable {
    @Column(length = 500)
    private String tituloAnterior;
    @Convert(converter = ListaTextoConverter.class)
    @Lob
    private List<String> objetivosAnteriores;
    @Column(length = 500)
    private String tituloPropuesto;
    @Convert(converter = ListaTextoConverter.class)
    @Lob
    private List<String> objetivosPropuestos;
    @Column(length = 2000)
    private String justificacion;
    private LocalDate fechaSolicitud;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "organo", column = @Column(name = "decision_organo")),
            @AttributeOverride(name = "resultado", column = @Column(name = "decision_resultado")),
            @AttributeOverride(name = "acta", column = @Column(name = "decision_acta")),
            @AttributeOverride(name = "rutaArchivo", column = @Column(name = "decision_ruta_archivo", length = 1000)),
            @AttributeOverride(name = "observaciones", column = @Column(name = "decision_observaciones", length = 2000)),
            @AttributeOverride(name = "fecha", column = @Column(name = "decision_fecha")),
            @AttributeOverride(name = "responsableId", column = @Column(name = "decision_responsable_id")) })
    private DecisionEmbeddable decision;
}
