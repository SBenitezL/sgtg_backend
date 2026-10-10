package com.sgtg.backend.infraestructure.input.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sgtg.backend.application.input.trabajo_grado.ManageTrabajoGradoCUIntPort;

/** La tarea programada delega en el caso de uso y no propaga errores. */
class VencimientosSchedulerTest {

    private final ManageTrabajoGradoCUIntPort casoDeUso = mock(ManageTrabajoGradoCUIntPort.class);
    private final VencimientosScheduler tarea = new VencimientosScheduler(casoDeUso);

    @Test
    void invocaLaVerificacionDeVencimientos() {
        when(casoDeUso.verificarVencimientos()).thenReturn(2);
        tarea.verificarVencimientos();
        verify(casoDeUso).verificarVencimientos();
    }

    @Test
    void unErrorNoDetieneLaTarea() {
        when(casoDeUso.verificarVencimientos()).thenThrow(new IllegalStateException("Base de datos caída"));
        assertDoesNotThrow(tarea::verificarVencimientos);
    }
}
