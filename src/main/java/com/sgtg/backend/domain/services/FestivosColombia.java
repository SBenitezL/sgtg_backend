package com.sgtg.backend.domain.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calcula los días festivos de Colombia (Ley 51 de 1983, "Ley Emiliani"):
 * <ul>
 * <li>Fijos: 1 ene, 1 may, 20 jul, 7 ago, 8 dic, 25 dic.</li>
 * <li>Trasladables al lunes siguiente: 6 ene, 19 mar, 29 jun, 15 ago, 12 oct,
 * 1 nov, 11 nov.</li>
 * <li>Según la Pascua: Jueves y Viernes Santo, y Ascensión, Corpus Christi y
 * Sagrado Corazón trasladados a lunes.</li>
 * </ul>
 */
public final class FestivosColombia {

    private static final List<MonthDay> FIJOS = List.of(MonthDay.of(1, 1), MonthDay.of(5, 1), MonthDay.of(7, 20),
            MonthDay.of(8, 7), MonthDay.of(12, 8), MonthDay.of(12, 25));

    private static final List<MonthDay> TRASLADABLES = List.of(MonthDay.of(1, 6), MonthDay.of(3, 19),
            MonthDay.of(6, 29), MonthDay.of(8, 15), MonthDay.of(10, 12), MonthDay.of(11, 1), MonthDay.of(11, 11));

    /** Días después del domingo de Pascua (ya trasladados a lunes cuando aplica) */
    private static final List<Integer> DESDE_PASCUA = List.of(-3, -2, 43, 64, 71);

    private FestivosColombia() {
    }

    /**
     * @param anio año a consultar
     * @return los festivos del año
     */
    public static Set<LocalDate> delAnio(int anio) {
        Set<LocalDate> festivos = new HashSet<>();
        FIJOS.forEach(dia -> festivos.add(dia.atYear(anio)));
        TRASLADABLES.forEach(dia -> festivos.add(siguienteLunes(dia.atYear(anio))));
        LocalDate pascua = domingoDePascua(anio);
        DESDE_PASCUA.forEach(dias -> festivos.add(pascua.plusDays(dias)));
        return festivos;
    }

    /**
     * @return true si la fecha es festivo en Colombia
     */
    public static boolean esFestivo(LocalDate fecha) {
        return delAnio(fecha.getYear()).contains(fecha);
    }

    private static LocalDate siguienteLunes(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    /** Algoritmo de Meeus/Jones/Butcher para el calendario gregoriano. */
    static LocalDate domingoDePascua(int anio) {
        int a = anio % 19;
        int b = anio / 100;
        int c = anio % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }
}
