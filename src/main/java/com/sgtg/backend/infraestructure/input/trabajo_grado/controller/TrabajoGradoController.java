package com.sgtg.backend.infraestructure.input.trabajo_grado.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sgtg.backend.application.input.trabajo_grado.ArchivoAdjunto;
import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.ActualizarPropuestaDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.AnulacionDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.DecisionAnteproyectoDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.RevisionPazYSalvoDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.AsignarEvaluadoresDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.CalificacionSustentacionDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.ConceptoEvaluadorDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.CrearPracticaProfesionalDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.CrearTrabajoInvestigacionDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.DecisionComiteDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.DecisionConsejoDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.DecisionNuevaFechaDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.ObservacionesDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.ObservacionesOpcionalesDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.ProgramacionSustentacionDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.SolicitudCambioObjetivosDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.SolicitudProrrogaDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.input.VerificacionAjustesDTORequest;
import com.sgtg.backend.infraestructure.input.trabajo_grado.dto.output.TrabajoGradoDTOResponse;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Trabajos de grado (Fases 1 a 8: creación, propuesta, anteproyecto,
 * evaluación, aprobación del Consejo de Facultad, trabajo en curso, prórroga,
 * suspensión, sustentación y cancelación). Los permisos por rol se declaran aquí; los que dependen del
 * trabajo (ser su director, evaluador o participante, o jefe del departamento
 * del director) los valida el caso de uso.
 * <p>
 * Las operaciones que adjuntan documentos reciben {@code multipart/form-data}:
 * el documento (PDF) en la parte {@code archivo} y, si la operación tiene más
 * datos, un JSON en la parte {@code datos}. Las consultas devuelven solo las
 * rutas de los archivos; cada archivo se obtiene con
 * {@code GET /{trabajoId}/archivos?ruta=...}.
 */
@RestController
@RequestMapping("/api/v1/sgtg/trabajos-grado")
@RequiredArgsConstructor
public class TrabajoGradoController {

    private final ManageTrabajoGradoCUIntPort manageTrabajoGrado;

    /** Crea un trabajo de investigación; el docente autenticado queda como director. */
    @PostMapping("/investigacion")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<TrabajoGradoDTOResponse> crearInvestigacion(@AuthenticationPrincipal UserEntity usuario,
            @Valid @RequestBody CrearTrabajoInvestigacionDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TrabajoGradoDTOResponse
                .desde(manageTrabajoGrado.crear(usuario.getCusuari(), request.toNuevoTrabajo())));
    }

    /** Crea una práctica profesional con la carta de aceptación; el docente autenticado queda como director. */
    @PostMapping(value = "/practica-profesional", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'DIRECTOR')")
    public ResponseEntity<TrabajoGradoDTOResponse> crearPracticaProfesional(
            @AuthenticationPrincipal UserEntity usuario,
            @Valid @RequestPart("datos") CrearPracticaProfesionalDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile cartaAceptacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TrabajoGradoDTOResponse.desde(
                manageTrabajoGrado.crear(usuario.getCusuari(), request.toNuevoTrabajo(adjunto(cartaAceptacion)))));
    }

    /** Trabajos en los que participa el usuario autenticado. */
    @GetMapping("/mis-trabajos")
    public ResponseEntity<List<TrabajoGradoDTOResponse>> listarMisTrabajos(
            @AuthenticationPrincipal UserEntity usuario) {
        return ResponseEntity.ok(manageTrabajoGrado.listarMisTrabajos(usuario.getCusuari()).stream()
                .map(TrabajoGradoDTOResponse::desde).toList());
    }

    /**
     * Trabajos en una etapa (ej. ANTEPROYECTO para asignar evaluadores) o, sin
     * etapa, todos los trabajos. Solo para los roles de consulta general.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMITE', 'JEFE_DEP', 'CONSEJO', 'ADMINISTRADOR', 'DECANATURA')")
    public ResponseEntity<List<TrabajoGradoDTOResponse>> listar(@RequestParam(required = false) Proceso etapa) {
        List<TrabajoGrado> trabajos = etapa == null ? manageTrabajoGrado.listarTodos()
                : manageTrabajoGrado.listarPorEtapa(etapa);
        return ResponseEntity.ok(trabajos.stream().map(TrabajoGradoDTOResponse::desde).toList());
    }

    /**
     * Estudiantes que se pueden elegir para un trabajo nuevo: sin otro trabajo
     * activo y sin un trabajo aprobado.
     */
    @GetMapping("/estudiantes-disponibles")
    @PreAuthorize("hasAnyAuthority('DOCENTE', 'DIRECTOR', 'ADMINISTRADOR')")
    public ResponseEntity<List<Long>> estudiantesDisponibles() {
        return ResponseEntity.ok(manageTrabajoGrado.estudiantesDisponibles());
    }

    @GetMapping("/{trabajoId}")
    public ResponseEntity<TrabajoGradoDTOResponse> consultar(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId) {
        return ResponseEntity
                .ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.consultar(trabajoId, usuario.getCusuari())));
    }

    /**
     * Descarga un archivo del trabajo. Solo los usuarios que pueden consultar
     * el trabajo (sus participantes y los roles de consulta general).
     *
     * @param ruta ruta del archivo tal como aparece en la consulta del trabajo
     */
    @GetMapping("/{trabajoId}/archivos")
    public ResponseEntity<byte[]> descargarArchivo(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @RequestParam String ruta) {
        ArchivoAdjunto archivo = manageTrabajoGrado.obtenerArchivo(trabajoId, usuario.getCusuari(), ruta);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre(), StandardCharsets.UTF_8).build()
                                .toString())
                .body(archivo.contenido());
    }

    /** Radica una versión de la propuesta (formato A). Solo el director del trabajo. */
    @PostMapping(value = "/{trabajoId}/propuesta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> radicarPropuesta(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse
                .desde(manageTrabajoGrado.radicarPropuesta(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /**
     * Corrige título, objetivos, descripción y línea de investigación. Solo el
     * director, antes de la primera radicación o después de un NO APROBADO.
     */
    @PutMapping("/{trabajoId}/propuesta")
    public ResponseEntity<TrabajoGradoDTOResponse> actualizarPropuesta(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestBody ActualizarPropuestaDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.actualizarPropuesta(trabajoId,
                usuario.getCusuari(), request.titulo(), request.objetivos(), request.descripcion(),
                request.lineaInvestigacion())));
    }

    /** Registra el concepto del Comité de Programa sobre la propuesta, con el acta opcional. */
    @PostMapping(value = "/{trabajoId}/propuesta/decisiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('COMITE')")
    public ResponseEntity<TrabajoGradoDTOResponse> registrarDecisionComite(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestPart("datos") DecisionComiteDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.registrarDecisionComite(trabajoId,
                usuario.getCusuari(), request.resultado(), request.acta(), adjunto(archivo),
                request.observaciones(), request.fecha())));
    }

    /** Radica una versión del anteproyecto ante el Departamento. Solo el director del trabajo. */
    @PostMapping(value = "/{trabajoId}/anteproyecto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> radicarAnteproyecto(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse
                .desde(manageTrabajoGrado.radicarAnteproyecto(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /** Asigna los dos evaluadores. Solo el jefe del departamento del director. */
    @PostMapping("/{trabajoId}/anteproyecto/evaluadores")
    @PreAuthorize("hasAuthority('JEFE_DEP')")
    public ResponseEntity<TrabajoGradoDTOResponse> asignarEvaluadores(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestBody AsignarEvaluadoresDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.asignarEvaluadores(trabajoId,
                usuario.getCusuari(), request.evaluador1Id(), request.evaluador2Id())));
    }

    /**
     * Registra el concepto de un evaluador asignado sobre el anteproyecto: el
     * formato B (parte {@code archivo}, obligatorio) y, opcionalmente, un
     * documento con sus anotaciones (parte {@code anotaciones}).
     */
    @PostMapping(value = "/{trabajoId}/anteproyecto/conceptos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> registrarConceptoEvaluador(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestPart("datos") ConceptoEvaluadorDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @RequestPart(value = "anotaciones", required = false) MultipartFile anotaciones) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.registrarConceptoEvaluador(
                trabajoId, usuario.getCusuari(), request.resultado(), request.observaciones(), adjunto(archivo),
                adjunto(anotaciones))));
    }

    /**
     * El Jefe del departamento del director remite al Consejo de Facultad el
     * anteproyecto aprobado por ambos evaluadores, con el formato C (parte
     * {@code archivo}).
     */
    @PostMapping(value = "/{trabajoId}/anteproyecto/remision", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('JEFE_DEP')")
    public ResponseEntity<TrabajoGradoDTOResponse> remitirAnteproyectoAlConsejo(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(
                manageTrabajoGrado.remitirAnteproyectoAlConsejo(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /** Registra el concepto del Consejo de Facultad sobre el anteproyecto, con la resolución adjunta. */
    @PostMapping(value = "/{trabajoId}/anteproyecto/decisiones-consejo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSEJO')")
    public ResponseEntity<TrabajoGradoDTOResponse> registrarDecisionConsejo(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestPart("datos") DecisionAnteproyectoDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.registrarDecisionConsejo(trabajoId,
                usuario.getCusuari(), request.resultado(), adjunto(archivo), request.observaciones(),
                request.mesesEjecucion())));
    }

    /** Solicita el cambio de título u objetivos del trabajo en curso. Solo el director. */
    @PostMapping("/{trabajoId}/cambio-objetivos")
    public ResponseEntity<TrabajoGradoDTOResponse> solicitarCambioObjetivos(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestBody SolicitudCambioObjetivosDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.solicitarCambioObjetivos(trabajoId,
                usuario.getCusuari(), request.titulo(), request.objetivos(), request.justificacion())));
    }

    /** Registra la decisión del Consejo de Facultad sobre el cambio de título u objetivos. */
    @PostMapping(value = "/{trabajoId}/cambio-objetivos/decisiones-consejo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSEJO')")
    public ResponseEntity<TrabajoGradoDTOResponse> resolverCambioObjetivos(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestPart("datos") DecisionConsejoDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.resolverCambioObjetivos(trabajoId,
                usuario.getCusuari(), request.resultado(), adjunto(archivo), request.observaciones())));
    }

    /**
     * Radica un informe mensual (formato D, parte {@code archivo}) con
     * observaciones opcionales (parte {@code datos}). Lo radica un estudiante del
     * trabajo; en práctica profesional también el director.
     */
    @PostMapping(value = "/{trabajoId}/informes-mensuales", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> registrarInformeMensual(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @RequestPart(value = "datos", required = false) ObservacionesOpcionalesDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.registrarInformeMensual(trabajoId,
                usuario.getCusuari(), request == null ? null : request.observaciones(), adjunto(archivo))));
    }

    /**
     * Revisión de un informe mensual por el director, el codirector o el asesor:
     * observaciones (parte {@code datos}) y, opcionalmente, el PDF con sus notas
     * (parte {@code archivo}). No cambia el estado del trabajo.
     */
    @PostMapping(value = "/{trabajoId}/informes-mensuales/{numero}/revisiones",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> revisarInformeMensual(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId, @PathVariable int numero,
            @RequestPart(value = "datos", required = false) ObservacionesOpcionalesDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.revisarInformeMensual(trabajoId,
                numero, usuario.getCusuari(), request == null ? null : request.observaciones(), adjunto(archivo))));
    }

    /** Solicita la prórroga del plazo (1 a 3 meses). Solo el director. */
    @PostMapping(value = "/{trabajoId}/prorroga", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> solicitarProrroga(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestPart("datos") SolicitudProrrogaDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.solicitarProrroga(trabajoId,
                usuario.getCusuari(), request.meses(), adjunto(archivo))));
    }

    /** Registra la decisión del Consejo de Facultad sobre la prórroga. */
    @PostMapping(value = "/{trabajoId}/prorroga/decisiones-consejo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSEJO')")
    public ResponseEntity<TrabajoGradoDTOResponse> resolverProrroga(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestPart("datos") DecisionConsejoDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.resolverProrroga(trabajoId,
                usuario.getCusuari(), request.resultado(), adjunto(archivo), request.observaciones())));
    }

    /** Solicita la suspensión del trabajo. Solo el director. */
    @PostMapping(value = "/{trabajoId}/suspension", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> solicitarSuspension(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(
                manageTrabajoGrado.solicitarSuspension(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /** Registra la decisión del Consejo de Facultad sobre la suspensión. */
    @PostMapping(value = "/{trabajoId}/suspension/decisiones-consejo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSEJO')")
    public ResponseEntity<TrabajoGradoDTOResponse> resolverSuspension(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestPart("datos") DecisionConsejoDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.resolverSuspension(trabajoId,
                usuario.getCusuari(), request.resultado(), adjunto(archivo), request.observaciones())));
    }

    /** Reanuda el trabajo suspendido. Solo el director. */
    @PostMapping(value = "/{trabajoId}/suspension/reanudacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> reanudar(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse
                .desde(manageTrabajoGrado.reanudar(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /**
     * Solicita la sustentación con la monografía, el documento final (formato
     * E), en práctica profesional el concepto del asesor y, opcionalmente, los
     * anexos (PDF). Solo el director. El paz y salvo lo revisa Decanatura después.
     */
    @PostMapping(value = "/{trabajoId}/sustentacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> solicitarSustentacion(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId,
            @RequestPart(value = "monografia", required = false) MultipartFile monografia,
            @RequestPart(value = "documentoFinal", required = false) MultipartFile documentoFinal,
            @RequestPart(value = "conceptoAsesor", required = false) MultipartFile conceptoAsesor,
            @RequestPart(value = "anexos", required = false) MultipartFile anexos) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.solicitarSustentacion(trabajoId,
                usuario.getCusuari(), adjunto(monografia), adjunto(documentoFinal), adjunto(conceptoAsesor),
                adjunto(anexos))));
    }

    /**
     * Revisión del paz y salvo de Decanatura: {@code datos} (aprobado y
     * observaciones) y el formato F en la parte {@code archivo}, obligatorio
     * tanto si lo aprueba como si no.
     */
    @PostMapping(value = "/{trabajoId}/sustentacion/paz-y-salvo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DECANATURA')")
    public ResponseEntity<TrabajoGradoDTOResponse> revisarPazYSalvo(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestPart("datos") RevisionPazYSalvoDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.revisarPazYSalvo(trabajoId,
                usuario.getCusuari(), request.aprobado(), request.observaciones(), adjunto(archivo))));
    }

    /**
     * Programa la sesión de sustentación: jurado coordinador, otro jurado,
     * fecha, hora y salón (parte {@code datos}) y el formato E (parte
     * {@code archivo}, obligatorio).
     */
    @PostMapping(value = "/{trabajoId}/sustentacion/programacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSEJO')")
    public ResponseEntity<TrabajoGradoDTOResponse> programarSustentacion(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestPart("datos") ProgramacionSustentacionDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.programarSustentacion(trabajoId,
                usuario.getCusuari(), request.juradoCoordinadorId(), request.juradoId(), request.fechaHora(),
                request.salon(), adjunto(archivo))));
    }

    /**
     * Solicita una nueva fecha para la sesión programada, antes de la fecha
     * designada, con el documento de la solicitud. Solo el director.
     */
    @PostMapping(value = "/{trabajoId}/sustentacion/nueva-fecha", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> solicitarNuevaFechaSustentacion(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado
                .solicitarNuevaFechaSustentacion(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /**
     * Registra la decisión del Consejo sobre la nueva fecha (parte {@code datos})
     * con la resolución (parte {@code archivo}). Si la aprueba, reprograma la sesión.
     */
    @PostMapping(value = "/{trabajoId}/sustentacion/nueva-fecha/decisiones-consejo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSEJO')")
    public ResponseEntity<TrabajoGradoDTOResponse> resolverNuevaFechaSustentacion(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestPart("datos") DecisionNuevaFechaDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.resolverNuevaFechaSustentacion(
                trabajoId, usuario.getCusuari(), request.resultado(), request.fechaHora(), request.salon(),
                adjunto(archivo), request.observaciones())));
    }

    /** Registra la calificación de la sesión actual con el acta (formato G). Solo el jurado coordinador. */
    @PostMapping(value = "/{trabajoId}/sustentacion/calificaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> calificarSustentacion(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestPart("datos") CalificacionSustentacionDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.calificarSustentacion(trabajoId,
                usuario.getCusuari(), request.calificacion(), request.observaciones(), adjunto(archivo))));
    }

    /** Registra la inasistencia del estudiante. Solo el jurado coordinador de la sesión actual. */
    @PostMapping("/{trabajoId}/sustentacion/inasistencia")
    public ResponseEntity<TrabajoGradoDTOResponse> registrarInasistencia(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestBody ObservacionesDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(
                manageTrabajoGrado.registrarInasistencia(trabajoId, usuario.getCusuari(), request.observaciones())));
    }

    /**
     * Radica los ajustes de una sustentación aprobada con observaciones (parte
     * {@code archivo}) y, opcionalmente, sus anexos (parte {@code anexos}). Solo el director.
     */
    @PostMapping(value = "/{trabajoId}/sustentacion/ajustes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> radicarAjustesSustentacion(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @RequestPart(value = "anexos", required = false) MultipartFile anexos) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado
                .radicarAjustesSustentacion(trabajoId, usuario.getCusuari(), adjunto(archivo), adjunto(anexos))));
    }

    /** Registra la verificación de los ajustes. Solo el jurado coordinador de la sesión actual. */
    @PostMapping("/{trabajoId}/sustentacion/ajustes/verificaciones")
    public ResponseEntity<TrabajoGradoDTOResponse> verificarAjustesSustentacion(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @Valid @RequestBody VerificacionAjustesDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.verificarAjustesSustentacion(
                trabajoId, usuario.getCusuari(), request.cumple(), request.observaciones())));
    }

    /** Carga la versión definitiva (formato H) de la sustentación aprobada. Solo el jurado coordinador. */
    @PostMapping(value = "/{trabajoId}/sustentacion/version-definitiva",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> cargarVersionDefinitiva(
            @AuthenticationPrincipal UserEntity usuario, @PathVariable Long trabajoId,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado
                .cargarVersionDefinitiva(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /**
     * Anula el trabajo (borrado lógico): queda cerrado y archivado en el
     * historial. Solo el director, en propuesta o anteproyecto.
     */
    @PostMapping("/{trabajoId}/anulacion")
    public ResponseEntity<TrabajoGradoDTOResponse> anular(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestBody AnulacionDTORequest request) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(
                manageTrabajoGrado.anular(trabajoId, usuario.getCusuari(), request.justificacion())));
    }

    /** Solicita la cancelación del trabajo con el formato adjunto. Solo el director. */
    @PostMapping(value = "/{trabajoId}/cancelacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrabajoGradoDTOResponse> solicitarCancelacion(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(
                manageTrabajoGrado.solicitarCancelacion(trabajoId, usuario.getCusuari(), adjunto(archivo))));
    }

    /** Registra la decisión del Consejo de Facultad sobre la cancelación. */
    @PostMapping(value = "/{trabajoId}/cancelacion/decisiones-consejo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONSEJO')")
    public ResponseEntity<TrabajoGradoDTOResponse> resolverCancelacion(@AuthenticationPrincipal UserEntity usuario,
            @PathVariable Long trabajoId, @Valid @RequestPart("datos") DecisionConsejoDTORequest request,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(TrabajoGradoDTOResponse.desde(manageTrabajoGrado.resolverCancelacion(trabajoId,
                usuario.getCusuari(), request.resultado(), adjunto(archivo), request.observaciones())));
    }

    /** Convierte el archivo recibido al modelo de la aplicación; null si no se envió. */
    private static ArchivoAdjunto adjunto(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty())
            return null;
        try {
            return new ArchivoAdjunto(archivo.getOriginalFilename(), archivo.getContentType(), archivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo recibido", e);
        }
    }
}
