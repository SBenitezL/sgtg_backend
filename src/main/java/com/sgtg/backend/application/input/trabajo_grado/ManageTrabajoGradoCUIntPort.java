package com.sgtg.backend.application.input.trabajo_grado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.trabajo_grado.Calificacion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;

/**
 * Puerto de entrada para la gestión del trabajo de grado (Fases 1 a 8: propuesta,
 * anteproyecto, evaluación, aprobación del Consejo de Facultad, trabajo en curso,
 * prórroga, suspensión, sustentación y cancelación). Los documentos se reciben
 * como archivos: el caso de uso los almacena y el dominio solo guarda su ruta.
 */
public interface ManageTrabajoGradoCUIntPort {

    /**
     * Crea un trabajo de grado en la etapa de propuesta. El docente que lo crea
     * queda como director (Art. 10: el profesor presenta la idea).
     *
     * @param directorUsuarioId usuario autenticado que crea el trabajo
     * @param datos             datos del trabajo
     * @return el trabajo creado con su identificador
     */
    public TrabajoGrado crear(Long directorUsuarioId, NuevoTrabajoGrado datos);

    /**
     * Consulta un trabajo. Pueden hacerlo sus participantes y los usuarios con rol
     * COMITE, JEFE_DEP, CONSEJO o ADMINISTRADOR.
     */
    public TrabajoGrado consultar(Long trabajoId, Long usuarioId);

    /**
     * Obtiene un archivo del trabajo. Pueden hacerlo los mismos usuarios que lo
     * pueden consultar; los demás reciben un error de acceso.
     *
     * @param ruta ruta del archivo, tal como la devuelve la consulta del trabajo
     * @return el archivo con su nombre, tipo y contenido
     */
    public ArchivoAdjunto obtenerArchivo(Long trabajoId, Long usuarioId, String ruta);

    /** @return los trabajos en los que participa el usuario */
    public List<TrabajoGrado> listarMisTrabajos(Long usuarioId);

    /**
     * Lista los trabajos que se encuentran en una etapa (ej. los anteproyectos
     * pendientes de evaluadores). Para los roles de consulta general; el
     * controlador restringe el acceso.
     *
     * @param etapa etapa del proceso
     * @return los trabajos en esa etapa, del más reciente al más antiguo
     */
    public List<TrabajoGrado> listarPorEtapa(Proceso etapa);

    /**
     * Lista todos los trabajos de grado, en cualquier etapa. Para los roles de
     * consulta general (el controlador restringe el acceso): pantallas de
     * seguimiento, historial y estadísticas.
     *
     * @return todos los trabajos, del más reciente al más antiguo
     */
    public List<TrabajoGrado> listarTodos();

    /**
     * Radica una versión de la propuesta. Solo el director del trabajo.
     *
     * @param documento archivo de la propuesta (formato A)
     */
    public TrabajoGrado radicarPropuesta(Long trabajoId, Long usuarioId, ArchivoAdjunto documento);

    /**
     * Corrige título, objetivos, descripción y línea de investigación de la
     * propuesta, mientras se puede radicar una versión. Solo el director.
     */
    public TrabajoGrado actualizarPropuesta(Long trabajoId, Long usuarioId, String titulo, List<String> objetivos,
            String descripcion, String lineaInvestigacion);

    /**
     * Registra el concepto del Comité de Programa sobre la propuesta.
     *
     * @param usuarioId usuario del comité que registra la decisión
     * @param resultado APROBADO o NO_APROBADO
     * @param soporte   archivo de soporte (acta del Comité), opcional
     * @param fecha     fecha de la decisión, null para usar la fecha actual
     */
    public TrabajoGrado registrarDecisionComite(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String acta, ArchivoAdjunto soporte, String observaciones, LocalDate fecha);

    /**
     * Radica una versión del anteproyecto ante el Departamento. Solo el director
     * del trabajo.
     *
     * @param documento archivo del anteproyecto
     */
    public TrabajoGrado radicarAnteproyecto(Long trabajoId, Long usuarioId, ArchivoAdjunto documento);

    /**
     * Asigna los dos evaluadores del anteproyecto. Solo el jefe del departamento
     * al que está adscrito el director del trabajo.
     *
     * @param usuarioId    jefe de departamento que asigna
     * @param evaluador1Id usuario del primer evaluador
     * @param evaluador2Id usuario del segundo evaluador
     */
    public TrabajoGrado asignarEvaluadores(Long trabajoId, Long usuarioId, Long evaluador1Id, Long evaluador2Id);

    /**
     * Registra el concepto de un evaluador asignado sobre la versión actual del
     * anteproyecto, con la fecha actual.
     *
     * @param usuarioId     evaluador que emite el concepto
     * @param resultado     APROBADO o CON_CORRECCIONES
     * @param observaciones obligatorias si pide correcciones
     * @param formato       archivo del formato B, opcional
     */
    public TrabajoGrado registrarConceptoEvaluador(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String observaciones, ArchivoAdjunto formato);

    /**
     * Registra el concepto del Consejo de Facultad sobre el anteproyecto
     * aprobado por los evaluadores. La fecha de la decisión es la actual y, si
     * aprueba, es la fecha de notificación desde la que corre el plazo.
     *
     * @param usuarioId     usuario del Consejo que registra la decisión
     * @param resultado     APROBADO o NO_APROBADO
     * @param resolucion    archivo de la resolución
     * @param observaciones comentario opcional
     */
    public TrabajoGrado registrarDecisionConsejo(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones);

    /**
     * Solicita el cambio de título u objetivos del trabajo en curso. Solo el
     * director; lo decide el Consejo de Facultad.
     */
    public TrabajoGrado solicitarCambioObjetivos(Long trabajoId, Long usuarioId, String titulo,
            List<String> objetivos, String justificacion);

    /**
     * Registra la decisión del Consejo de Facultad sobre el cambio de título u
     * objetivos solicitado, con la fecha actual.
     *
     * @param resultado     APROBADO o NO_APROBADO
     * @param resolucion    archivo de la resolución
     * @param observaciones comentario opcional
     */
    public TrabajoGrado resolverCambioObjetivos(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones);

    /**
     * Radica un informe mensual de la práctica profesional con la fecha actual.
     * Solo el director.
     *
     * @param documento archivo del informe (formato D)
     */
    public TrabajoGrado registrarInformeMensual(Long trabajoId, Long usuarioId, ArchivoAdjunto documento);

    /**
     * Solicita la prórroga del plazo con la fecha actual. Solo el director.
     *
     * @param meses     meses solicitados, de 1 a 3
     * @param solicitud archivo de la solicitud
     */
    public TrabajoGrado solicitarProrroga(Long trabajoId, Long usuarioId, int meses, ArchivoAdjunto solicitud);

    /**
     * Registra la decisión del Consejo de Facultad sobre la prórroga, con la
     * fecha actual.
     *
     * @param usuarioId     usuario del Consejo que registra la decisión
     * @param resultado     APROBADO o NO_APROBADO
     * @param resolucion    archivo de la resolución
     * @param observaciones comentario opcional
     */
    public TrabajoGrado resolverProrroga(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones);

    /**
     * Solicita la suspensión del trabajo con la fecha actual. Solo el director.
     *
     * @param solicitud archivo de la solicitud
     */
    public TrabajoGrado solicitarSuspension(Long trabajoId, Long usuarioId, ArchivoAdjunto solicitud);

    /**
     * Registra la decisión del Consejo de Facultad sobre la suspensión, con la
     * fecha actual (desde la que rige si se aprueba).
     *
     * @param usuarioId     usuario del Consejo que registra la decisión
     * @param resultado     APROBADO o NO_APROBADO
     * @param resolucion    archivo de la resolución
     * @param observaciones comentario opcional
     */
    public TrabajoGrado resolverSuspension(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones);

    /**
     * Reanuda el trabajo suspendido con la fecha actual. Solo el director.
     *
     * @param documento archivo de la reanudación
     */
    public TrabajoGrado reanudar(Long trabajoId, Long usuarioId, ArchivoAdjunto documento);

    /**
     * Solicita la sustentación con la fecha actual. Solo el director.
     *
     * @param documentoFinal documento final (formato E)
     * @param pazYSalvo      paz y salvo (formato F)
     * @param conceptoAsesor concepto favorable del asesor; obligatorio en práctica profesional
     */
    public TrabajoGrado solicitarSustentacion(Long trabajoId, Long usuarioId, ArchivoAdjunto documentoFinal,
            ArchivoAdjunto pazYSalvo, ArchivoAdjunto conceptoAsesor);

    /**
     * Programa la sesión de sustentación (la primera o, tras un aplazamiento,
     * la segunda): jurados, fecha, hora y salón.
     *
     * @param usuarioId usuario del Consejo de Facultad que programa
     */
    public TrabajoGrado programarSustentacion(Long trabajoId, Long usuarioId, Long jurado1Id, Long jurado2Id,
            LocalDateTime fechaHora, String salon);

    /**
     * Registra la calificación de un jurado de la sesión actual, con la fecha actual.
     *
     * @param observaciones obligatorias si no es APROBADO
     * @param acta          archivo del acta de sustentación (formato G), opcional
     */
    public TrabajoGrado calificarSustentacion(Long trabajoId, Long usuarioId, Calificacion calificacion,
            String observaciones, ArchivoAdjunto acta);

    /** Registra la inasistencia del estudiante a la sesión actual. Solo un jurado de la sesión. */
    public TrabajoGrado registrarInasistencia(Long trabajoId, Long usuarioId, String observaciones);

    /** Radica los ajustes de una sustentación aprobada con observaciones. Solo el director. */
    public TrabajoGrado radicarAjustesSustentacion(Long trabajoId, Long usuarioId, ArchivoAdjunto documento);

    /** Registra la verificación de los ajustes. Solo un jurado de la sesión. */
    public TrabajoGrado verificarAjustesSustentacion(Long trabajoId, Long usuarioId, boolean cumple,
            String observaciones);

    /** Carga la versión definitiva (formato H) de la sustentación aprobada. Solo un jurado de la sesión. */
    public TrabajoGrado cargarVersionDefinitiva(Long trabajoId, Long usuarioId, ArchivoAdjunto documento);

    /**
     * Solicita la cancelación del trabajo con la fecha actual. Solo el director.
     *
     * @param solicitud archivo del formato de solicitud de cancelación
     */
    public TrabajoGrado solicitarCancelacion(Long trabajoId, Long usuarioId, ArchivoAdjunto solicitud);

    /**
     * Registra la decisión del Consejo de Facultad sobre la cancelación, con la
     * fecha actual.
     *
     * @param usuarioId     usuario del Consejo que registra la decisión
     * @param resultado     APROBADO o NO_APROBADO
     * @param resolucion    archivo de la resolución
     * @param observaciones comentario opcional
     */
    public TrabajoGrado resolverCancelacion(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            ArchivoAdjunto resolucion, String observaciones);

    /**
     * Aplica los plazos a los trabajos en desarrollo, suspendidos o en sustentación: reanuda las
     * suspensiones que llegaron a su fecha máxima y cierra como NO APROBADO por
     * vencimiento los trabajos cuyo plazo terminó. Lo invoca la tarea
     * programada diaria.
     *
     * @return cantidad de trabajos que cambiaron de etapa
     */
    public int verificarVencimientos();
}
