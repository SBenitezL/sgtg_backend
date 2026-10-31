package com.sgtg.backend.infraestructure.output.percistence.gateway;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageInstantaneaGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.Instantanea;
import com.sgtg.backend.infraestructure.output.percistence.entities.CredencialEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.PerfilDocenteEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.PerfilEstudianteEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.DecisionEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.EvaluadorTrabajoEmbeddable;
import com.sgtg.backend.infraestructure.output.percistence.entities.trabajo_grado.TrabajoGradoEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.CredencialRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.PerfilDocenteRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.PerfilEstudianteRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.TrabajoGradoRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Datos de usuarios y trabajos de grado que guarda la auditoría antes y después
 * de cada operación sobre ellos. Se leen en su propia transacción, fuera de la
 * de la operación. Los textos largos (observaciones, justificaciones) no se
 * incluyen: solo lo que decide el trámite.
 */
@Service
@RequiredArgsConstructor
public class ManageInstantaneaGatewayImplAdapter implements ManageInstantaneaGatewayIntPort {

    private final UserRepository userRepository;
    private final CredencialRepository credencialRepository;
    private final PerfilDocenteRepository perfilDocenteRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final TrabajoGradoRepository trabajoRepository;

    @Override
    @Transactional(readOnly = true)
    public Instantanea usuario(Long usuarioId) {
        UserEntity usuario = usuarioId == null ? null : userRepository.findById(usuarioId).orElse(null);
        if (usuario == null)
            return null;
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("código", usuario.getCodigo());
        datos.put("documento", usuario.getTipoDocumento() + " " + usuario.getNumeroDocumento());
        datos.put("correo", usuario.getEmail());
        datos.put("nombre", String.join(" ", Objects.toString(usuario.getPrimerNombre(), ""),
                Objects.toString(usuario.getSegundoNombre(), ""), Objects.toString(usuario.getPrimerApellido(), ""),
                Objects.toString(usuario.getSegundoApellido(), "")).replaceAll("\\s+", " ").trim());
        datos.put("roles", usuario.getRole().stream().map(rol -> rol.getTrole().name()).sorted()
                .collect(Collectors.joining(", ")));
        datos.put("activo", usuario.isActivo() ? "sí" : "no");
        PerfilDocenteEntity docente = perfilDocenteRepository.findById(usuarioId).orElse(null);
        datos.put("vinculación", docente == null ? null : String.valueOf(docente.getTipoVinculacion()));
        datos.put("departamento", docente == null || docente.getDepartamento() == null ? null
                : docente.getDepartamento().getNombre());
        PerfilEstudianteEntity estudiante = perfilEstudianteRepository.findById(usuarioId).orElse(null);
        datos.put("programa", estudiante == null || estudiante.getPrograma() == null ? null
                : estudiante.getPrograma().getNombre());
        // Nunca el hash: solo cuántas veces cambió y si debe cambiarla
        CredencialEntity credencial = credencialRepository.findById(usuarioId).orElse(null);
        datos.put("contraseña", credencial == null ? null : "versión " + credencial.getVersion());
        datos.put("cambio de contraseña pendiente", credencial == null ? null : credencial.isDebeCambiar() ? "sí" : "no");
        return new Instantanea(datos);
    }

    @Override
    @Transactional(readOnly = true)
    public Instantanea trabajo(Long trabajoId) {
        TrabajoGradoEntity t = trabajoId == null ? null : trabajoRepository.findById(trabajoId).orElse(null);
        if (t == null)
            return null;
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("etapa", texto(t.getEtapa()));
        datos.put("motivo de cierre", texto(t.getMotivoCierre()));
        datos.put("título", t.getTitulo());
        datos.put("modalidad", texto(t.getModalidad()));
        datos.put("línea de investigación", t.getLineaInvestigacion());
        datos.put("estudiantes", lista(t.getEstudiantes(), e -> "usuario " + e.getUsuarioId()));
        datos.put("dirección", lista(t.getDireccion(), d -> d.getRol() + " usuario " + d.getUsuarioId()));
        datos.put("asesor", t.getAsesor() == null ? null
                : t.getAsesor().getUsuarioId() != null ? "usuario " + t.getAsesor().getUsuarioId()
                        : t.getAsesor().getEmail());
        datos.put("propuesta: fecha límite del concepto", texto(t.getPropuestaFechaLimiteConcepto()));
        datos.put("propuesta: versiones", String.valueOf(t.getPropuestaVersiones().size()));
        datos.put("propuesta: decisiones", lista(t.getPropuestaDecisiones(), ManageInstantaneaGatewayImplAdapter::decision));
        datos.put("anteproyecto: versiones", String.valueOf(t.getAnteproyectoVersiones().size()));
        datos.put("anteproyecto: evaluadores", lista(t.getAnteproyectoEvaluadores(), e -> "usuario " + e.getUsuarioId()));
        datos.put("anteproyecto: conceptos", lista(t.getAnteproyectoConceptos(), c -> "v" + c.getVersion()
                + " evaluador " + c.getEvaluadorUsuarioId() + ": " + c.getResultado()));
        datos.put("anteproyecto: decisiones del Consejo",
                lista(t.getAnteproyectoDecisionesConsejo(), ManageInstantaneaGatewayImplAdapter::decision));
        datos.put("anteproyecto: fecha límite de evaluación", texto(t.getAnteproyectoFechaLimiteEvaluacion()));
        datos.put("anteproyecto: fecha límite de correcciones", texto(t.getAnteproyectoFechaLimiteCorrecciones()));
        datos.put("ejecución: fecha de fin", texto(t.getEjecucionFechaFin()));
        datos.put("ejecución: fecha mínima de sustentación", texto(t.getEjecucionFechaMinimaSustentacion()));
        datos.put("ejecución: cambios de objetivos", lista(t.getEjecucionCambiosObjetivos(),
                c -> "solicitado " + c.getFechaSolicitud() + ": " + decision(c.getDecision())));
        datos.put("ejecución: informes mensuales", String.valueOf(t.getEjecucionInformesMensuales().size()));
        datos.put("ejecución: reuniones", String.valueOf(t.getEjecucionReuniones().size()));
        datos.put("prórroga", t.getProrroga() == null || t.getProrroga().getFechaSolicitud() == null ? null
                : t.getProrroga().getMeses() + " meses, " + decision(t.getProrroga().getDecision()));
        datos.put("suspensiones", lista(t.getEjecucionSuspensiones(), s -> "solicitada " + s.getFechaSolicitud()
                + ": " + decision(s.getDecision()) + (s.getFechaReanudacion() == null ? ""
                        : ", reanudada " + s.getFechaReanudacion())));
        datos.put("cancelaciones", lista(t.getCancelaciones(),
                c -> "solicitada " + c.getFechaSolicitud() + ": " + decision(c.getDecision())));
        datos.put("sustentación: estado", texto(t.getSustentacionEstado()));
        datos.put("sustentación: sesiones", lista(t.getSustentacionSesiones(), s -> "sesión " + s.getNumero() + " "
                + s.getFechaHora() + " en " + s.getSalon() + ", jurados " + jurado(s.getJurado1()) + " y "
                + jurado(s.getJurado2())));
        datos.put("sustentación: calificaciones", lista(t.getSustentacionCalificaciones(), c -> "sesión "
                + c.getSesion() + " jurado " + c.getJuradoUsuarioId() + ": " + c.getCalificacion()));
        datos.put("sustentación: inasistencias", lista(t.getSustentacionInasistencias(),
                r -> "sesión " + r.getSesion() + " jurado " + r.getJuradoUsuarioId()));
        datos.put("sustentación: verificaciones de ajustes", lista(t.getSustentacionVerificaciones(), r -> "sesión "
                + r.getSesion() + " jurado " + r.getJuradoUsuarioId() + ": "
                + (Boolean.TRUE.equals(r.getFavorable()) ? "favorable" : "no favorable")));
        datos.put("sustentación: fecha límite de ajustes", texto(t.getSustentacionFechaLimiteAjustes()));
        datos.put("sustentación: versión definitiva", texto(t.getSustentacionFechaVersionDefinitiva()));
        datos.put("sustentación: motivo de no aprobación", texto(t.getSustentacionMotivoNoAprobacion()));
        return new Instantanea(datos);
    }

    private static String decision(DecisionEmbeddable decision) {
        return decision == null || decision.getResultado() == null ? "sin decisión"
                : decision.getOrgano() + " " + decision.getResultado() + " (" + decision.getFecha() + ")";
    }

    private static String jurado(EvaluadorTrabajoEmbeddable jurado) {
        return jurado == null || jurado.getUsuarioId() == null ? "sin asignar" : "usuario " + jurado.getUsuarioId();
    }

    private static <T> String lista(Collection<T> elementos, Function<T, String> texto) {
        return elementos == null || elementos.isEmpty() ? null
                : elementos.stream().map(texto).collect(Collectors.joining("; "));
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
