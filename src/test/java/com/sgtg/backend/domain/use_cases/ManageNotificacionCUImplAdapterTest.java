package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgtg.backend.application.output.ManageNotificacionGatewayIntPort;
import com.sgtg.backend.domain.exceptions.ReglaNegocioException;
import com.sgtg.backend.domain.models.notificacion.EventoTrabajoGrado;
import com.sgtg.backend.domain.models.notificacion.Notificacion;
import com.sgtg.backend.domain.models.notificacion.TipoNotificacion;
import com.sgtg.backend.domain.models.trabajo_grado.Proceso;
import com.sgtg.backend.infraestructure.output.exception.own.EntityNotFoundException;
import com.sgtg.backend.infraestructure.output.exception.own.NoAccessException;
import com.sgtg.backend.infraestructure.output.formatter.ExceptionFormatterImplAdapter;

/** Cada usuario solo consulta y gestiona sus propias notificaciones. */
class ManageNotificacionCUImplAdapterTest {

    private ManageNotificacionGatewayIntPort gateway;
    private ManageNotificacionCUImplAdapter casoDeUso;

    private static Notificacion notificacion(long id, long usuarioId, boolean leida) {
        return new Notificacion(id, usuarioId, TipoNotificacion.INFO, "Título", "Mensaje", 7L, Proceso.PROPUESTA,
                EventoTrabajoGrado.PROPUESTA_RADICADA, null, LocalDateTime.of(2026, 3, 2, 10, 0), leida);
    }

    @BeforeEach
    void preparar() {
        gateway = mock(ManageNotificacionGatewayIntPort.class);
        casoDeUso = new ManageNotificacionCUImplAdapter(gateway, new ExceptionFormatterImplAdapter());
        when(gateway.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void listaLasDelUsuario() {
        List<Notificacion> propias = List.of(notificacion(1L, 10L, false));
        when(gateway.listarPorUsuario(10L)).thenReturn(propias);
        assertSame(propias, casoDeUso.listarMias(10L));
    }

    @Test
    void marcaComoLeidaSoloLaPropia() {
        when(gateway.buscarPorId(1L)).thenReturn(notificacion(1L, 10L, false));

        assertThrows(NoAccessException.class, () -> casoDeUso.marcarLeida(1L, 11L));
        assertTrue(casoDeUso.marcarLeida(1L, 10L).leida());
        verify(gateway).guardar(notificacion(1L, 10L, true));
    }

    @Test
    void unaYaLeidaNoSeVuelveAGuardar() {
        when(gateway.buscarPorId(1L)).thenReturn(notificacion(1L, 10L, true));
        casoDeUso.marcarLeida(1L, 10L);
        verify(gateway, never()).guardar(any());
    }

    @Test
    void eliminaSoloLaPropia() {
        when(gateway.buscarPorId(1L)).thenReturn(notificacion(1L, 10L, false));

        assertThrows(NoAccessException.class, () -> casoDeUso.eliminar(1L, 11L));
        verify(gateway, never()).eliminar(1L);
        casoDeUso.eliminar(1L, 10L);
        verify(gateway).eliminar(1L);
    }

    @Test
    void inexistente() {
        assertThrows(EntityNotFoundException.class, () -> casoDeUso.marcarLeida(404L, 10L));
    }

    @Test
    void lasOperacionesMasivasSonDelUsuario() {
        casoDeUso.marcarTodasLeidas(10L);
        casoDeUso.eliminarTodas(10L);
        verify(gateway).marcarTodasLeidas(10L);
        verify(gateway).eliminarPorUsuario(10L);
    }

    @Test
    void unaNotificacionExigeDestinatarioYTexto() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class, () -> Notificacion.nueva(null,
                TipoNotificacion.INFO, "T", "M", 7L, Proceso.PROPUESTA, null, null, LocalDateTime.now()));
        assertEquals("error.dominio.notificacion.datos_incompletos", error.getMessage());
    }
}
