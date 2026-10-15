package com.sgtg.backend.domain.models.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.participantes.Evaluador;

/**
 * Estado persistido de la sustentación, usado para reconstruirla.
 *
 * @param rutaDocumentoFinal       ruta del documento final (formato E)
 * @param rutaPazYSalvo            ruta del paz y salvo (formato F)
 * @param rutaConceptoAsesor       ruta del concepto favorable del asesor, solo práctica profesional
 * @param fechaSolicitud           fecha de la solicitud
 * @param estado                   estado actual
 * @param sesiones                 sesiones programadas, en orden
 * @param calificaciones           calificaciones de los jurados, en orden
 * @param inasistencias            inasistencias registradas, en orden
 * @param verificaciones           verificaciones de ajustes, en orden
 * @param rutaAjustes              ruta del documento con los ajustes, null si no se han radicado
 * @param fechaAjustes             fecha de radicación de los ajustes
 * @param fechaLimiteAjustes       fecha límite para radicar los ajustes
 * @param fechaLimiteSegundaSesion fecha límite de la segunda sesión tras un aplazamiento
 * @param rutaVersionDefinitiva    ruta de la versión definitiva (formato H)
 * @param fechaVersionDefinitiva   fecha en que se cargó la versión definitiva
 * @param juradoVersionDefinitiva  jurado que cargó la versión definitiva
 * @param motivoNoAprobacion       motivo del cierre si no se aprobó
 * @param rutaMonografia           ruta de la monografía que acompaña al formato E
 * @param rutaAnexos               ruta de los anexos de la solicitud, null si no se adjuntaron
 * @param rutaAnexosAjustes        ruta de los anexos de los ajustes, null si no se adjuntaron
 * @param solicitudesNuevaFecha    solicitudes de nueva fecha de sesión, en orden
 */
public record DatosSustentacion(String rutaDocumentoFinal, String rutaPazYSalvo, String rutaConceptoAsesor,
        LocalDate fechaSolicitud, EstadoSustentacion estado, List<SesionSustentacion> sesiones,
        List<CalificacionJurado> calificaciones, List<RegistroJurado> inasistencias,
        List<RegistroJurado> verificaciones, String rutaAjustes, LocalDate fechaAjustes,
        LocalDate fechaLimiteAjustes, LocalDate fechaLimiteSegundaSesion, String rutaVersionDefinitiva,
        LocalDate fechaVersionDefinitiva, Evaluador juradoVersionDefinitiva, MotivoCierre motivoNoAprobacion,
        String rutaMonografia, String rutaAnexos, String rutaAnexosAjustes,
        List<SolicitudNuevaFecha> solicitudesNuevaFecha) {

    public DatosSustentacion {
        if (rutaDocumentoFinal == null || rutaPazYSalvo == null || fechaSolicitud == null || estado == null)
            throw new ReglaNegocioException("error.dominio.sustentacion.solicitud_incompleta");
        sesiones = sesiones == null ? List.of() : List.copyOf(sesiones);
        calificaciones = calificaciones == null ? List.of() : List.copyOf(calificaciones);
        inasistencias = inasistencias == null ? List.of() : List.copyOf(inasistencias);
        verificaciones = verificaciones == null ? List.of() : List.copyOf(verificaciones);
        solicitudesNuevaFecha = solicitudesNuevaFecha == null ? List.of() : List.copyOf(solicitudesNuevaFecha);
    }

    /** Estado sin monografía, anexos ni solicitudes de nueva fecha (compatibilidad). */
    public DatosSustentacion(String rutaDocumentoFinal, String rutaPazYSalvo, String rutaConceptoAsesor,
            LocalDate fechaSolicitud, EstadoSustentacion estado, List<SesionSustentacion> sesiones,
            List<CalificacionJurado> calificaciones, List<RegistroJurado> inasistencias,
            List<RegistroJurado> verificaciones, String rutaAjustes, LocalDate fechaAjustes,
            LocalDate fechaLimiteAjustes, LocalDate fechaLimiteSegundaSesion, String rutaVersionDefinitiva,
            LocalDate fechaVersionDefinitiva, Evaluador juradoVersionDefinitiva, MotivoCierre motivoNoAprobacion) {
        this(rutaDocumentoFinal, rutaPazYSalvo, rutaConceptoAsesor, fechaSolicitud, estado, sesiones, calificaciones,
                inasistencias, verificaciones, rutaAjustes, fechaAjustes, fechaLimiteAjustes, fechaLimiteSegundaSesion,
                rutaVersionDefinitiva, fechaVersionDefinitiva, juradoVersionDefinitiva, motivoNoAprobacion, null, null,
                null, List.of());
    }
}
