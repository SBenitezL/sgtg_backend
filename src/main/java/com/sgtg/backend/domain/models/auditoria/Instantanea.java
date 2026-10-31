package com.sgtg.backend.domain.models.auditoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Datos de un usuario o un trabajo de grado en un momento (nombre del dato →
 * valor), para registrar en la auditoría qué cambió una operación.
 *
 * @param datos datos en el orden en que se muestran; un valor null es un dato vacío
 */
public record Instantanea(Map<String, String> datos) {

    /** Valor con que se muestra un dato vacío */
    public static final String VACIO = "—";

    public Instantanea {
        datos = new LinkedHashMap<>(datos);
    }

    /**
     * Cambios de esta instantánea a la siguiente, uno por dato que cambió, en la
     * forma {@code dato: antes → después}.
     */
    public List<String> cambiosHasta(Instantanea despues) {
        Set<String> nombres = new LinkedHashSet<>(datos.keySet());
        nombres.addAll(despues.datos().keySet());
        List<String> cambios = new ArrayList<>();
        for (String nombre : nombres) {
            String antes = datos.get(nombre);
            String ahora = despues.datos().get(nombre);
            if (!Objects.equals(antes, ahora))
                cambios.add(nombre + ": " + mostrar(antes) + " → " + mostrar(ahora));
        }
        return cambios;
    }

    private static String mostrar(String valor) {
        return valor == null || valor.isBlank() ? VACIO : valor;
    }
}
