package com.sgtg.backend.domain.models.trabajo_grado;

import java.util.Optional;

/**
 * Tipos de documento que se cargan durante el trabajo de grado. Los que
 * corresponden a un formato del reglamento conocen su letra, y el código
 * completo depende de la modalidad (ej. PROPUESTA → TI-A o PP-A).
 */
public enum TipoDocumento {
    PROPUESTA("A", false),
    ANTEPROYECTO(null, false),
    EVALUACION_ANTEPROYECTO("B", false),
    REMISION_CONSEJO("C", false),
    INFORME_MENSUAL("D", false),
    DOCUMENTO_FINAL("E", false),
    /** Monografía del trabajo, que acompaña al formato E en la solicitud de sustentación */
    MONOGRAFIA(null, false),
    /** Anexos opcionales (PDF) de la solicitud de sustentación o de los ajustes */
    ANEXOS(null, false),
    PAZ_Y_SALVO("F", false),
    ACTA_SUSTENTACION("G", false),
    VERSION_DEFINITIVA("H", false),
    CARTA_ACEPTACION(null, true),
    RESOLUCION(null, false),
    ACTA_COMITE(null, false),
    SOLICITUD_PRORROGA(null, false),
    SOLICITUD_SUSPENSION(null, false),
    REANUDACION(null, false),
    CONCEPTO_ASESOR(null, true),
    /** Documento con las anotaciones del evaluador sobre el anteproyecto (opcional, acompaña al formato B) */
    ANOTACIONES_EVALUACION(null, false),
    /** PDF con las notas del director, codirector o asesor al revisar un informe mensual */
    REVISION_INFORME(null, false),
    /** Solicitud del director de una nueva fecha para la sesión de sustentación programada */
    SOLICITUD_NUEVA_FECHA(null, false),
    AJUSTES_SUSTENTACION(null, false),
    SOLICITUD_CANCELACION(null, false);

    /** Letra del formato en el reglamento, null si no corresponde a un formato */
    private final String letraFormato;
    /** Indica si el documento solo aplica a la modalidad de práctica profesional */
    private final boolean soloPracticaProfesional;

    TipoDocumento(String letraFormato, boolean soloPracticaProfesional) {
        this.letraFormato = letraFormato;
        this.soloPracticaProfesional = soloPracticaProfesional;
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
