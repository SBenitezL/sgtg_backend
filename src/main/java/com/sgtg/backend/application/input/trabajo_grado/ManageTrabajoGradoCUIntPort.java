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
     * Consulta un trabajo. Pueden hacerlo sus participantes; los usuarios con rol
     * CONSEJO, DECANATURA o ADMINISTRADOR; el Comité del programa de sus
     * estudiantes (docentes con rol COMITE del departamento al que está adscrito
     * el programa) y el jefe del departamento de su director (JEFE_DEP).
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
     * pendientes de evaluadores) y que el usuario puede consultar (ver
     * {@link #consultar}): el Comité solo ve los de su programa y el jefe los de
     * su departamento. El controlador restringe el acceso por rol.
     *
     * @param etapa     etapa del proceso
     * @param usuarioId usuario que consulta
     * @return los trabajos en esa etapa, del más reciente al más antiguo
     */
    public List<TrabajoGrado> listarPorEtapa(Proceso etapa, Long usuarioId);

    /**
     * Lista los trabajos de grado, en cualquier etapa, que el usuario puede
     * consultar (ver {@link #consultar}): pantallas de seguimiento, historial y
     * estadísticas. El controlador restringe el acceso por rol.
     *
     * @param usuarioId usuario que consulta
     * @return los trabajos, del más reciente al más antiguo
     */
    public List<TrabajoGrado> listarTodos(Long usuarioId);

    /**
     * @return los usuarios estudiantes (activos) que pueden participar en un trabajo
     *         nuevo: sin un trabajo activo y sin un trabajo aprobado (FINALIZADO)
     */
    public List<Long> estudiantesDisponibles();

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
     * Registra el concepto del Comité de Programa sobre la propuesta. Solo un
     * miembro del Comité del programa de los estudiantes del trabajo.
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
     * @param formato       archivo del formato B, obligatorio
     * @param anotaciones   documento con las anotaciones del evaluador, opcional
     */
    public TrabajoGrado registrarConceptoEvaluador(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String observaciones, ArchivoAdjunto formato, ArchivoAdjunto anotaciones);

    /**
     * El Jefe del departamento del director remite al Consejo de Facultad, con
     * el formato C, el anteproyecto que ambos evaluadores aprobaron. Paso
     * obligatorio antes de la decisión del Consejo.
     *
     * @param usuarioId jefe de departamento que remite
     * @param formatoC  archivo del formato C
     */
    public TrabajoGrado remitirAnteproyectoAlConsejo(Long trabajoId, Long usuarioId, ArchivoAdjunto formatoC);

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
            ArchivoAdjunto resolucion, String observaciones, Integer mesesEjecucion);

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
     * Radica un informe mensual con la fecha actual, en ambas modalidades. Lo
     * radica un estudiante del trabajo; en práctica profesional también el director.
     *
     * @param observaciones observaciones opcionales de quien radica
     * @param documento     archivo del informe (formato D)
     */
    public TrabajoGrado registrarInformeMensual(Long trabajoId, Long usuarioId, String observaciones,
            ArchivoAdjunto documento);

    /**
     * Registra la revisión de un informe mensual por el director, el codirector
     * o el asesor del trabajo. Es una formalidad: no cambia el estado del trabajo.
     *
     * @param numero        consecutivo del informe
     * @param observaciones observaciones del revisor
     * @param notas         PDF opcional con las notas del revisor
     */
    public TrabajoGrado revisarInformeMensual(Long trabajoId, int numero, Long usuarioId, String observaciones,
            ArchivoAdjunto notas);

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
     * @param monografia     monografía del trabajo
     * @param documentoFinal documento final (formato E)
     * @param conceptoAsesor concepto favorable del asesor; obligatorio en práctica profesional
     * @param anexos         anexos (PDF), opcional
     */
    public TrabajoGrado solicitarSustentacion(Long trabajoId, Long usuarioId, ArchivoAdjunto monografia,
            ArchivoAdjunto documentoFinal, ArchivoAdjunto conceptoAsesor,
            ArchivoAdjunto anexos);

    /**
     * Revisión del paz y salvo (formato F) de Decanatura: lo aprueba o no, siempre
     * con el documento; si no lo aprueba, con observaciones.
     */
    public TrabajoGrado revisarPazYSalvo(Long trabajoId, Long usuarioId, boolean aprobado, String observaciones,
            ArchivoAdjunto formatoF);

    /**
     * Programa la sesión de sustentación (la primera o, tras un aplazamiento,
     * la segunda): jurado coordinador, otro jurado, fecha, hora, salón y el
     * formato E. Asigna el rol JURADO a ambos y JURADO_COORDINADOR al coordinador.
     *
     * @param usuarioId     usuario del Consejo de Facultad que programa
     * @param coordinadorId usuario del jurado coordinador
     * @param juradoId      usuario del otro jurado
     * @param formatoE      formato E (obligatorio)
     */
    public TrabajoGrado programarSustentacion(Long trabajoId, Long usuarioId, Long coordinadorId, Long juradoId,
            LocalDateTime fechaHora, String salon, ArchivoAdjunto formatoE);

    /**
     * Solicita una nueva fecha para la sesión programada, antes de la fecha
     * designada. Solo el director.
     *
     * @param solicitud documento de la solicitud
     */
    public TrabajoGrado solicitarNuevaFechaSustentacion(Long trabajoId, Long usuarioId, ArchivoAdjunto solicitud);

    /**
     * Registra la decisión del Consejo sobre la nueva fecha. Si la aprueba,
     * reprograma la sesión.
     *
     * @param nuevaFechaHora nueva fecha y hora; obligatoria si se aprueba
     * @param salon          nuevo salón; si es vacío se conserva
     * @param resolucion     resolución del Consejo (obligatoria)
     */
    public TrabajoGrado resolverNuevaFechaSustentacion(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            LocalDateTime nuevaFechaHora, String salon, ArchivoAdjunto resolucion, String observaciones);

    /**
     * Registra la calificación de la sesión actual, con la fecha actual. Solo el
     * jurado coordinador.
     *
     * @param observaciones obligatorias si no es APROBADO
     * @param acta          archivo del acta de sustentación (formato G), obligatorio
     */
    public TrabajoGrado calificarSustentacion(Long trabajoId, Long usuarioId, Calificacion calificacion,
            String observaciones, ArchivoAdjunto acta);

    /** Registra la inasistencia del estudiante a la sesión actual. Solo el jurado coordinador. */
    public TrabajoGrado registrarInasistencia(Long trabajoId, Long usuarioId, String observaciones);

    /**
     * Radica los ajustes de una sustentación aprobada con observaciones. Solo el director.
     *
     * @param anexos anexos de los ajustes (PDF), opcional
     */
    public TrabajoGrado radicarAjustesSustentacion(Long trabajoId, Long usuarioId, ArchivoAdjunto documento,
            ArchivoAdjunto anexos);

    /** Registra la verificación de los ajustes. Solo el jurado coordinador. */
    public TrabajoGrado verificarAjustesSustentacion(Long trabajoId, Long usuarioId, boolean cumple,
            String observaciones);

    /** Carga la versión definitiva (formato H) de la sustentación aprobada. Solo el jurado coordinador. */
    public TrabajoGrado cargarVersionDefinitiva(Long trabajoId, Long usuarioId, ArchivoAdjunto documento);

    /**
     * Anulación del director (borrado lógico): el trabajo se cierra y queda en
     * el historial. Solo en propuesta o anteproyecto.
     *
     * @param justificacion motivo de la anulación (obligatorio)
     */
    public TrabajoGrado anular(Long trabajoId, Long usuarioId, String justificacion);

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
