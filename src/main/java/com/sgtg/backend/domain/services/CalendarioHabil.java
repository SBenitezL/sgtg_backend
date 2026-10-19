package com.sgtg.backend.domain.services;

import java.time.LocalDate;

/**
 * Calendario de días hábiles de la universidad. El dominio lo usa para
 * calcular los plazos expresados en días hábiles; la implementación (con los
 * festivos de Colombia) pertenece a la infraestructura.
 */
public interface CalendarioHabil {

    /**
     * @param fecha fecha a consultar
     * @return true si la fecha es día hábil
     */
    boolean esHabil(LocalDate fecha);

    /**
     * Calcula la fecha que resulta de sumar días hábiles a una fecha.
     *
     * @param desde fecha desde la que se cuenta, no se incluye
     * @param dias  cantidad de días hábiles a sumar
     * @return fecha del último día hábil del plazo
     */
    default LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate fecha = desde;
        int contados = 0;
        while (contados < dias) {
            fecha = fecha.plusDays(1);
            if (esHabil(fecha))
                contados++;
        }
        return fecha;
    }

    /**
     * Cuenta los días hábiles que faltan para una fecha límite: los posteriores
     * a {@code desde} hasta {@code hasta} inclusive. Si la fecha límite ya pasó,
     * devuelve en negativo los días hábiles transcurridos después de ella hasta
     * {@code desde} inclusive.
     *
     * @param desde fecha desde la que se cuenta (normalmente hoy), no se incluye
     * @param hasta fecha límite
     * @return días hábiles restantes; 0 si es el mismo día; negativo si ya venció
     */
    default int contarDiasHabiles(LocalDate desde, LocalDate hasta) {
        boolean vencido = hasta.isBefore(desde);
        LocalDate fecha = vencido ? hasta : desde;
        LocalDate fin = vencido ? desde : hasta;
        int contados = 0;
        while (fecha.isBefore(fin)) {
            fecha = fecha.plusDays(1);
            if (esHabil(fecha))
                contados++;
        }
        return vencido ? -contados : contados;
    }
}
