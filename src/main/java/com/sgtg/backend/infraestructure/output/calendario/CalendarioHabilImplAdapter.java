package com.sgtg.backend.infraestructure.output.calendario;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sgtg.backend.domain.services.CalendarioHabil;
import com.sgtg.backend.domain.services.FestivosColombia;
import com.sgtg.backend.infraestructure.output.percistence.entities.DiaNoHabilEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.DiaNoHabilRepository;

import lombok.RequiredArgsConstructor;

/**
 * Calendario de días hábiles de la universidad: excluye fines de semana,
 * festivos de ley ({@link FestivosColombia}) y los días no hábiles
 * institucionales registrados por un administrador.
 * <p>
 * Los plazos se calculan en cada respuesta de un trabajo (días restantes,
 * conceptos a tiempo), así que el calendario se mantiene en memoria: los
 * festivos por año y los días institucionales, que se recargan cuando un
 * administrador registra o elimina uno ({@link #invalidar()}) y, como
 * respaldo, cada {@link #VIGENCIA_DIAS_INSTITUCIONALES}.
 */
@Service
@RequiredArgsConstructor
public class CalendarioHabilImplAdapter implements CalendarioHabil {

    /** Tiempo máximo que se conservan en memoria los días institucionales. */
    static final Duration VIGENCIA_DIAS_INSTITUCIONALES = Duration.ofMinutes(10);

    private final DiaNoHabilRepository diaNoHabilRepository;

    private final Map<Integer, Set<LocalDate>> festivosPorAnio = new ConcurrentHashMap<>();
    private volatile DiasInstitucionales diasInstitucionales;

    private record DiasInstitucionales(Set<LocalDate> fechas, long cargadosEn) {
    }

    @Override
    public boolean esHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY
                && !festivosPorAnio.computeIfAbsent(fecha.getYear(), FestivosColombia::delAnio).contains(fecha)
                && !diasInstitucionales().contains(fecha);
    }

    /**
     * Descarta los días institucionales en memoria para que se recarguen en la
     * próxima consulta. Dentro de una transacción espera a que se confirme,
     * así ninguna consulta concurrente vuelve a cargar los datos anteriores.
     */
    public void invalidar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    diasInstitucionales = null;
                }
            });
        } else {
            diasInstitucionales = null;
        }
    }

    private Set<LocalDate> diasInstitucionales() {
        DiasInstitucionales actuales = diasInstitucionales;
        long ahora = System.nanoTime();
        if (actuales == null || ahora - actuales.cargadosEn() > VIGENCIA_DIAS_INSTITUCIONALES.toNanos()) {
            Set<LocalDate> fechas = diaNoHabilRepository.findAll().stream()
                    .map(DiaNoHabilEntity::getFecha)
                    .collect(Collectors.toUnmodifiableSet());
            actuales = new DiasInstitucionales(fechas, ahora);
            diasInstitucionales = actuales;
        }
        return actuales.fechas();
    }
}
