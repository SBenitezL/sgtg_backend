package com.sgtg.backend.domain.models.trabajo_grado;

import java.util.List;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.utils.CheckString;

/**
 * Trabajo de grado en la modalidad de Práctica Profesional (Capítulo III). Es
 * individual (Art. 29) y requiere un asesor de la organización y la carta de
 * aceptación expedida por ella (Art. 32).
 */
public class PracticaProfesional extends TrabajoGrado {
    /** Carta de aceptación de la organización, en la que figura el asesor */
    private Documento cartaAceptacion;

    /**
     * Crea una práctica profesional en la etapa de propuesta.
     *
     * @param estudiante      único estudiante que realiza la práctica
     * @param codirector      obligatorio si el director no es de planta, null si no aplica
     * @param asesor          asesor de la organización, obligatorio
     * @param cartaAceptacion documento de tipo {@link TipoDocumento#CARTA_ACEPTACION}
     */
    public PracticaProfesional(Long id, String titulo, List<String> objetivos, Estudiante estudiante,
            Director director, Director codirector, Asesor asesor, Documento cartaAceptacion) {
        super(id, titulo, objetivos, estudiante == null ? List.of() : List.of(estudiante), director, codirector,
                asesor);
        if (asesor == null || CheckString.isBlank(asesor.getOrganizacion()))
            throw new ReglaNegocioException("error.dominio.practica.asesor_obligatorio");
        if (cartaAceptacion == null || cartaAceptacion.tipo() != TipoDocumento.CARTA_ACEPTACION)
            throw new ReglaNegocioException("error.dominio.practica.carta_aceptacion_obligatoria");
        this.cartaAceptacion = cartaAceptacion;
    }

    @Override
    public Modalidad getModalidad() {
        return Modalidad.PRACTICA_PROFESIONAL;
    }

    /**
     * @return el único estudiante de la práctica
     */
    public Estudiante getEstudiante() {
        return getEstudiantes().get(0);
    }

    public Documento getCartaAceptacion() {
        return cartaAceptacion;
    }
}
