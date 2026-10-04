package com.sgtg.backend.application.input.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;

/** Puerto de entrada para la gestión del trabajo de grado (Fases 1 y 2: propuesta, anteproyecto y evaluación). */
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
     * Radica una versión de la propuesta. Solo el director del trabajo.
     *
     * @param url ubicación del documento ya cargado al servidor de archivos
     */
    public TrabajoGrado radicarPropuesta(Long trabajoId, Long usuarioId, String url);

    /**
     * Registra el concepto del Comité de Programa sobre la propuesta.
     *
     * @param usuarioId   usuario del comité que registra la decisión
     * @param resultado   APROBADO o NO_APROBADO
     * @param rutaArchivo ruta del archivo de soporte en el servidor de archivos, opcional
     * @param fecha       fecha de la decisión, null para usar la fecha actual
     */
    public TrabajoGrado registrarDecisionComite(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String acta, String rutaArchivo, String observaciones, LocalDate fecha);

    /**
     * Radica una versión del anteproyecto ante el Departamento. Solo el director
     * del trabajo.
     *
     * @param url ubicación del documento ya cargado al servidor de archivos
     */
    public TrabajoGrado radicarAnteproyecto(Long trabajoId, Long usuarioId, String url);

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
     * @param urlFormato    ubicación del formato B en el servidor de archivos, opcional
     */
    public TrabajoGrado registrarConceptoEvaluador(Long trabajoId, Long usuarioId, ResultadoConcepto resultado,
            String observaciones, String urlFormato);
}
