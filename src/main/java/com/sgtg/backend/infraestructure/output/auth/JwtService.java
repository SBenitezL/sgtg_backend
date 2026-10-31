package com.sgtg.backend.infraestructure.output.auth;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Emite y valida los tokens de acceso. Duran poco ({@code sgtg.sesion.minutos-token},
 * 15 minutos por defecto): la sesión se mantiene renovándolos con el token de
 * renovación (ver {@link SesionService}). Cada token lleva:
 * <ul>
 * <li>la sesión a la que pertenece ({@value #CLAIM_SESION}), la misma en todos
 * los tokens de un inicio de sesión: queda en la auditoría y, si la sesión se
 * cierra, sus tokens dejan de servir;</li>
 * <li>la versión de las credenciales del usuario ({@value #CLAIM_VERSION}): al
 * cambiar la contraseña la versión aumenta y los tokens anteriores dejan de
 * servir;</li>
 * <li>{@value #CLAIM_CAMBIO_PASSWORD} cuando el usuario debe cambiar la contraseña
 * que le asignó un administrador, para que el front lo lleve a cambiarla.</li>
 * </ul>
 */
@Service
public class JwtService {
    static final String CLAIM_SESION = "sid";
    static final String CLAIM_VERSION = "ver";
    static final String CLAIM_CAMBIO_PASSWORD = "cambioPassword";

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${sgtg.sesion.minutos-token:15}")
    private long minutosToken = 15;

    /**
     * Una clave vacía o débil falla al arrancar con un mensaje claro, en lugar
     * de fallar en el primer inicio de sesión.
     */
    @PostConstruct
    void validarClave() {
        try {
            getKey();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "jwt.secret (variable JWT_SECRET) debe ser una clave en Base64 de al menos 32 bytes", e);
        }
    }

    /**
     * @param versionCredencial   versión actual de las credenciales del usuario
     * @param debeCambiarPassword true si debe cambiar la contraseña antes de usar el sistema
     * @param sesion              sesión a la que pertenece el token
     */
    public String getToken(UserDetails user, int versionCredencial, boolean debeCambiarPassword, String sesion) {
        Map<String, Object> extraClaims = new HashMap<>();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        extraClaims.put("roles", roles);
        extraClaims.put(CLAIM_SESION, sesion);
        extraClaims.put(CLAIM_VERSION, versionCredencial);
        if (debeCambiarPassword)
            extraClaims.put(CLAIM_CAMBIO_PASSWORD, true);
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(extraClaims)
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(ahora))
                .setExpiration(new Date(ahora + Duration.ofMinutes(minutosToken).toMillis()))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Obtiene el usuario (correo) al que pertenece el token. Lanza una
     * {@link io.jsonwebtoken.JwtException} si el token es inválido o expiró.
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Sesión a la que pertenece el token; null en los tokens emitidos antes de
     * que existieran las sesiones (ya no se aceptan). Lanza una
     * {@link io.jsonwebtoken.JwtException} si el token es inválido o expiró.
     */
    public String getSesion(String token) {
        return getClaims(token).get(CLAIM_SESION, String.class);
    }

    /**
     * Versión de las credenciales con la que se emitió el token; 0 si no la
     * trae. Lanza una {@link io.jsonwebtoken.JwtException} si el token es
     * inválido o expiró.
     */
    public int getVersionCredencial(String token) {
        Integer version = getClaims(token).get(CLAIM_VERSION, Integer.class);
        return version == null ? 0 : version;
    }

    /**
     * @return true si el token pertenece al usuario y no ha expirado
     */
    public boolean isTokenValid(String token, UserDetails user) {
        Claims claims = getClaims(token);
        return user.getUsername().equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
