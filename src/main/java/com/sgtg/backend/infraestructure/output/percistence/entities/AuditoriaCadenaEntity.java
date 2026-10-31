package com.sgtg.backend.infraestructure.output.percistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Final de la cadena de la auditoría (una sola fila): la última secuencia y su
 * sello. Se bloquea al registrar, para que dos registros simultáneos no tomen
 * el mismo lugar en la cadena.
 */
@Entity
@Table(name = "auditoria_cadena")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaCadenaEntity {

    /** Identificador de la única fila */
    public static final long UNICA = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private long ultimaSecuencia;

    @Column(length = 64)
    private String ultimoSello;
}
