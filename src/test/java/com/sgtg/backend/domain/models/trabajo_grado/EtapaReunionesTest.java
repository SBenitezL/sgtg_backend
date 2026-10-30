package com.sgtg.backend.domain.models.trabajo_grado;

import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.OBJETIVOS;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.directorPlanta;
import static com.sgtg.backend.domain.models.trabajo_grado.DatosPrueba.estudiante;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;

/** Reuniones de seguimiento mensuales del plazo de ejecución (Art. 45). */
class EtapaReunionesTest {

    /** Notificación 20-mar-2026; en investigación el plazo es de 9 meses y termina el 20-dic-2026 */
    private static final LocalDate NOTIFICACION = LocalDate.of(2026, 3, 20);
    private static final LocalDate FIN = LocalDate.of(2026, 12, 20);
    private static final LocalDateTime PRIMERA = LocalDate.of(2026, 4, 20).atTime(10, 0);

    private TrabajoGrado trabajo;

    private static void assertRegla(String clave, Executable accion) {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, accion);
        assertEquals(clave, error.getMessage());
    }

    private static Decision consejo(LocalDate fecha) {
        return new Decision(Organo.CONSEJO_FACULTAD, ResultadoConcepto.APROBADO, null, "resoluciones/r.pdf", null,
                fecha);
    }

    @BeforeEach
    void crearTrabajo() {
        trabajo = TrabajoGrado.reconstruir(new DatosTrabajoGrado(7L, Modalidad.TRABAJO_DE_INVESTIGACION,
                "Sistema de gestión", OBJETIVOS, List.of(estudiante(10L)), directorPlanta(1L), null, null, null,
                Proceso.EN_CURSO, null, null, Ejecucion.iniciar(NOTIFICACION, Modalidad.TRABAJO_DE_INVESTIGACION)
                        .datos(),
                List.of(), null));
    }

    private List<ReunionDelPlazo> reuniones(LocalDate hoy) {
        return trabajo.getEjecucion().reuniones(hoy);
    }

    // ----- Calendario -----

    @Test
    void hayUnaReunionPorCadaMesDelPlazo() {
        List<ReunionDelPlazo> calendario = reuniones(NOTIFICACION);

        assertEquals(9, calendario.size());
        assertEquals(LocalDate.of(2026, 4, 20), calendario.get(0).fechaSugerida());
        assertEquals(FIN, calendario.get(8).fechaSugerida());
        assertTrue(calendario.stream().allMatch(reunion -> reunion.estado() == EstadoReunion.PENDIENTE));
        assertTrue(calendario.stream().noneMatch(ReunionDelPlazo::conInforme));
    }

    @Test
    void laProrrogaSumaSusMeses() {
        trabajo.solicitarProrroga(2, "solicitudes/prorroga.pdf", LocalDate.of(2026, 9, 1));
        trabajo.resolverProrroga(consejo(LocalDate.of(2026, 9, 10)), "CF01", LocalDate.of(2026, 9, 10).atTime(9, 0));

        List<ReunionDelPlazo> calendario = reuniones(LocalDate.of(2026, 9, 10));
        assertEquals(11, calendario.size());
        assertEquals(LocalDate.of(2027, 2, 20), calendario.get(10).fechaSugerida());
    }

    @Test
    void conLaSuspensionLasReunionesQueQuedabanSeCorren() {
        trabajo.solicitarSuspension("solicitudes/suspension.pdf", LocalDate.of(2026, 10, 1));
        trabajo.resolverSuspension(consejo(LocalDate.of(2026, 10, 10)), "CF01",
                LocalDate.of(2026, 10, 10).atTime(10, 0));

        // Suspendido desde el 10-oct: la reunión del 20-oct en adelante aún no tiene fecha
        List<ReunionDelPlazo> suspendido = reuniones(LocalDate.of(2026, 11, 1));
        assertEquals(9, suspendido.size());
        assertEquals(LocalDate.of(2026, 9, 20), suspendido.get(5).fechaSugerida());
        assertNull(suspendido.get(6).fechaSugerida());
        assertEquals(EstadoReunion.EN_SUSPENSION, suspendido.get(6).estado());

        // Reanudado el 1-feb-2027: las que quedaban se corren los 114 días de la suspensión
        trabajo.reanudar("solicitudes/reanudacion.pdf", "D1", LocalDate.of(2027, 2, 1).atTime(10, 0));
        List<ReunionDelPlazo> reanudado = reuniones(LocalDate.of(2027, 2, 1));
        assertEquals(9, reanudado.size());
        assertEquals(LocalDate.of(2026, 9, 20), reanudado.get(5).fechaSugerida());
        assertEquals(LocalDate.of(2027, 2, 11), reanudado.get(6).fechaSugerida());
        // La última coincide con el nuevo fin del plazo
        assertEquals(trabajo.getEjecucion().getFechaFin(), reanudado.get(8).fechaSugerida());
    }

    @Test
    void cadaReunionSeAsociaAlInformeMensualDeSuMes() {
        trabajo.registrarInformeMensual("informes/1.pdf", LocalDate.of(2026, 4, 25));

        List<ReunionDelPlazo> calendario = reuniones(LocalDate.of(2026, 5, 1));
        assertTrue(calendario.get(0).conInforme());
        assertFalse(calendario.get(1).conInforme());
        // Pasó la fecha sin registrarse como realizada
        assertEquals(EstadoReunion.VENCIDA, calendario.get(0).estado());
        assertEquals(EstadoReunion.PENDIENTE, calendario.get(1).estado());
    }

    // ----- Agenda -----

    @Test
    void elDirectorAgendaReprogramaYRegistraLaReunion() {
        LocalDate hoy = LocalDate.of(2026, 4, 10);
        trabajo.agendarReunion(1, PRIMERA, "Sala de reuniones 2", "D1", hoy);
        assertEquals(EstadoReunion.AGENDADA, reuniones(hoy).get(0).estado());
        assertEquals("Sala de reuniones 2", reuniones(hoy).get(0).agendada().lugar());

        // Se reprograma para el día siguiente
        trabajo.agendarReunion(1, PRIMERA.plusDays(1), "https://meet.google.com/abc-defg-hij", "D1", hoy);
        assertEquals(1, trabajo.getEjecucion().getReuniones().size());
        assertEquals(PRIMERA.plusDays(1), reuniones(hoy).get(0).agendada().fechaHora());

        assertRegla("error.dominio.reunion.aun_no_ocurre",
                () -> trabajo.registrarReunionRealizada(1, null, PRIMERA));
        trabajo.registrarReunionRealizada(1, "Se revisó el avance del primer mes", PRIMERA.plusDays(1));

        ReunionDelPlazo realizada = reuniones(LocalDate.of(2026, 6, 1)).get(0);
        assertEquals(EstadoReunion.REALIZADA, realizada.estado());
        assertEquals("Se revisó el avance del primer mes", realizada.agendada().observaciones());
        assertRegla("error.dominio.reunion.ya_realizada",
                () -> trabajo.agendarReunion(1, PRIMERA.plusDays(5), "Sala", "D1", hoy));
        assertRegla("error.dominio.reunion.ya_realizada",
                () -> trabajo.registrarReunionRealizada(1, null, PRIMERA.plusDays(2)));
    }

    @Test
    void laReunionSeAgendaDentroDelPlazoEnUnaFechaNoPasadaYConLugar() {
        LocalDate hoy = LocalDate.of(2026, 4, 10);
        assertRegla("error.dominio.reunion.fuera_del_plazo",
                () -> trabajo.agendarReunion(10, PRIMERA, "Sala", "D1", hoy));
        assertRegla("error.dominio.reunion.fecha_pasada",
                () -> trabajo.agendarReunion(1, hoy.minusDays(1).atTime(10, 0), "Sala", "D1", hoy));
        assertRegla("error.dominio.reunion.datos_incompletos",
                () -> trabajo.agendarReunion(1, PRIMERA, " ", "D1", hoy));
        assertRegla("error.dominio.reunion.no_agendada",
                () -> trabajo.registrarReunionRealizada(2, null, PRIMERA));
        assertTrue(trabajo.getEjecucion().getReuniones().isEmpty());
    }

    @Test
    void soloSeAgendaConElTrabajoEnCursoOEnProrroga() {
        trabajo.solicitarSuspension("solicitudes/suspension.pdf", LocalDate.of(2026, 4, 1));
        trabajo.resolverSuspension(consejo(LocalDate.of(2026, 4, 5)), "CF01", LocalDate.of(2026, 4, 5).atTime(10, 0));

        assertRegla("error.dominio.trabajo_grado.etapa_invalida",
                () -> trabajo.agendarReunion(1, PRIMERA, "Sala", "D1", LocalDate.of(2026, 4, 10)));
    }

    @Test
    void lasReunionesSeReconstruyenConSuEstado() {
        trabajo.agendarReunion(1, PRIMERA, "Sala", "D1", LocalDate.of(2026, 4, 10));
        trabajo.registrarReunionRealizada(1, "Avance", PRIMERA);

        Ejecucion reconstruida = new Ejecucion(trabajo.getEjecucion().datos());
        assertEquals(trabajo.getEjecucion().getReuniones(), reconstruida.getReuniones());
    }
}
