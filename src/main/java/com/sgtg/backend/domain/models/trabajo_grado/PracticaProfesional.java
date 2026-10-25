package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
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

    /** Reconstruye la práctica con su estado persistido (ver {@link TrabajoGrado#reconstruir}). */
    PracticaProfesional(DatosTrabajoGrado datos) {
        super(datos);
        this.cartaAceptacion = datos.cartaAceptacion();
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

    /**
     * Actualiza la ruta de la carta de aceptación cuando el archivo se mueve a
     * su ubicación definitiva (la carta se carga antes de que el trabajo tenga
     * identificador). Conserva el tipo, la versión y la fecha de carga.
     *
     * @param ruta nueva ruta de la carta
     */
    public void reubicarCartaAceptacion(String ruta) {
        this.cartaAceptacion = this.cartaAceptacion.conRuta(ruta);
    }

    /**
     * Carga una nueva versión de la carta de aceptación al corregir la
     * propuesta. Solo mientras se puede radicar una versión: antes de la
     * primera radicación o después de que el Comité no la apruebe. La versión
     * anterior deja de ser la vigente.
     *
     * @param ruta ruta de la nueva carta en el almacenamiento de archivos
     * @param hoy  fecha de carga
     */
    public void actualizarCartaAceptacion(String ruta, LocalDate hoy) {
        validarEtapa(Proceso.PROPUESTA);
        if (!getPropuesta().puedeRadicar())
            throw new ReglaNegocioException("error.dominio.propuesta.edicion_no_permitida");
        if (CheckString.isBlank(ruta))
            throw new ReglaNegocioException("error.dominio.practica.carta_aceptacion_obligatoria");
        this.cartaAceptacion = this.cartaAceptacion == null
                ? Documento.inicial(TipoDocumento.CARTA_ACEPTACION, ruta, hoy)
                : this.cartaAceptacion.nuevaVersion(ruta, hoy);
    }

    public Documento getCartaAceptacion() {
        return cartaAceptacion;
    }
}
