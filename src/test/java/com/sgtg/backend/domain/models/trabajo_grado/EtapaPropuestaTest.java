package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.LUNES;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.URL_PROPUESTA;
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
    void radicarCreaLaPrimeraVersionYCalculaElPlazoDelComite() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);

        Propuesta propuesta = trabajo.getPropuesta();
        assertEquals(Estado.RADICADO, propuesta.getEstado());
        assertEquals(1, propuesta.versionActual().get().version());
        assertEquals(TipoDocumento.PROPUESTA, propuesta.versionActual().get().tipo());
        // 10 días hábiles desde el lunes 2-mar-2026 (sin fines de semana)
        assertEquals(LocalDate.of(2026, 3, 16), propuesta.getFechaLimiteConcepto());
        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
    }

    @Test
    void propuestaAprobadaAvanzaAAnteproyectoYQuedaEnElHistorial() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(5)), "C001", ahora);

        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertTrue(trabajo.getPropuesta().estaAprobada());
        assertEquals(1, trabajo.getHistorial().size());
        CambioEtapa cambio = trabajo.getHistorial().get(0);
        assertEquals(Proceso.PROPUESTA, cambio.origen());
        assertEquals(Proceso.ANTEPROYECTO, cambio.destino());
        assertEquals("C001", cambio.responsable());
        assertEquals(ahora, cambio.fecha());
    }

    @Test
    void noAprobadaSeRadicaUnaNuevaVersionHastaSerAprobada() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.NO_APROBADO, LUNES.plusDays(3)),
                "C001", ahora);

        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
        assertEquals(Estado.NO_APROBADO, trabajo.getPropuesta().getEstado());
        assertTrue(trabajo.getHistorial().isEmpty());

        trabajo.radicarPropuesta("https://archivos.unicauca.edu.co/tg/propuesta_v2.pdf", LUNES.plusDays(7),
                calendario);
        assertEquals(2, trabajo.getPropuesta().versionActual().get().version());
        assertEquals(Estado.RADICADO, trabajo.getPropuesta().getEstado());

        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(10)), "C001",
                ahora);
        assertEquals(Proceso.ANTEPROYECTO, trabajo.getEtapa());
        assertEquals(2, trabajo.getPropuesta().getDecisiones().size());
    }

    @ParameterizedTest
    @EnumSource(value = ResultadoConcepto.class, names = { "APROBADO", "NO_APROBADO" },
            mode = EnumSource.Mode.EXCLUDE)
    void elComiteSoloApruebaONoAprueba(ResultadoConcepto resultado) {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.resultado_invalido", () -> trabajo
                .registrarDecisionComite(decisionComite(resultado, LUNES.plusDays(1)), "C001", ahora));
        assertEquals(Estado.RADICADO, trabajo.getPropuesta().getEstado());
        assertTrue(trabajo.getPropuesta().getDecisiones().isEmpty());
    }

    @Test
    void laDecisionGuardaLaRutaDelArchivoDeSoporte() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        Decision decision = new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, "Acta 01",
                "actas/2026/acta-01.pdf", null, LUNES.plusDays(1));
        trabajo.registrarDecisionComite(decision, "C001", ahora);

        assertEquals("actas/2026/acta-01.pdf", trabajo.getPropuesta().getDecisiones().get(0).rutaArchivo());
    }

    @Test
    void noSeRadicaMientrasHayUnaVersionPendienteDeConcepto() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.radicacion_no_permitida",
                () -> trabajo.radicarPropuesta(URL_PROPUESTA, LUNES.plusDays(1), calendario));
    }

    @Test
    void noHayDecisionSinPropuestaRadicada() {
        assertRegla("error.dominio.propuesta.decision_no_permitida", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES), "C001", ahora));
    }

    @Test
    void soloElComiteDeProgramaEmiteConceptoSobreLaPropuesta() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        Decision delConsejo = new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, "Res. 1", null, null, LUNES);
        assertRegla("error.dominio.propuesta.organo_invalido",
                () -> trabajo.registrarDecisionComite(delConsejo, "C001", ahora));
    }

    @Test
    void laDecisionNoPuedeSerAnteriorALaRadicacion() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.propuesta.fecha_decision_invalida", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.minusDays(1)), "C001", ahora));
    }

    @Test
    void sinResponsableNoSeAvanzaNiSeModificaLaPropuesta() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        assertRegla("error.dominio.cambio_etapa.datos_incompletos", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(1)), " ", ahora));

        assertEquals(Proceso.PROPUESTA, trabajo.getEtapa());
        assertEquals(Estado.RADICADO, trabajo.getPropuesta().getEstado());
        assertTrue(trabajo.getPropuesta().getDecisiones().isEmpty());
    }

    @Test
    void despuesDeAprobadaNoSeOperaSobreLaPropuesta() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(1)), "C001", ahora);

        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.radicarPropuesta(URL_PROPUESTA, LUNES.plusDays(2), calendario));
        assertRegla("error.dominio.trabajo_grado.etapa_invalida", () -> trabajo
                .registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LUNES.plusDays(2)), "C001", ahora));
    }

    @Test
    void radicarRequiereUnaUrlValida() {
        assertRegla("error.dominio.documento.url_invalida",
                () -> trabajo.radicarPropuesta("propuesta.pdf", LUNES, calendario));
        assertEquals(null, trabajo.getPropuesta().getEstado());
    }

    @Test
    void conceptoVencidoSoloMientrasEstaPendiente() {
        trabajo.radicarPropuesta(URL_PROPUESTA, LUNES, calendario);
        Propuesta propuesta = trabajo.getPropuesta();

        assertFalse(propuesta.conceptoVencido(LocalDate.of(2026, 3, 16)));
        assertTrue(propuesta.conceptoVencido(LocalDate.of(2026, 3, 17)));

        trabajo.registrarDecisionComite(decisionComite(ResultadoConcepto.APROBADO, LocalDate.of(2026, 3, 17)), "C001",
                ahora);
        assertFalse(propuesta.conceptoVencido(LocalDate.of(2026, 3, 20)));
    }
}
