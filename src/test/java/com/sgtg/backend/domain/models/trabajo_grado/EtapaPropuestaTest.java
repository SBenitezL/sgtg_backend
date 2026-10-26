package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.LUNES;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.RUTA_PROPUESTA;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.decisionComite;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Flujo de la etapa de propuesta (Arts. 10-11, 32-33). */
class EtapaPropuestaTest {

    private final CalendarioHabil calendario = new CalendarioHabilFalso();
    private final LocalDateTime ahora = LUNES.plusDays(5).atTime(10, 0);
    private TrabajoInvestigacion trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = DatosPrueba.investigacion();
    }

    @Test
    void laDescripcionYLaLineaSonOpcionales() {
        trabajo.describir("  Plataforma web  ", " ");
        assertEquals("Plataforma web", trabajo.getDescripcion());
        assertEquals(null, trabajo.getLineaInvestigacion());
    }

    @Test
    void laPropuestaSeCorrigeAntesDeRadicarYTrasUnNoAprobado() {
        trabajo.actualizarPropuesta("Nuevo título", java.util.List.of("Nuevo objetivo"), "Descripción", "IA");
        assertEquals("Nuevo título", trabajo.getTitulo());
        assertEquals("IA", trabajo.getLineaInvestigacion());

        // Mientras el Comité la revisa no se corrige
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.edicion_no_permitida", () -> trabajo.actualizarPropuesta("Otro",
                java.util.List.of("O"), null, null));

        // Después de un NO APROBADO sí
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(3)), "C001",
                LUNES.plusDays(3).atTime(9, 0));
        trabajo.actualizarPropuesta("Corregido", java.util.List.of("Objetivo"), null, null);
        assertEquals("Corregido", trabajo.getTitulo());
        assertRegla("error.dominio.trabajo_grado.titulo_obligatorio",
                () -> trabajo.actualizarPropuesta(" ", java.util.List.of("O"), null, null));
    }

    @Test
    void laCartaDeAceptacionSeCorrigeComoNuevaVersionCuandoLaPropuestaSePuedeCorregir() {
        PracticaProfesional practica = new PracticaProfesional(null, "Título", DatosPrueba.OBJETIVOS,
                DatosPrueba.estudiante(10L), DatosPrueba.directorPlanta(1L), null, DatosPrueba.asesorOrganizacion(),
                DatosPrueba.cartaAceptacion());
        int inicial = practica.getCartaAceptacion().version();

        // Antes de radicar
        practica.actualizarCartaAceptacion("9/CARTA_ACEPTACION/v2/carta.pdf", LUNES);
        assertEquals(inicial + 1, practica.getCartaAceptacion().version());
        assertEquals("9/CARTA_ACEPTACION/v2/carta.pdf", practica.getCartaAceptacion().ruta());
        assertEquals(TipoDocumento.CARTA_ACEPTACION, practica.getCartaAceptacion().tipo());

        // Mientras el Comité la revisa no se corrige
        practica.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.edicion_no_permitida",
                () -> practica.actualizarCartaAceptacion("9/CARTA_ACEPTACION/v3/carta.pdf", LUNES));

        // Después de un NO APROBADO sí; sin archivo se rechaza
        practica.registrarDecisionComite(decisionComite(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(3)), "C001",
                LUNES.plusDays(3).atTime(9, 0));
        assertRegla("error.dominio.practica.carta_aceptacion_obligatoria",
                () -> practica.actualizarCartaAceptacion(null, LUNES.plusDays(4)));
        practica.actualizarCartaAceptacion("9/CARTA_ACEPTACION/v3/carta.pdf", LUNES.plusDays(4));
        assertEquals(inicial + 2, practica.getCartaAceptacion().version());
        assertEquals(LUNES.plusDays(4), practica.getCartaAceptacion().fechaCarga());
    }

    @Test
    void enPracticaProfesionalLaPropuestaCorregidaSeRadicaConUnaNuevaCartaDeAceptacion() {
        PracticaProfesional practica = new PracticaProfesional(null, "Título", DatosPrueba.OBJETIVOS,
                DatosPrueba.estudiante(10L), DatosPrueba.directorPlanta(1L), null, DatosPrueba.asesorOrganizacion(),
                DatosPrueba.cartaAceptacion());
        practica.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        practica.registrarDecisionComite(decisionComite(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(3)), "C001",
                LUNES.plusDays(3).atTime(9, 0));

        // Sin la nueva carta no se radica la corrección, aunque la carta vigente sea del mismo día
        assertRegla("error.dominio.practica.carta_aceptacion_corregida",
                () -> practica.radicarPropuesta(RUTA_PROPUESTA, LUNES.plusDays(3), calendario));
        practica.actualizarCartaAceptacion("9/CARTA_ACEPTACION/v2/carta.pdf", LUNES.plusDays(4));
        assertRegla("error.dominio.practica.carta_aceptacion_corregida",
                () -> practica.radicarPropuesta(RUTA_PROPUESTA, LUNES.plusDays(4), calendario));

        practica.radicarPropuesta(RUTA_PROPUESTA, "9/CARTA_ACEPTACION/v3/carta.pdf", LUNES.plusDays(4), calendario);
        assertEquals(Estado.EN_REVISION, practica.getPropuesta().getEstado());
        assertEquals("9/CARTA_ACEPTACION/v3/carta.pdf", practica.getCartaAceptacion().ruta());
    }

    @Test
    void radicarCreaLaPrimeraVersionYCalculaElPlazoDelComite() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);

        Propuesta propuesta = trabajo.getPropuesta();
        assertEquals(Estado.EN_REVISION, propuesta.getEstado());
        assertEquals(1, propuesta.versionActual().get().version());
        assertEquals(TipoDocumento.PROPUESTA, propuesta.versionActual().get().tipo());
        // 10 días hábiles desde el lunes 2-mar-2026 (sin fines de semana)
        assertEquals(LocalDate.of(2026, 3, 16), propuesta.getFechaLimiteConcepto());
        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
    }

    @Test
    void propuestaAprobadaAvanzaAAnteproyectoYQuedaEnElHistorial() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(5)), "C001", ahora);

        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertTrue(trabajo.getPropuesta().estaAprobada());
        assertEquals(Estado.APROBADO, trabajo.getPropuesta().getEstado());
        assertEquals(1, trabajo.getHistorial().size());
        CambioEtapa cambio = trabajo.getHistorial().get(0);
        assertEquals(Proceso.PROPUESTA, cambio.origen());
        assertEquals(Proceso.ANTEPROYECTO, cambio.destino());
        assertEquals("C001", cambio.responsable());
        assertEquals(ahora, cambio.fecha());
    }

    @Test
    void noAprobadaSeRadicaUnaNuevaVersionHastaSerAprobada() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(3)),
                "C001", ahora);

        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
        assertEquals(Estado.NO_APROBADO, trabajo.getPropuesta().getEstado());
        assertTrue(trabajo.getHistorial().isEmpty());

        trabajo.radicarPropuesta("1/tg/propuesta_v2.pdf", LUNES.plusDays(7),
                calendario);
        assertEquals(2, trabajo.getPropuesta().versionActual().get().version());
        assertEquals(Estado.EN_REVISION, trabajo.getPropuesta().getEstado());

        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(10)), "C001",
                ahora);
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals(2, trabajo.getPropuesta().getDecisiones().size());
        // Cada versión conserva el estado de su revisión
        assertEquals(java.util.List.of(Estado.NO_APROBADO, Estado.APROBADO),
                trabajo.getPropuesta().getVersiones().stream().map(VersionDocumento::estado).toList());
    }

    @Test
    void laPropuestaNoAprobadaNoRecibeOtroConceptoSinUnaNuevaVersion() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(3)),
                "C001", ahora);

        // Solo la versión EN_REVISION recibe concepto del Comité
        assertRegla("error.dominio.propuesta.decision_no_permitida", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(4)), "C001", ahora));
        assertEquals(Estado.NO_APROBADO, trabajo.getPropuesta().getEstado());
        assertEquals(1, trabajo.getPropuesta().getDecisiones().size());
    }

    @ParameterizedTest
    @EnumSource(value = ResultadoConcepto.class, names = { "APROBADO", "NO_APROBADO" },
            mode = EnumSource.Mode.EXCLUDE)
    void elComiteSoloApruebaONoAprueba(ResultadoConcepto resultado) {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.resultado_invalido", () -> trabajo
                .registrarDecisionComite(decisionComite(resultado, LUNES.plusDays(1)), "C001", ahora));
        assertEquals(Estado.EN_REVISION, trabajo.getPropuesta().getEstado());
        assertTrue(trabajo.getPropuesta().getDecisiones().isEmpty());
    }

    @Test
    void laDecisionGuardaLaRutaDelArchivoDeSoporte() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        Decision decision = new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, "Acta 01",
                "actas/2026/acta-01.pdf", null, LUNES.plusDays(1));
        trabajo.registrarDecisionComite(decision, "C001", ahora);

        assertEquals("actas/2026/acta-01.pdf", trabajo.getPropuesta().getDecisiones().get(0).rutaArchivo());
    }

    @Test
    void laRespuestaDelComiteExigeElDocumento() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        for (ResultadoConcepto resultado : new ResultadoConcepto[] { ResultadoConcepto.APROBADO,
                ResultadoConcepto.NO_APROBADO })
            assertRegla("error.dominio.propuesta.documento_respuesta_obligatorio", () -> trabajo.registrarDecisionComite(
                    new Decision(Organo.COMITE_PROGRAMA, resultado, "Acta 01", null, "Obs", LUNES.plusDays(1)), "C001",
                    ahora));
        assertEquals(Estado.EN_REVISION, trabajo.getPropuesta().getEstado());
    }

    @Test
    void noSeRadicaMientrasHayUnaVersionPendienteDeConcepto() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.radicacion_no_permitida",
                () -> trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES.plusDays(1), calendario));
    }

    @Test
    void noHayDecisionSinPropuestaRadicada() {
        assertRegla("error.dominio.propuesta.decision_no_permitida", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES), "C001", ahora));
    }

    @Test
    void soloElComiteDeProgramaEmiteConceptoSobreLaPropuesta() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        Decision delConsejo = new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, "Res. 1", null, null, LUNES);
        assertRegla("error.dominio.propuesta.organo_invalido",
                () -> trabajo.registrarDecisionComite(delConsejo, "C001", ahora));
    }

    @Test
    void laDecisionNoPuedeSerAnteriorALaRadicacion() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.fecha_decision_invalida", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.minusDays(1)), "C001", ahora));
    }

    @Test
    void sinResponsableNoSeAvanzaNiSeModificaLaPropuesta() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.cambio_etapa.datos_incompletos", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(1)), " ", ahora));

        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
        assertEquals(Estado.EN_REVISION, trabajo.getPropuesta().getEstado());
        assertTrue(trabajo.getPropuesta().getDecisiones().isEmpty());
    }

    @Test
    void despuesDeAprobadaNoSeOperaSobreLaPropuesta() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(1)), "C001", ahora);

        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES.plusDays(2), calendario));
        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(2)), "C001", ahora));
    }

    @Test
    void radicarRequiereUnaRutaValida() {
        assertRegla("error.dominio.documento.ruta_invalida",
                () -> trabajo.radicarPropuesta("../propuesta.pdf", LUNES, calendario));
        assertEquals(null, trabajo.getPropuesta().getEstado());
    }

    @Test
    void conceptoVencidoSoloMientrasEstaPendiente() {
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        Propuesta propuesta = trabajo.getPropuesta();

        assertFalse(propuesta.conceptoVencido(LocalDate.of(2026, 3, 16)));
        assertTrue(propuesta.conceptoVencido(LocalDate.of(2026, 3, 17)));

        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LocalDate.of(2026, 3, 17)), "C001",
                ahora);
        assertFalse(propuesta.conceptoVencido(LocalDate.of(2026, 3, 20)));
    }

    @Test
    void laFechaLimiteDeCadaDecisionDelComiteSeCuentaDesdeLaRadicacionDeSuVersion() {
        // Versión 1: lunes 2 de marzo → 10 días hábiles: lunes 16; versión 2: lunes 9 → lunes 23
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(3)), "C001",
                LUNES.plusDays(3).atTime(9, 0));
        trabajo.radicarPropuesta(RUTA_PROPUESTA, LUNES.plusDays(7), calendario);

        Propuesta propuesta = trabajo.getPropuesta();
        assertEquals(java.util.Optional.of(LocalDate.of(2026, 3, 16)), propuesta.fechaLimiteDecision(0, calendario));
        assertEquals(java.util.Optional.of(LocalDate.of(2026, 3, 23)), propuesta.fechaLimiteDecision(1, calendario));
        assertEquals(java.util.Optional.empty(), propuesta.fechaLimiteDecision(2, calendario));
    }
}
