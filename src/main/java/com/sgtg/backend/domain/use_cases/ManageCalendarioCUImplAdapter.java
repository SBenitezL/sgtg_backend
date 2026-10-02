package com.sgtg.backend.domain.use_cases;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.sgtg.backend.application.input.calendario.ManageCalendarioCUIntPort;
import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.application.output.ManageCalendarioGatewayIntPort;
import com.sgtg.backend.domain.models.comun.DiaNoHabil;
import com.sgtg.backend.domain.services.FestivosColombia;

/** Casos de uso del calendario de días no hábiles. */
public class ManageCalendarioCUImplAdapter implements ManageCalendarioCUIntPort {

    private final ManageCalendarioGatewayIntPort calendarioGateway;
    private final ExceptionFormatterIntPort exceptionFormatter;

    public ManageCalendarioCUImplAdapter(ManageCalendarioGatewayIntPort calendarioGateway,
            ExceptionFormatterIntPort exceptionFormatter) {
        this.calendarioGateway = calendarioGateway;
        this.exceptionFormatter = exceptionFormatter;
    }

    @Override
    public DiaNoHabil registrarDiaNoHabil(LocalDate fecha, String descripcion) {
        DiaNoHabil dia = new DiaNoHabil(fecha, descripcion);
        if (this.calendarioGateway.existe(fecha))
            this.exceptionFormatter.returnResponseErrorEntityExists("error.calendario.dia_existente");
        return this.calendarioGateway.guardar(dia);
    }

    @Override
    public void eliminarDiaNoHabil(LocalDate fecha) {
        if (!this.calendarioGateway.existe(fecha))
            this.exceptionFormatter.returnResponseErrorEntityNotFound("error.calendario.dia_no_encontrado");
        this.calendarioGateway.eliminar(fecha);
    }

    @Override
    public List<DiaNoHabil> listarDiasNoHabiles(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde))
            this.exceptionFormatter.returnResponseBusinessRuleViolated("error.calendario.rango_invalido");
        return this.calendarioGateway.listar(desde, hasta);
    }

    @Override
    public Set<LocalDate> festivos(int anio) {
        return FestivosColombia.delAnio(anio);
    }
}
