package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.asesorOrganizacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.cartaAceptacion;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.directorPlanta;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.estudiante;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

/** Trabajo en curso: cambio de título u objetivos, informes mensuales y vencimiento (Arts. 15-18, 21, 45). */
class EtapaEnCursoTest {

    /** Notificación de la aprobación del Consejo: viernes 20 de marzo de 2026 */
    private static final LocalDate NOTIFICACION = LocalDate.of(2026, 3, 20);
    private static final String RUTA_RESOLUCION = "resoluciones/2026/res-200.pdf";
    private static final String RUTA_ACTA_COMITE = "actas/2026/comite-30.pdf";
    private static final List<String> NUEVOS_OBJETIVOS = List.of("Implementar el módulo de sustentaciones");

    private TrabajoGrado trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    /** Trabajo reconstruido en curso, con la ejecución iniciada en la notificación. */
    private static TrabajoGrado enCurso(Modalidad modalidad) {
        Ejecucion ejecucion = Ejecucion.iniciar(NOTIFICACION, modalidad);
        boolean practica = modalidad == Modalidad.PRACTICA_PROFESIONAL;
        return TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, modalidad, "Sistema de gestión", OBJETIVOS,
                List.of(estudiante(10L)), directorPlanta(1L), null, practica ? asesorOrganizacion() : null,
                practica ? cartaAceptacion() : null, Proceso.EN_CURSO, null, null, ejecucion.datos(), List.of(),
                null));
    }

    private static Decision consejo(ResultadoConcepto resultado, LocalDate fecha) {
        return new Decision(Organo.CONSEJO_FACULTAD, resultado, null, RUTA_RESOLUCION, null, fecha);
    }

    private static Decision comite(ResultadoConcepto resultado, String observaciones, LocalDate fecha) {
        return new Decision(Organo.COMITE_PROGRAMA, resultado, null, RUTA_ACTA_COMITE, observaciones, fecha);
    }

    /** Solicita el cambio y el Comité lo avala, para que lo decida el Consejo. */
    private void solicitarYAvalar(LocalDate fecha) {
        solicitarCambio(fecha);
        trabajo.avalarCambioObjetivos(comite(ResultadoConcepto.APROBADO, null, fecha.plusDays(2)));
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = enCurso(Modalidad.TRABAJO_DE_INVESTIGACION);
    }

    private void solicitarCambio(LocalDate fecha) {
        trabajo.solicitarCambioObjetivos("Sistema de gestión y sustentación", NUEVOS_OBJETIVOS,
                "Se amplía el alcance acordado con el programa", fecha);
    }

    // ----- Cambio de título u objetivos -----

    @Test
    void elConsejoApruebaElCambioAvaladoYElTrabajoAdoptaLaNuevaDescripcion() {
        solicitarCambio(LocalDate.of(2026, 5, 4));
        CambioObjetivos pendiente = trabajo.getEjecucion().cambioPendiente().get();
        assertEquals("Sistema de gestión", pendiente.tituloAnterior());
        assertTrue(pendiente.pendienteDelComite());

        trabajo.avalarCambioObjetivos(comite(ResultadoConcepto.APROBADO, null, LocalDate.of(2026, 5, 8)));
        assertTrue(trabajo.getEjecucion().cambioPendiente().get().pendienteDelConsejo());

        trabajo.resolverCambioObjetivos(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 5, 15)));

        assertEquals("Sistema de gestión y sustentación", trabajo.getTitulo());
        assertEquals(NUEVOS_OBJETIVOS, trabajo.getObjetivos());
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        CambioObjetivos resuelto = trabajo.getEjecucion().getCambiosObjetivos().get(0);
        assertTrue(resuelto.fueAprobado());
        assertEquals(OBJETIVOS, resuelto.objetivosAnteriores());
        assertEquals(RUTA_RESOLUCION, resuelto.decision().rutaArchivo());
        assertEquals(RUTA_ACTA_COMITE, resuelto.avalComite().rutaArchivo());
        assertTrue(trabajo.getEjecucion().cambioPendiente().isEmpty());
    }

    @Test
    void unCambioNoAprobadoNoConsumeLaOportunidad() {
        solicitarYAvalar(LocalDate.of(2026, 5, 4));
        // El comentario del Consejo es opcional: la resolución lo soporta
        trabajo.resolverCambioObjetivos(consejo(ResultadoConcepto.NO_APROBADO, LocalDate.of(2026, 5, 15)));

        assertEquals("Sistema de gestión", trabajo.getTitulo());
        assertEquals(OBJETIVOS, trabajo.getObjetivos());

        solicitarCambio(LocalDate.of(2026, 6, 1));
        assertEquals(2, trabajo.getEjecucion().getCambiosObjetivos().size());
    }

    @Test
    void elCambioSoloSeRealizaUnaVez() {
        solicitarYAvalar(LocalDate.of(2026, 5, 4));
        trabajo.resolverCambioObjetivos(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 5, 15)));

        assertRegla("error.dominio.cambio_objetivos.ya_realizado", () -> trabajo.solicitarCambioObjetivos(
                "Otro título", OBJETIVOS, "Otra justificación", LocalDate.of(2026, 6, 1)));
    }

    @Test
    void noSeSolicitaOtroCambioMientrasHayaUnoPendiente() {
        solicitarCambio(LocalDate.of(2026, 5, 4));

        assertRegla("error.dominio.cambio_objetivos.solicitud_pendiente", () -> trabajo.solicitarCambioObjetivos(
                "Otro título", OBJETIVOS, "Otra justificación", LocalDate.of(2026, 5, 6)));
    }

    @Test
    void soloSeSolicitaDentroDeLosPrimerosSeisMeses() {
        assertEquals(LocalDate.of(2026, 9, 20), trabajo.getEjecucion().fechaLimiteCambioObjetivos());
        assertRegla("error.dominio.cambio_objetivos.fuera_de_plazo",
                () -> solicitarCambio(LocalDate.of(2026, 9, 21)));
        assertRegla("error.dominio.cambio_objetivos.fuera_de_plazo",
                () -> solicitarCambio(NOTIFICACION.minusDays(1)));

        solicitarCambio(LocalDate.of(2026, 9, 20));
        assertTrue(trabajo.getEjecucion().cambioPendiente().isPresent());
    }

    @Test
    void laSolicitudDebeCambiarAlgoYEstarJustificada() {
        LocalDate fecha = LocalDate.of(2026, 5, 4);
        assertRegla("error.dominio.cambio_objetivos.sin_cambios", () -> trabajo.solicitarCambioObjetivos(
                "Sistema de gestión", OBJETIVOS, "Sin cambios reales", fecha));
        assertRegla("error.dominio.cambio_objetivos.datos_incompletos", () -> trabajo.solicitarCambioObjetivos(
                "Nuevo título", OBJETIVOS, " ", fecha));
        assertRegla("error.dominio.cambio_objetivos.descripcion_obligatoria", () -> trabajo.solicitarCambioObjetivos(
                "Nuevo título", List.of(""), "Justificación", fecha));
        // Cambiar solo el título es válido
        trabajo.solicitarCambioObjetivos("Nuevo título", OBJETIVOS, "Justificación", fecha);
    }

    @Test
    void laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido() {
        LocalDate fecha = LocalDate.of(2026, 5, 15);
        assertRegla("error.dominio.cambio_objetivos.sin_solicitud_pendiente",
                () -> trabajo.resolverCambioObjetivos(consejo(ResultadoConcepto.APROBADO, fecha)));

        solicitarYAvalar(LocalDate.of(2026, 5, 4));
        assertRegla("error.dominio.cambio_objetivos.resolucion_obligatoria", () -> trabajo.resolverCambioObjetivos(
                new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, null, null, fecha)));
        assertRegla("error.dominio.cambio_objetivos.organo_invalido", () -> trabajo.resolverCambioObjetivos(
                new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, null, RUTA_RESOLUCION, null, fecha)));
        assertRegla("error.dominio.cambio_objetivos.resultado_invalido",
                () -> trabajo.resolverCambioObjetivos(consejo(ResultadoConcepto.CON_CORRECCIONES, fecha)));
        assertRegla("error.dominio.cambio_objetivos.fecha_invalida", () -> trabajo
                .resolverCambioObjetivos(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 5, 3))));
        // Ninguna decisión inválida modificó la solicitud
        assertTrue(trabajo.getEjecucion().cambioPendiente().isPresent());
        assertEquals("Sistema de gestión", trabajo.getTitulo());
    }

    @Test
    void noSeSolicitaOtroCambioMientrasEsteAvaladoYPendienteDelConsejo() {
        solicitarYAvalar(LocalDate.of(2026, 5, 4));

        assertRegla("error.dominio.cambio_objetivos.solicitud_pendiente", () -> trabajo.solicitarCambioObjetivos(
                "Otro título", OBJETIVOS, "Otra justificación", LocalDate.of(2026, 5, 10)));
    }

    @Test
    void siElComiteNoAvalaElCambioQuedaNoAprobadoYNoConsumeLaOportunidad() {
        solicitarCambio(LocalDate.of(2026, 5, 4));
        // Si no lo avala, las observaciones son obligatorias
        assertRegla("error.dominio.decision.observaciones_obligatorias", () -> trabajo.avalarCambioObjetivos(
                comite(ResultadoConcepto.NO_APROBADO, " ", LocalDate.of(2026, 5, 8))));

        trabajo.avalarCambioObjetivos(comite(ResultadoConcepto.NO_APROBADO, "No corresponde al programa",
                LocalDate.of(2026, 5, 8)));

        CambioObjetivos noAvalado = trabajo.getEjecucion().getCambiosObjetivos().get(0);
        assertFalse(noAvalado.estaPendiente());
        assertFalse(noAvalado.fueAprobado());
        assertNull(noAvalado.decision());
        assertEquals("Sistema de gestión", trabajo.getTitulo());
        // El Consejo ya no decide sobre ella
        assertRegla("error.dominio.cambio_objetivos.sin_solicitud_pendiente",
                () -> trabajo.resolverCambioObjetivos(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 5, 15))));
        // El director puede solicitar otro cambio
        solicitarCambio(LocalDate.of(2026, 6, 1));
        assertEquals(2, trabajo.getEjecucion().getCambiosObjetivos().size());
    }

    @Test
    void elConsejoSoloDecideDespuesDelAvalDelComite() {
        solicitarCambio(LocalDate.of(2026, 5, 4));

        assertRegla("error.dominio.cambio_objetivos.aval_comite_pendiente",
                () -> trabajo.resolverCambioObjetivos(consejo(ResultadoConcepto.APROBADO, LocalDate.of(2026, 5, 15))));
        assertTrue(trabajo.getEjecucion().cambioPendiente().get().pendienteDelComite());
    }

    @Test
    void elAvalLoDaElComiteUnaSolaVezConResultadoYFechaValidos() {
        LocalDate fecha = LocalDate.of(2026, 5, 8);
        assertRegla("error.dominio.cambio_objetivos.sin_aval_pendiente",
                () -> trabajo.avalarCambioObjetivos(comite(ResultadoConcepto.APROBADO, null, fecha)));

        solicitarCambio(LocalDate.of(2026, 5, 4));
        assertRegla("error.dominio.cambio_objetivos.organo_aval_invalido", () -> trabajo.avalarCambioObjetivos(
                new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, RUTA_RESOLUCION, null, fecha)));
        assertRegla("error.dominio.cambio_objetivos.resultado_invalido", () -> trabajo.avalarCambioObjetivos(
                comite(ResultadoConcepto.CON_CORRECCIONES, "Ajustar", fecha)));
        assertRegla("error.dominio.cambio_objetivos.fecha_invalida", () -> trabajo.avalarCambioObjetivos(
                comite(ResultadoConcepto.APROBADO, null, LocalDate.of(2026, 5, 3))));
        // El acta es opcional
        trabajo.avalarCambioObjetivos(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, null, null,
                null, fecha));

        assertRegla("error.dominio.cambio_objetivos.sin_aval_pendiente",
                () -> trabajo.avalarCambioObjetivos(comite(ResultadoConcepto.APROBADO, null, fecha)));
    }

    @Test
    void elCambioSoloSeSolicitaEnCurso() {
        TrabajoInvestigacion enPropuesta = DatosPrueba.investigacion();
        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> enPropuesta.solicitarCambioObjetivos(
                "Nuevo título", OBJETIVOS, "Justificación", LocalDate.of(2026, 5, 4)));
    }

    // ----- Informes mensuales -----

    /** Agenda la reunión del mes indicado a las 10:00 del día 20 (el informe se radica antes). */
    private static void agendar(TrabajoGrado trabajo, int numero, LocalDate dia) {
        trabajo.agendarReunion(numero, dia.atTime(10, 0), "Sala", "D1", NOTIFICACION);
    }

    @Test
    void laPracticaProfesionalRegistraUnInformePorReunion() {
        TrabajoGrado practica = enCurso(Modalidad.PRACTICA_PROFESIONAL);
        agendar(practica, 1, LocalDate.of(2026, 4, 20));
        agendar(practica, 2, LocalDate.of(2026, 5, 20));

        practica.registrarInformeMensual("informes/2026/abril.pdf", LocalDate.of(2026, 4, 18).atTime(9, 0));
        InformeMensual segundo = practica.registrarInformeMensual(" informes/2026/mayo.pdf ",
                LocalDate.of(2026, 5, 18).atTime(9, 0));

        assertEquals(2, segundo.numero());
        assertEquals("informes/2026/mayo.pdf", segundo.rutaArchivo());
        assertEquals(2, practica.getEjecucion().getInformesMensuales().size());
    }

    @Test
    void losInformesTambienAplicanALaInvestigacionYSeRevisanSinCambiarLaEtapa() {
        agendar(trabajo, 1, LocalDate.of(2026, 4, 20));
        InformeMensual informe = trabajo.registrarInformeMensual("informes/abril.pdf",
                LocalDate.of(2026, 4, 20).atTime(8, 0), "E001", "Avance de abril");
        assertEquals(1, informe.numero());
        assertEquals("E001", informe.radicadoPor());

        InformeMensual revisado = trabajo.revisarInformeMensual(1,
                new RevisionInforme(1L, "Director", "Bien", "informes/notas.pdf", LocalDate.of(2026, 4, 21)));
        assertEquals(1, revisado.revisiones().size());
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());
        // Una revisión por revisor, no anterior al informe y sobre un informe existente
        assertRegla("error.dominio.informe_mensual.revision_repetida", () -> trabajo.revisarInformeMensual(1,
                new RevisionInforme(1L, "Director", "Otra", null, LocalDate.of(2026, 4, 22))));
        assertRegla("error.dominio.informe_mensual.fecha_revision_invalida", () -> trabajo.revisarInformeMensual(1,
                new RevisionInforme(2L, "Codirector", "Antes", null, LocalDate.of(2026, 4, 19))));
        assertRegla("error.dominio.informe_mensual.no_encontrado", () -> trabajo.revisarInformeMensual(2,
                new RevisionInforme(2L, "Codirector", "No existe", null, LocalDate.of(2026, 4, 22))));
        assertRegla("error.dominio.informe_mensual.revision_vacia",
                () -> new RevisionInforme(2L, "Codirector", " ", null, LocalDate.of(2026, 4, 22)));
    }

    @Test
    void elInformeExigeElDocumentoYUnaFechaValida() {
        TrabajoGrado practica = enCurso(Modalidad.PRACTICA_PROFESIONAL);
        agendar(practica, 1, LocalDate.of(2026, 4, 20));

        assertRegla("error.dominio.informe_mensual.documento_obligatorio",
                () -> practica.registrarInformeMensual("  ", LocalDate.of(2026, 4, 18).atTime(9, 0)));
        assertRegla("error.dominio.informe_mensual.fecha_invalida",
                () -> practica.registrarInformeMensual("informes/marzo.pdf", NOTIFICACION.minusDays(1).atTime(9, 0)));
        assertTrue(practica.getEjecucion().getInformesMensuales().isEmpty());
    }

    // ----- Vencimiento -----

    @Test
    void alVencerElPlazoSinSustentacionQuedaNoAprobado() {
        assertFalse(trabajo.verificarVencimientos(LocalDate.of(2026, 12, 20).atTime(0, 5)));
        assertEquals(Proceso.EN_CURSO, trabajo.getEtapa());

        assertTrue(trabajo.verificarVencimientos(LocalDate.of(2026, 12, 21).atTime(0, 5)));

        assertEquals(Proceso.NO_APROBADO, trabajo.getEtapa());
        assertEquals(MotivoCierre.VENCIMIENTO_PLAZO, trabajo.getMotivoCierre());
        assertTrue(trabajo.estaCerrado());
        CambioEtapa cambio = trabajo.getHistorial().get(trabajo.getHistorial().size() - 1);
        assertEquals(Proceso.EN_CURSO, cambio.origen());
        assertEquals(TrabajoGrado.RESPONSABLE_SISTEMA, cambio.responsable());
        // Una segunda verificación no hace nada
        assertFalse(trabajo.verificarVencimientos(LocalDate.of(2026, 12, 22).atTime(0, 5)));
    }

    @Test
    void elVencimientoSoloAplicaAlTrabajoEnDesarrollo() {
        TrabajoInvestigacion enPropuesta = DatosPrueba.investigacion();

        assertFalse(enPropuesta.verificarVencimientos(LocalDate.of(2030, 1, 1).atTime(0, 5)));
        assertEquals(Proceso.PROPUESTA, enPropuesta.getEtapa());
        assertNull(enPropuesta.getMotivoCierre());
    }

    @Test
    void laEjecucionReconstruidaConservaCambiosEInformes() {
        TrabajoGrado practica = enCurso(Modalidad.PRACTICA_PROFESIONAL);
        practica.solicitarCambioObjetivos("Nueva práctica", OBJETIVOS, "Cambio de área", LocalDate.of(2026, 4, 1));
        practica.avalarCambioObjetivos(comite(ResultadoConcepto.APROBADO, null, LocalDate.of(2026, 4, 3)));
        agendar(practica, 1, LocalDate.of(2026, 4, 20));
        practica.registrarInformeMensual("informes/abril.pdf", LocalDate.of(2026, 4, 18).atTime(9, 0));

        DatosEjecucion datos = practica.getEjecucion().datos();
        Ejecucion reconstruida = new Ejecucion(datos);

        assertEquals(datos, reconstruida.datos());
        assertTrue(reconstruida.cambioPendiente().isPresent());
        assertEquals(1, reconstruida.getInformesMensuales().size());
    }
}
