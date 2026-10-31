package com.sgtg.backend.infraestructure.output.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.application.output.ManageAlertaGatewayIntPort;
import com.sgtg.backend.domain.models.acceso.SesionUsuario;
import com.sgtg.backend.domain.models.acceso.SesionUsuario.Renovacion;
import com.sgtg.backend.domain.models.auditoria.AlertaSeguridad;
import com.sgtg.backend.domain.models.auditoria.TipoAlerta;
import com.sgtg.backend.infraestructure.output.percistence.entities.CredencialEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.SesionEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.CredencialRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.SesionRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sesiones de los usuarios ({@link SesionUsuario}). El token de renovación es
 * {@code <sesión>.<secreto aleatorio de 256 bits>}; solo se guarda la huella
 * SHA-256 del secreto. Usa la hora real, no el reloj ajustable de desarrollo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SesionService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final int BYTES_SECRETO = 32;

    private final SesionRepository sesionRepository;
    private final UserRepository userRepository;
    private final CredencialRepository credencialRepository;
    private final ManageAlertaGatewayIntPort alertas;
    private final SecureRandom aleatorio = new SecureRandom();

    /** Sesión recién iniciada o renovada, con su token de renovación */
    public record Abierta(SesionUsuario sesion, String tokenRenovacion) {
    }

    /** Renovación exitosa: la sesión con su token nuevo y los datos para el token de acceso */
    public record Renovada(Abierta abierta, UserEntity usuario, CredencialEntity credencial) {
    }

    @Transactional
    public Abierta iniciar(Long usuarioId, int versionCredencial) {
        String secreto = secreto();
        SesionUsuario sesion = SesionUsuario.nueva(UUID.randomUUID().toString(), usuarioId, versionCredencial,
                huella(secreto), ahora());
        sesionRepository.save(SesionEntity.desde(sesion));
        return new Abierta(sesion, sesion.getId() + "." + secreto);
    }

    /** @return true si la sesión existe y sigue abierta */
    @Transactional(readOnly = true)
    public boolean estaActiva(String sesion) {
        return sesion != null && sesionRepository.findById(sesion)
                .map(entidad -> entidad.aDominio().estaActiva(ahora())).orElse(false);
    }

    /**
     * Renueva la sesión del token y lo rota. Si el token ya se había usado,
     * revoca la sesión y avisa a los administradores.
     *
     * @return vacío si el token no sirve (sesión cerrada, vencida, reutilizada,
     *         usuario inactivo o credenciales cambiadas)
     */
    @Transactional
    public Optional<Renovada> renovar(String tokenRenovacion) {
        String[] partes = partes(tokenRenovacion);
        if (partes == null)
            return Optional.empty();
        SesionEntity entidad = sesionRepository.bloquear(partes[0]).orElse(null);
        if (entidad == null)
            return Optional.empty();
        SesionUsuario sesion = entidad.aDominio();
        UserEntity usuario = userRepository.findById(sesion.getUsuarioId()).orElse(null);
        CredencialEntity credencial = credencialRepository.findById(sesion.getUsuarioId()).orElse(null);
        // Un usuario inactivo o sin credenciales no puede seguir: ninguna versión coincide
        int version = usuario != null && usuario.isEnabled() && credencial != null ? credencial.getVersion() : -1;
        String secretoNuevo = secreto();
        LocalDateTime ahora = ahora();
        Renovacion resultado = sesion.renovar(huella(partes[1]), version, huella(secretoNuevo), ahora);
        sesionRepository.save(SesionEntity.desde(sesion));
        if (resultado == Renovacion.REUTILIZADA) {
            log.warn("Se reutilizó un token de renovación de la sesión {}: queda cerrada", sesion.getId());
            alertas.alertar(new AlertaSeguridad(TipoAlerta.TOKEN_RENOVACION_REUTILIZADO, sesion.getId(),
                    "Se presentó un token de renovación ya usado de la sesión " + sesion.getId() + " (usuario "
                            + sesion.getUsuarioId() + "). Alguien más pudo copiarlo: la sesión quedó cerrada.",
                    ahora));
        }
        return resultado == Renovacion.RENOVADA
                ? Optional.of(new Renovada(new Abierta(sesion, sesion.getId() + "." + secretoNuevo), usuario,
                        credencial))
                : Optional.empty();
    }

    /** Cierra la sesión indicada. */
    @Transactional
    public void revocar(String sesion) {
        if (sesion == null)
            return;
        sesionRepository.bloquear(sesion).ifPresent(entidad -> {
            SesionUsuario dominio = entidad.aDominio();
            dominio.revocar(ahora());
            sesionRepository.save(SesionEntity.desde(dominio));
        });
    }

    /**
     * Cierra la sesión de un token de renovación, si es el vigente o el anterior
     * de su sesión: sin el secreto nadie puede cerrar la sesión de otro.
     */
    @Transactional
    public void revocarConToken(String tokenRenovacion) {
        String[] partes = partes(tokenRenovacion);
        if (partes == null)
            return;
        String huella = huella(partes[1]);
        sesionRepository.bloquear(partes[0])
                .filter(entidad -> huella.equals(entidad.getHuellaActual()) || huella.equals(entidad.getHuellaAnterior()))
                .ifPresent(entidad -> revocar(entidad.getId()));
    }

    /** Cierra las sesiones abiertas del usuario, salvo {@code excepto} (null: todas). */
    @Transactional
    public int revocarDeUsuario(Long usuarioId, String excepto) {
        return sesionRepository.revocarDeUsuario(usuarioId, excepto, ahora());
    }

    /** La sesión sigue abierta con la versión nueva de las credenciales (el usuario cambió su contraseña en ella). */
    @Transactional
    public void actualizarVersion(String sesion, int versionCredencial) {
        sesionRepository.bloquear(sesion).ifPresent(entidad -> {
            SesionUsuario dominio = entidad.aDominio();
            dominio.actualizarVersion(versionCredencial);
            sesionRepository.save(SesionEntity.desde(dominio));
        });
    }

    /** Borra las sesiones que ya no pueden usarse desde hace más de un día. */
    @Transactional
    public int depurar() {
        LocalDateTime haceUnDia = ahora().minusDays(1);
        return sesionRepository.depurar(haceUnDia.minus(SesionUsuario.DURACION_MAXIMA),
                haceUnDia.minus(SesionUsuario.INACTIVIDAD));
    }

    /** Sesión de un token de renovación (sin validarlo), o null si no tiene el formato */
    public static String sesionDe(String tokenRenovacion) {
        String[] partes = partes(tokenRenovacion);
        return partes == null ? null : partes[0];
    }

    private static String[] partes(String tokenRenovacion) {
        if (tokenRenovacion == null)
            return null;
        String[] partes = tokenRenovacion.split("\\.", -1);
        return partes.length == 2 && partes[0].length() == 36 && !partes[1].isEmpty() ? partes : null;
    }

    private String secreto() {
        byte[] bytes = new byte[BYTES_SECRETO];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String huella(String secreto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(secreto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }
}
