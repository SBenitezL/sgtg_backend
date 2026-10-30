package com.sgtg.backend.domain.models.acceso;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Inicios de sesión fallidos con un correo, para frenar la adivinación de
 * contraseñas: tras {@value #MAXIMO_FALLOS} fallos seguidos (sin pasar más de
 * {@link #VENTANA} entre uno y otro) el correo queda bloqueado durante
 * {@link #BLOQUEO}, aunque después se escriba la contraseña correcta. Se lleva
 * por correo exista o no la cuenta, para no revelar cuáles están registradas.
 */
public class IntentosAcceso {

    /** Fallos seguidos con los que se bloquea el correo */
    public static final int MAXIMO_FALLOS = 5;
    /** Tiempo máximo entre un fallo y el siguiente para que se sigan contando */
    public static final Duration VENTANA = Duration.ofMinutes(15);
    /** Tiempo que dura el bloqueo */
    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    /** Correo con el que se intentó iniciar sesión, en minúsculas */
    private final String correo;
    /** Fallos seguidos desde el último bloqueo o inicio de sesión exitoso */
    private int fallos;
    private LocalDateTime ultimoFallo;
    /** Hasta cuándo está bloqueado el correo, null si nunca se bloqueó */
    private LocalDateTime bloqueadoHasta;

    public IntentosAcceso(String correo, int fallos, LocalDateTime ultimoFallo, LocalDateTime bloqueadoHasta) {
        this.correo = correo;
        this.fallos = fallos;
        this.ultimoFallo = ultimoFallo;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /** Un correo sin fallos registrados. */
    public static IntentosAcceso sinFallos(String correo) {
        return new IntentosAcceso(correo, 0, null, null);
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /**
     * Registra un inicio de sesión fallido. Si el anterior fue hace más de la
     * ventana se empieza a contar de nuevo; al llegar al máximo se bloquea el
     * correo y la cuenta vuelve a cero.
     */
    public void registrarFallo(LocalDateTime ahora) {
        boolean ventanaVencida = ultimoFallo == null || ahora.isAfter(ultimoFallo.plus(VENTANA));
        fallos = ventanaVencida ? 1 : fallos + 1;
        ultimoFallo = ahora;
        if (fallos >= MAXIMO_FALLOS) {
            bloqueadoHasta = ahora.plus(BLOQUEO);
            fallos = 0;
        }
    }

    /** @return true si hay algo registrado (fallos o un bloqueo) */
    public boolean tieneRegistro() {
        return fallos > 0 || bloqueadoHasta != null;
    }

    public String getCorreo() {
        return correo;
    }

    public int getFallos() {
        return fallos;
    }

    public LocalDateTime getUltimoFallo() {
        return ultimoFallo;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
