package com.sgtg.backend.infraestructure.output.percistence.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.sgtg.backend.domain.models.comun.NombrePersona;
import com.sgtg.backend.domain.models.comun.Decision;
import com.sgtg.backend.domain.models.comun.Organo;
import com.sgtg.backend.domain.models.comun.ResultadoConcepto;
import com.sgtg.backend.domain.models.participantes.Asesor;
import com.sgtg.backend.domain.models.participantes.Departamento;
import com.sgtg.backend.domain.models.participantes.Director;
import com.sgtg.backend.domain.models.participantes.Estudiante;
import com.sgtg.backend.domain.models.participantes.Programa;
import com.sgtg.backend.domain.models.participantes.TipoVinculacion;
import com.sgtg.backend.domain.models.trabajo_grado.Documento;
import com.sgtg.backend.domain.models.trabajo_grado.Estado;
import com.sgtg.backend.domain.models.trabajo_grado.PracticaProfesional;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.domain.models.trabajo_grado.TipoDocumento;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoGrado;
import com.sgtg.backend.domain.models.trabajo_grado.TrabajoInvestigacion;
import com.sgtg.backend.domain.services.CalendarioHabilFalso;

/** Guarda y recupera el agregado completo en H2 con el gateway y el mapper reales. */
@DataJpaTest
@Import(ManageTrabajoGradoGatewayImplAdapter.class)
class ManageTrabajoGradoGatewayImplAdapterTest {

    private static final Departamento SISTEMAS = new Departamento(1L, "Sistemas");
    private static final Programa ING_SISTEMAS = new Programa(1L, "Ingeniería de Sistemas");
    private static final LocalDate LUNES = LocalDate.of(2026, 3, 2);

    @Autowired
    private ManageTrabajoGradoGatewayImplAdapter gateway;

    private static Estudiante estudiante(long id) {
        return new Estudiante(id, "E" + id, new NombrePersona("Estudiante", null, "Prueba", null), "e" + id + "@unicauca.edu.co", ING_SISTEMAS);
    }

    private static Director director(long id, TipoVinculacion vinculacion) {
        return new Director(id, "D" + id, new NombrePersona("Docente", null, "Prueba", null), "d" + id + "@unicauca.edu.co", vinculacion, SISTEMAS);
    }

    @Test
    void guardaYRecuperaUnTrabajoConSuPropuestaEHistorial() {
        TrabajoGrado nuevo = new TrabajoInvestigacion(null, "Sistema de gestión", List.of("Objetivo 1", "Objetivo 2"),
                List.of(estudiante(10L), estudiante(11L)), director(1L, TipoVinculacion.CATEDRA),
                director(2L, TipoVinculacion.PLANTA), null);
        TrabajoGrado guardado = gateway.guardar(nuevo);
        assertNotNull(guardado.getId());

        // Radicar con correcciones, nueva versión y aprobación
        guardado.radicarPropuesta("https://archivos.unicauca.edu.co/v1.pdf", LUNES, new CalendarioHabilFalso());
        guardado.registrarDecisionComite(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.NO_APROBADO,
                "Acta 1", "actas/2026/acta-01.pdf", "Ajustar alcance", LUNES.plusDays(2)), "C001",
                LUNES.plusDays(2).atTime(9, 0));
        guardado = gateway.guardar(guardado);
        guardado.radicarPropuesta("https://archivos.unicauca.edu.co/v2.pdf", LUNES.plusDays(7),
                new CalendarioHabilFalso());
        guardado.registrarDecisionComite(new Decision(Organo.COMITE_PROGRAMA, ResultadoConcepto.APROBADO, "Acta 2",
                null, null, LUNES.plusDays(9)), "C001", LUNES.plusDays(9).atTime(9, 0));
        gateway.guardar(guardado);

        TrabajoGrado leido = gateway.buscarPorId(guardado.getId());

        assertInstanceOf(TrabajoInvestigacion.class, leido);
        assertEquals(Proceso.ANTEPROYECTO, leido.getEtapa());
        assertEquals(List.of("Objetivo 1", "Objetivo 2"), leido.getObjetivos());
        assertEquals(List.of(10L, 11L), leido.getEstudiantes().stream().map(Estudiante::getUsuarioId).toList());
        assertEquals(ING_SISTEMAS, leido.getEstudiantes().get(0).getPrograma());
        assertEquals(TipoVinculacion.CATEDRA, leido.getDirector().getTipoVinculacion());
        assertEquals(2L, leido.getCodirector().getUsuarioId());
        assertNull(leido.getAsesor());
        assertEquals(Estado.APROBADO, leido.getPropuesta().getEstado());
        assertEquals(List.of(1, 2), leido.getPropuesta().getVersiones().stream().map(Documento::version).toList());
        assertEquals(2, leido.getPropuesta().getDecisiones().size());
        assertEquals(ResultadoConcepto.NO_APROBADO, leido.getPropuesta().getDecisiones().get(0).resultado());
        assertEquals("actas/2026/acta-01.pdf", leido.getPropuesta().getDecisiones().get(0).rutaArchivo());
        assertNull(leido.getPropuesta().getDecisiones().get(1).rutaArchivo());
        assertEquals("Estudiante Prueba", leido.getEstudiantes().get(0).getNombreCompleto());
        assertEquals(1, leido.getHistorial().size());
        assertEquals(Proceso.ANTEPROYECTO, leido.getHistorial().get(0).destino());
    }

    @Test
    void guardaLaPracticaProfesionalConCartaYAsesorExterno() {
        Asesor externo = new Asesor(null, "1061000000", new NombrePersona("Asesor", null, "Externo", null), "a@empresa.com", "Empresa S.A.S.", true);
        Documento carta = Documento.inicial(TipoDocumento.CARTA_ACEPTACION, "https://archivos.unicauca.edu.co/c.pdf",
                LUNES);
        TrabajoGrado guardado = gateway.guardar(new PracticaProfesional(null, "Práctica", List.of("Objetivo"),
                estudiante(10L), director(1L, TipoVinculacion.PLANTA), null, externo, carta));

        PracticaProfesional leida = assertInstanceOf(PracticaProfesional.class,
                gateway.buscarPorId(guardado.getId()));
        assertEquals(carta, leida.getCartaAceptacion());
        assertNull(leida.getAsesor().getUsuarioId());
        assertEquals("Empresa S.A.S.", leida.getAsesor().getOrganizacion());
        assertTrue(leida.getAsesor().isExterno());
        assertNull(leida.getCodirector());
        assertNull(leida.getPropuesta().getEstado());
    }

    @Test
    void listaLosTrabajosDondeParticipaElUsuario() {
        TrabajoGrado t1 = gateway.guardar(new TrabajoInvestigacion(null, "T1", List.of("O"), List.of(estudiante(10L)),
                director(1L, TipoVinculacion.PLANTA), null, null));
        TrabajoGrado t2 = gateway.guardar(new TrabajoInvestigacion(null, "T2", List.of("O"),
                List.of(estudiante(20L), estudiante(21L)), director(2L, TipoVinculacion.PLANTA), null, null));

        assertEquals(List.of(t1.getId()), gateway.listarPorParticipante(10L).stream().map(TrabajoGrado::getId).toList());
        assertEquals(List.of(t2.getId()), gateway.listarPorParticipante(21L).stream().map(TrabajoGrado::getId).toList());
        assertEquals(List.of(t2.getId()), gateway.listarPorParticipante(2L).stream().map(TrabajoGrado::getId).toList());
        assertTrue(gateway.listarPorParticipante(99L).isEmpty());
        assertNull(gateway.buscarPorId(404L));
    }
}
