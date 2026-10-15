package com.sgtg.backend.domain.models.notificacion;

import static com.sgtg.backend.domain.models.notificacion.Destinatario.COMITE;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.CONSEJO;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.DECANATURA;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.DIRECCION;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.ESTUDIANTES;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.EVALUADORES;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.EVALUADORES_PENDIENTES;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.JEFE_DEP;
import static com.sgtg.backend.domain.models.notificacion.Destinatario.JURADOS;
import static com.sgtg.backend.domain.models.notificacion.TipoNotificacion.ALERTA;
import static com.sgtg.backend.domain.models.notificacion.TipoNotificacion.CONFIRMACION;
import static com.sgtg.backend.domain.models.notificacion.TipoNotificacion.ERROR;
import static com.sgtg.backend.domain.models.notificacion.TipoNotificacion.INFO;

import java.util.EnumSet;
import java.util.Set;

/**
 * Hechos del trabajo de grado que generan notificaciones: el título, el texto
 * y a quiénes se envían. El texto recibe el título del trabajo ({@code %1$s})
 * y un detalle ({@code %2$s}: un resultado, una fecha, una justificación...).
 * Quien realiza la acción no recibe la notificación.
 */
public enum EventoTrabajoGrado {
    // ----- Propuesta -----
    TRABAJO_REGISTRADO(INFO, "Trabajo de grado registrado",
            "El director registró el trabajo de grado \"%1$s\".", ESTUDIANTES, DIRECCION),
    PROPUESTA_RADICADA(INFO, "Propuesta radicada",
            "Se radicó la propuesta \"%1$s\" para el concepto del Comité de Programa.", COMITE, ESTUDIANTES,
            DIRECCION),
    DECISION_COMITE(CONFIRMACION, "Concepto del Comité de Programa",
            "El Comité de Programa emitió su concepto sobre la propuesta \"%1$s\": %2$s.", ESTUDIANTES, DIRECCION),

    // ----- Anteproyecto -----
    ANTEPROYECTO_RADICADO(INFO, "Anteproyecto radicado",
            "Se radicó el anteproyecto \"%1$s\" para la asignación de evaluadores.", JEFE_DEP, ESTUDIANTES,
            DIRECCION),
    ANTEPROYECTO_CORREGIDO(INFO, "Nueva versión del anteproyecto",
            "Se radicó una nueva versión del anteproyecto \"%1$s\" para su evaluación.", EVALUADORES, ESTUDIANTES,
            DIRECCION),
    EVALUADORES_ASIGNADOS(INFO, "Evaluadores asignados",
            "Se asignaron los evaluadores del anteproyecto \"%1$s\".", EVALUADORES, ESTUDIANTES, DIRECCION),
    CONCEPTO_EVALUADOR(CONFIRMACION, "Concepto de evaluación registrado",
            "Un evaluador emitió su concepto sobre el anteproyecto \"%1$s\": %2$s.", ESTUDIANTES, DIRECCION,
            JEFE_DEP),
    ANTEPROYECTO_REMITIDO(INFO, "Anteproyecto remitido al Consejo",
            "El Departamento remitió el anteproyecto \"%1$s\" al Consejo de Facultad.", CONSEJO, ESTUDIANTES,
            DIRECCION),
    DECISION_CONSEJO_ANTEPROYECTO(CONFIRMACION, "Decisión del Consejo sobre el anteproyecto",
            "El Consejo de Facultad decidió sobre el anteproyecto \"%1$s\": %2$s.", ESTUDIANTES, DIRECCION,
            JEFE_DEP),

    // ----- En curso -----
    CAMBIO_OBJETIVOS_SOLICITADO(INFO, "Solicitud de cambio de título u objetivos",
            "El director solicitó cambiar el título u objetivos de \"%1$s\".", CONSEJO, ESTUDIANTES),
    CAMBIO_OBJETIVOS_RESUELTO(CONFIRMACION, "Cambio de título u objetivos resuelto",
            "El Consejo de Facultad decidió sobre el cambio de título u objetivos de \"%1$s\": %2$s.", ESTUDIANTES,
            DIRECCION),
    INFORME_RADICADO(INFO, "Informe mensual radicado",
            "Se radicó el informe mensual %2$s del trabajo \"%1$s\".", DIRECCION, ESTUDIANTES),
    INFORME_REVISADO(CONFIRMACION, "Informe mensual revisado",
            "Se registró la revisión del informe mensual %2$s del trabajo \"%1$s\".", ESTUDIANTES, DIRECCION),
    PRORROGA_SOLICITADA(INFO, "Solicitud de prórroga",
            "El director solicitó una prórroga para el trabajo \"%1$s\".", CONSEJO, ESTUDIANTES),
    PRORROGA_RESUELTA(CONFIRMACION, "Prórroga resuelta",
            "El Consejo de Facultad decidió sobre la prórroga del trabajo \"%1$s\": %2$s.", ESTUDIANTES, DIRECCION),
    SUSPENSION_SOLICITADA(INFO, "Solicitud de suspensión",
            "El director solicitó la suspensión del trabajo \"%1$s\".", CONSEJO, ESTUDIANTES),
    SUSPENSION_RESUELTA(CONFIRMACION, "Suspensión resuelta",
            "El Consejo de Facultad decidió sobre la suspensión del trabajo \"%1$s\": %2$s.", ESTUDIANTES,
            DIRECCION),
    TRABAJO_REANUDADO(INFO, "Trabajo reanudado",
            "El trabajo \"%1$s\" se reanudó.", ESTUDIANTES, DIRECCION, CONSEJO),

    // ----- Sustentación -----
    SUSTENTACION_SOLICITADA(INFO, "Solicitud de sustentación",
            "El director solicitó la sustentación del trabajo \"%1$s\"; Decanatura debe revisar el paz y salvo.",
            DECANATURA, CONSEJO, ESTUDIANTES, DIRECCION),
    PAZ_Y_SALVO_APROBADO(CONFIRMACION, "Paz y salvo aprobado",
            "Decanatura aprobó el paz y salvo del trabajo \"%1$s\"; la sustentación se puede programar.", CONSEJO,
            ESTUDIANTES, DIRECCION),
    PAZ_Y_SALVO_NO_APROBADO(ALERTA, "Paz y salvo no aprobado",
            "Decanatura no aprobó el paz y salvo del trabajo \"%1$s\": %2$s", ESTUDIANTES, DIRECCION),
    SUSTENTACION_PROGRAMADA(INFO, "Sustentación programada",
            "Se programó la sustentación del trabajo \"%1$s\" para el %2$s.", JURADOS, ESTUDIANTES, DIRECCION),
    NUEVA_FECHA_SOLICITADA(INFO, "Solicitud de nueva fecha de sustentación",
            "El director solicitó una nueva fecha para la sustentación del trabajo \"%1$s\".", CONSEJO, JURADOS,
            ESTUDIANTES),
    NUEVA_FECHA_RESUELTA(CONFIRMACION, "Nueva fecha de sustentación resuelta",
            "El Consejo de Facultad decidió sobre la nueva fecha de sustentación del trabajo \"%1$s\": %2$s.",
            ESTUDIANTES, DIRECCION, JURADOS),
    SUSTENTACION_CALIFICADA(CONFIRMACION, "Sustentación calificada",
            "El jurado coordinador calificó la sustentación del trabajo \"%1$s\": %2$s.", ESTUDIANTES, DIRECCION,
            JURADOS, CONSEJO),
    INASISTENCIA_REGISTRADA(ALERTA, "Inasistencia a la sustentación",
            "El jurado coordinador registró la inasistencia a la sustentación del trabajo \"%1$s\".", ESTUDIANTES,
            DIRECCION, JURADOS, CONSEJO),
    AJUSTES_RADICADOS(INFO, "Ajustes de la sustentación radicados",
            "El director radicó los ajustes de la sustentación del trabajo \"%1$s\".", JURADOS, ESTUDIANTES),
    AJUSTES_VERIFICADOS(CONFIRMACION, "Ajustes verificados",
            "El jurado coordinador verificó los ajustes del trabajo \"%1$s\": %2$s.", ESTUDIANTES, DIRECCION,
            JURADOS),
    VERSION_DEFINITIVA_CARGADA(CONFIRMACION, "Trabajo de grado finalizado",
            "Se cargó la versión definitiva del trabajo \"%1$s\"; el trabajo quedó finalizado.", ESTUDIANTES,
            DIRECCION, JURADOS, CONSEJO),

    // ----- Cancelación y anulación -----
    CANCELACION_SOLICITADA(INFO, "Solicitud de cancelación",
            "El director solicitó la cancelación del trabajo \"%1$s\".", CONSEJO, ESTUDIANTES),
    CANCELACION_RESUELTA(CONFIRMACION, "Cancelación resuelta",
            "El Consejo de Facultad decidió sobre la cancelación del trabajo \"%1$s\": %2$s.", ESTUDIANTES,
            DIRECCION),
    TRABAJO_ANULADO(ALERTA, "Trabajo de grado anulado",
            "El director anuló el trabajo \"%1$s\" y quedó archivado. Justificación: %2$s", ESTUDIANTES, DIRECCION,
            EVALUADORES),

    // ----- Tarea diaria del sistema -----
    ETAPA_ACTUALIZADA(INFO, "Cambio de etapa",
            "El trabajo \"%1$s\" pasó a la etapa %2$s.", ESTUDIANTES, DIRECCION),
    TRABAJO_CERRADO_POR_PLAZO(ERROR, "Trabajo cerrado por vencimiento",
            "El trabajo \"%1$s\" quedó no aprobado: %2$s.", ESTUDIANTES, DIRECCION, CONSEJO),
    PLAZO_PROPUESTA_POR_VENCER(ALERTA, "Recordatorio de concepto de propuesta",
            "Faltan %2$s días para que venza el plazo del concepto sobre la propuesta \"%1$s\".", COMITE),
    PLAZO_PROPUESTA_VENCIDO(ERROR, "Plazo de concepto vencido",
            "Venció el plazo del concepto sobre la propuesta \"%1$s\".", COMITE),
    PLAZO_EVALUACION_POR_VENCER(ALERTA, "Recordatorio de evaluación de anteproyecto",
            "Faltan %2$s días para que venza el plazo de evaluación del anteproyecto \"%1$s\".",
            EVALUADORES_PENDIENTES),
    PLAZO_EVALUACION_VENCIDO(ERROR, "Plazo de evaluación vencido",
            "Venció el plazo de evaluación del anteproyecto \"%1$s\".", EVALUADORES_PENDIENTES),
    PLAZO_TRABAJO_POR_VENCER(ALERTA, "Recordatorio de entrega final",
            "Faltan %2$s días para que termine el plazo de ejecución del trabajo \"%1$s\".", ESTUDIANTES,
            DIRECCION);

    private final TipoNotificacion tipo;
    private final String titulo;
    private final String plantilla;
    private final Set<Destinatario> destinatarios;

    EventoTrabajoGrado(TipoNotificacion tipo, String titulo, String plantilla, Destinatario... destinatarios) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.plantilla = plantilla;
        this.destinatarios = EnumSet.of(destinatarios[0], destinatarios);
    }

    /**
     * @param tituloTrabajo título del trabajo de grado
     * @param detalle       dato propio del hecho (resultado, fecha...), puede ser null
     * @return el texto de la notificación
     */
    public String mensaje(String tituloTrabajo, String detalle) {
        return String.format(this.plantilla, tituloTrabajo, detalle == null ? "" : detalle);
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public Set<Destinatario> getDestinatarios() {
        return destinatarios;
    }
}
