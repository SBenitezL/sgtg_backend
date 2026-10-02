package com.sgtg.backend.infraestructure.output.calendario;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.FestivosColombia;
import com.sgtg.backend.infraestructure.output.percistence.entities.DiaNoHabilEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.DiaNoHabilRepository;

import lombok.RequiredArgsConstructor;

/**
 * Calendario de días hábiles de la universidad: excluye fines de semana,
 * festivos de ley ({@link FestivosColombia}) y los días no hábiles
 * institucionales registrados por un administrador.
 */
@Service
@RequiredArgsConstructor
public class CalendarioHabilImplAdapter implements CalendarioHabil {

    private final DiaNoHabilRepository diaNoHabilRepository;

    @Override
    public LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        // Se consultan los días institucionales por bloques para no ir a la base de datos por cada día
        LocalDate finBloque = desde.plusDays(dias * 2L + 30);
        Set<LocalDate> noHabiles = cargarNoHabiles(desde, finBloque);
        LocalDate fecha = desde;
        int contados = 0;
        while (contados < dias) {
            fecha = fecha.plusDays(1);
            if (fecha.isAfter(finBloque)) {
                LocalDate inicioBloque = finBloque.plusDays(1);
                finBloque = inicioBloque.plusDays(dias * 2L + 30);
                noHabiles.addAll(cargarNoHabiles(inicioBloque, finBloque));
            }
            if (esHabil(fecha, noHabiles))
                contados++;
        }
        return fecha;
    }

    private Set<LocalDate> cargarNoHabiles(LocalDate desde, LocalDate hasta) {
        Set<LocalDate> fechas = new HashSet<>();
        for (int anio = desde.getYear(); anio <= hasta.getYear(); anio++)
            fechas.addAll(FestivosColombia.delAnio(anio));
        diaNoHabilRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta).stream()
                .map(DiaNoHabilEntity::getFecha)
                .forEach(fechas::add);
        return fechas;
    }

    private static boolean esHabil(LocalDate fecha, Set<LocalDate> noHabiles) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY && !noHabiles.contains(fecha);
    }
}
