package com.sgtg.backend.application.input.trabajo_grado;

import java.time.LocalDate;
import java.util.List;

import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;

/** Puerto de entrada para la gestión del trabajo de grado (Fase 1: propuesta). */
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
}
