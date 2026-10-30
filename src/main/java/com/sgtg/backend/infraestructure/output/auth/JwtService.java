package com.sgtg.backend.infraestructure.output.auth;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
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
import org.springframework.transaction.annotation.Transactional;

import com.sgtg.backend.infraestructure.output.percistence.entities.SesionCerradaEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.SesionCerradaRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Emite y valida los tokens de sesión. Cada token lleva un identificador de
 * sesión único (jti), que queda en la auditoría y permite cerrar esa sesión
 * antes de que el token venza.
 */
@Service
@RequiredArgsConstructor
public class JwtService {
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final SesionCerradaRepository sesionCerradaRepository;

    @Value("${jwt.secret}")
    private String SECRET_KEY;

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

    public String getToken(UserDetails user) {
        Map<String, Collection<?>> extraClaims = new HashMap<>();
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        extraClaims.put("roles", roles);
        return getToken(extraClaims, user);
    }

    private String getToken(Map<String, Collection<?>> extraClaims, UserDetails user) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setId(UUID.randomUUID().toString())
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
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
     * Identificador de la sesión del token (jti); null en los tokens emitidos
     * antes de que existiera. Lanza una {@link JwtException} si el token es
     * inválido o expiró.
     */
    public String getSesion(String token) {
        return getClaims(token).getId();
    }

    /**
     * @return true si el token pertenece al usuario y no ha expirado
     */
    public boolean isTokenValid(String token, UserDetails user) {
        Claims claims = getClaims(token);
        return user.getUsername().equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    /** @return true si la sesión fue cerrada por su usuario */
    public boolean isSesionCerrada(String sesion) {
        return sesion != null && sesionCerradaRepository.existsById(sesion);
    }

    /**
     * Cierra la sesión del token: deja de ser aceptado aunque no haya vencido.
     * Un token inválido, vencido o sin identificador de sesión no se registra.
     */
    public void cerrarSesion(String token) {
        try {
            Claims claims = getClaims(token);
            if (claims.getId() != null)
                sesionCerradaRepository.save(new SesionCerradaEntity(claims.getId(),
                        LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZONA)));
        } catch (JwtException | IllegalArgumentException e) {
            // Ya no sirve: no hay nada que cerrar
        }
    }

    /**
     * Borra las sesiones cerradas cuyos tokens ya vencieron (no hace falta
     * recordarlas). Usa la hora real, como el vencimiento de los tokens.
     *
     * @return cantidad de sesiones borradas
     */
    @Transactional
    public int depurarSesionesCerradas() {
        return sesionCerradaRepository.depurar(LocalDateTime.now(ZONA));
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
