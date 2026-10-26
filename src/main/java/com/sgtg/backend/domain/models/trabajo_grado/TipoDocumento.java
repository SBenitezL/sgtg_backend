package com.sgtg.backend.domain.models.trabajo_grado;

import java.util.Optional;

/**
 * Tipos de documento que se cargan durante el trabajo de grado. Los que
 * corresponden a un formato del reglamento conocen su letra, y el código
 * completo depende de la modalidad (ej. PROPUESTA → TI-A o PP-A).
 */
public enum TipoDocumento {
    PROPUESTA("A", false, "Formato A"),
    ANTEPROYECTO(null, false, "Anteproyecto"),
    EVALUACION_ANTEPROYECTO("B", false, "Formato B"),
    REMISION_CONSEJO("C", false, "Formato C"),
    /** Informe mensual de avance: no es un formato del reglamento (el formato D es la evaluación del asesor en práctica profesional) */
    INFORME_MENSUAL(null, false, "Informe mensual"),
    DOCUMENTO_FINAL("E", false, "Formato E"),
    /** Monografía del trabajo, que acompaña al formato E en la solicitud de sustentación */
    MONOGRAFIA(null, false, "Monografía"),
    /** Anexos opcionales (PDF) de la solicitud de sustentación o de los ajustes */
    ANEXOS(null, false, "Anexos"),
    PAZ_Y_SALVO("F", false, "Formato F"),
    ACTA_SUSTENTACION("G", false, "Formato G"),
    VERSION_DEFINITIVA("H", false, "Formato H"),
    CARTA_ACEPTACION(null, true, "Carta de aceptación"),
    RESOLUCION(null, false, "Resolución"),
    ACTA_COMITE(null, false, "Respuesta del Comité"),
    SOLICITUD_PRORROGA(null, false, "Solicitud de prórroga"),
    SOLICITUD_SUSPENSION(null, false, "Solicitud de suspensión"),
    REANUDACION(null, false, "Reanudación"),
    CONCEPTO_ASESOR(null, true, "Concepto del asesor"),
    /** Documento con las anotaciones del evaluador sobre el anteproyecto (opcional, acompaña al formato B) */
    ANOTACIONES_EVALUACION(null, false, "Anotaciones del evaluador"),
    /** PDF con las notas del director, codirector o asesor al revisar un informe mensual */
    REVISION_INFORME(null, false, "Notas de revisión"),
    /** Solicitud del director de una nueva fecha para la sesión de sustentación programada */
    SOLICITUD_NUEVA_FECHA(null, false, "Solicitud de nueva fecha"),
    AJUSTES_SUSTENTACION(null, false, "Ajustes de la sustentación"),
    SOLICITUD_CANCELACION(null, false, "Solicitud de cancelación");

    /** Letra del formato en el reglamento, null si no corresponde a un formato */
    private final String letraFormato;
    /** Indica si el documento solo aplica a la modalidad de práctica profesional */
    private final boolean soloPracticaProfesional;
    /** Nombre con el que se guarda el archivo (ej. "Formato A"), seguido de su versión */
    private final String nombreArchivo;

    TipoDocumento(String letraFormato, boolean soloPracticaProfesional, String nombreArchivo) {
        this.letraFormato = letraFormato;
        this.soloPracticaProfesional = soloPracticaProfesional;
        this.nombreArchivo = nombreArchivo;
    }

    /**
     * @return nombre con el que se guardan los archivos de este tipo (ej.
     *         "Formato A"); el almacenamiento le agrega la versión
     */
    public String nombreArchivo() {
        return nombreArchivo;
    }

    /**
     * @param modalidad modalidad del trabajo de grado
     * @return true si este tipo de documento se usa en la modalidad indicada
     */
    public boolean aplicaA(Modalidad modalidad) {
        return !this.soloPracticaProfesional || modalidad == Modalidad.PRACTICA_PROFESIONAL;
    }

    /**
     * @param modalidad modalidad del trabajo de grado
     * @return código del formato (ej. "TI-A"), vacío si el documento no
     *         corresponde a un formato o no aplica a la modalidad
     */
    public Optional<String> formato(Modalidad modalidad) {
        if (this.letraFormato == null || !this.aplicaA(modalidad))
            return Optional.empty();
        return Optional.of(modalidad.getPrefijoFormato() + "-" + this.letraFormato);
    }

    /**
     * @param modalidad modalidad del trabajo de grado
     * @return nombre con el que se agrupan los archivos de este tipo: el código
     *         del formato (ej. "PP-A") o, si no corresponde a un formato, el
     *         nombre del tipo (ej. "ANTEPROYECTO")
     */
    public String carpeta(Modalidad modalidad) {
        return formato(modalidad).orElse(this.name());
    }
}
