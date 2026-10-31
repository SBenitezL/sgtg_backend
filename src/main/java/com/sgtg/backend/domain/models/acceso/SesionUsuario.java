package com.sgtg.backend.domain.models.acceso;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Sesión de un usuario, desde que inicia sesión hasta que la cierra o vence.
 * Los tokens de acceso duran poco; la sesión se mantiene con un token de
 * renovación que cambia en cada uso (rotación). Del token de renovación solo se
 * guarda su huella.
 * <p>
 * La sesión vence si pasa {@link #INACTIVIDAD} sin renovarse o al cumplir
 * {@link #DURACION_MAXIMA}. Si se presenta un token de renovación ya usado
 * (fuera de {@link #GRACIA}), alguien más lo tiene: la sesión se revoca.
 */
public class SesionUsuario {

    /** Tiempo máximo sin renovar la sesión (el mismo del cierre por inactividad del front) */
    public static final Duration INACTIVIDAD = Duration.ofMinutes(30);
    /** Duración máxima de una sesión: después hay que volver a iniciar sesión */
    public static final Duration DURACION_MAXIMA = Duration.ofHours(12);
    /**
     * Tiempo en que el token de renovación anterior todavía se acepta: dos
     * pestañas que renuevan a la vez envían el mismo token
     */
    public static final Duration GRACIA = Duration.ofSeconds(30);

    /** Resultado de presentar un token de renovación */
    public enum Renovacion {
        /** Se renovó: hay un token de renovación nuevo */
        RENOVADA,
        /** La sesión fue cerrada, venció o es de otra versión de las credenciales */
        VENCIDA,
        /** Se presentó un token ya usado: posible robo, la sesión queda revocada */
        REUTILIZADA
    }

    private final String id;
    private final Long usuarioId;
    private int versionCredencial;
    private final LocalDateTime creada;
    private LocalDateTime ultimoUso;
    /** Huella del token de renovación vigente */
    private String huellaActual;
    /** Huella del token anterior y cuándo se reemplazó, para la gracia */
    private String huellaAnterior;
    private LocalDateTime rotada;
    private LocalDateTime revocada;

    public SesionUsuario(String id, Long usuarioId, int versionCredencial, LocalDateTime creada,
            LocalDateTime ultimoUso, String huellaActual, String huellaAnterior, LocalDateTime rotada,
            LocalDateTime revocada) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.versionCredencial = versionCredencial;
        this.creada = creada;
        this.ultimoUso = ultimoUso;
        this.huellaActual = huellaActual;
        this.huellaAnterior = huellaAnterior;
        this.rotada = rotada;
        this.revocada = revocada;
    }

    /** Sesión recién iniciada. */
    public static SesionUsuario nueva(String id, Long usuarioId, int versionCredencial, String huella,
            LocalDateTime ahora) {
        return new SesionUsuario(id, usuarioId, versionCredencial, ahora, ahora, huella, null, null, null);
    }

    /** @return true si la sesión sigue abierta: no se cerró ni venció */
    public boolean estaActiva(LocalDateTime ahora) {
        return revocada == null && ahora.isBefore(expira()) && ahora.isBefore(ultimoUso.plus(INACTIVIDAD));
    }

    /** Cuándo vence la sesión por su duración máxima */
    public LocalDateTime expira() {
        return creada.plus(DURACION_MAXIMA);
    }

    /**
     * Presenta un token de renovación. Si es el vigente (o el anterior dentro de
     * la gracia) y la sesión sigue activa con la misma versión de credenciales,
     * se rota al token nuevo.
     *
     * @param huella            huella del token presentado
     * @param versionCredencial versión actual de las credenciales del usuario
     * @param huellaNueva       huella del token que reemplaza al presentado
     */
    public Renovacion renovar(String huella, int versionCredencial, String huellaNueva, LocalDateTime ahora) {
        boolean vigente = Objects.equals(huella, huellaActual);
        boolean anteriorEnGracia = huella != null && huella.equals(huellaAnterior) && rotada != null
                && !ahora.isAfter(rotada.plus(GRACIA));
        if (!vigente && !anteriorEnGracia) {
            // Un token que ya se usó (o uno que nunca fue de esta sesión)
            revocar(ahora);
            return Renovacion.REUTILIZADA;
        }
        if (!estaActiva(ahora) || versionCredencial != this.versionCredencial) {
            revocar(ahora);
            return Renovacion.VENCIDA;
        }
        huellaAnterior = huellaActual;
        huellaActual = huellaNueva;
        rotada = ahora;
        ultimoUso = ahora;
        return Renovacion.RENOVADA;
    }

    /** Cierra la sesión: ni sus tokens de acceso ni el de renovación vuelven a servir. */
    public void revocar(LocalDateTime ahora) {
        if (revocada == null)
            revocada = ahora;
    }

    /** El usuario cambió su contraseña desde esta sesión: sigue abierta con la versión nueva. */
    public void actualizarVersion(int versionCredencial) {
        this.versionCredencial = versionCredencial;
    }

    public String getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public int getVersionCredencial() {
        return versionCredencial;
    }

    public LocalDateTime getCreada() {
        return creada;
    }

    public LocalDateTime getUltimoUso() {
        return ultimoUso;
    }

    public String getHuellaActual() {
        return huellaActual;
    }

    public String getHuellaAnterior() {
        return huellaAnterior;
    }

    public LocalDateTime getRotada() {
        return rotada;
    }

    public LocalDateTime getRevocada() {
        return revocada;
    }
}
