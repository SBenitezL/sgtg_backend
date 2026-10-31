package com.sgtg.backend.domain.use_cases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sgtg.backend.application.output.ManageAlertaGatewayIntPort;
import com.sgtg.backend.application.output.ManageArchivoGatewayIntPort;
import com.sgtg.backend.application.output.ManageAuditoriaGatewayIntPort;
import com.sgtg.backend.domain.models.acceso.IntentosAcceso;
import com.sgtg.backend.domain.models.auditoria.AlertaSeguridad;
import com.sgtg.backend.domain.models.auditoria.TipoAlerta;
import com.sgtg.backend.domain.models.auditoria.VerificacionCadena;

class ManageSeguridadCUImplAdapterTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-10-30T20:00:00Z");

    private ManageAuditoriaGatewayIntPort auditoria;
    private ManageArchivoGatewayIntPort archivos;
    private ManageAlertaGatewayIntPort alertas;
    private ManageSeguridadCUImplAdapter seguridad;

    @BeforeEach
    void preparar() {
        auditoria = mock(ManageAuditoriaGatewayIntPort.class);
        archivos = mock(ManageArchivoGatewayIntPort.class);
        alertas = mock(ManageAlertaGatewayIntPort.class);
        when(alertas.alertar(any())).thenReturn(true);
        when(auditoria.verificarCadena()).thenReturn(new VerificacionCadena(10, "sello", null));
        seguridad = new ManageSeguridadCUImplAdapter(auditoria, archivos, alertas, Clock.fixed(AHORA, BOGOTA));
    }

    private List<AlertaSeguridad> avisadas() {
        ArgumentCaptor<AlertaSeguridad> alerta = ArgumentCaptor.forClass(AlertaSeguridad.class);
        verify(alertas, atLeastOnce()).alertar(alerta.capture());
        return alerta.getAllValues();
    }

    @Test
    void revisaLosUltimosQuinceMinutosConLosUmbralesDeCadaAlerta() {
        LocalDateTime desde = LocalDateTime.ofInstant(AHORA, BOGOTA).minusMinutes(15);
        seguridad.detectarAlertas();

        verify(auditoria).ipsConLoginsFallidos(desde, ManageSeguridadCUImplAdapter.LOGINS_FALLIDOS_POR_IP);
        verify(auditoria).correosConLoginsFallidos(desde, IntentosAcceso.MAXIMO_FALLOS);
        verify(auditoria).usuariosConAccesosDenegados(desde, ManageSeguridadCUImplAdapter.ACCESOS_DENEGADOS);
        verify(auditoria).sesionesRechazadas(desde);
        verify(alertas, never()).alertar(any());
    }

    @Test
    void avisaCadaHechoConSuTipoYClave() {
        when(auditoria.ipsConLoginsFallidos(any(), anyLong())).thenReturn(List.of("10.0.0.9"));
        when(auditoria.correosConLoginsFallidos(any(), anyLong())).thenReturn(List.of("a@unicauca.edu.co"));
        when(auditoria.usuariosConAccesosDenegados(any(), anyLong())).thenReturn(List.of("b@unicauca.edu.co"));
        when(auditoria.sesionesRechazadas(any())).thenReturn(List.of("sesion-1"));

        assertEquals(4, seguridad.detectarAlertas());

        assertEquals(List.of(TipoAlerta.FUERZA_BRUTA_IP, TipoAlerta.CORREO_BLOQUEADO, TipoAlerta.ACCESOS_DENEGADOS,
                TipoAlerta.SESION_CERRADA_USADA), avisadas().stream().map(AlertaSeguridad::tipo).toList());
        assertEquals(List.of("10.0.0.9", "a@unicauca.edu.co", "b@unicauca.edu.co", "sesion-1"),
                avisadas().stream().map(AlertaSeguridad::clave).toList());
    }

    @Test
    void lasAlertasRepetidasNoCuentanComoNuevas() {
        when(auditoria.ipsConLoginsFallidos(any(), anyLong())).thenReturn(List.of("10.0.0.9"));
        when(alertas.alertar(any())).thenReturn(false);
        assertEquals(0, seguridad.detectarAlertas());
    }

    @Test
    void laVerificacionAvisaLaCadenaRotaYLosArchivosAlterados() {
        assertTrue(seguridad.verificarIntegridad());
        verify(alertas, never()).alertar(any());

        when(auditoria.verificarCadena()).thenReturn(new VerificacionCadena(4, "s4", "el registro 5 fue modificado"));
        when(archivos.verificarHuellas()).thenReturn(List.of("15/TI-A/v1/a.pdf: ya no existe"));

        assertFalse(seguridad.verificarIntegridad());
        List<AlertaSeguridad> avisos = avisadas();
        assertEquals(TipoAlerta.AUDITORIA_ALTERADA, avisos.get(0).tipo());
        assertTrue(avisos.get(0).detalle().contains("el registro 5 fue modificado"));
        assertEquals(TipoAlerta.ARCHIVO_ALTERADO, avisos.get(1).tipo());
        verify(alertas).alertar(eq(avisos.get(1)));
    }
}
