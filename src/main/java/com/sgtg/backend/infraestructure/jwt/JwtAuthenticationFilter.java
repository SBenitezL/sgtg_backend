package com.sgtg.backend.infraestructure.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sgtg.backend.infraestructure.input.auditoria.AuditoriaFilter;
import com.sgtg.backend.infraestructure.output.auth.JwtService;
import com.sgtg.backend.infraestructure.output.percistence.entities.CredencialEntity;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;
import com.sgtg.backend.infraestructure.output.percistence.repositories.CredencialRepository;
import com.sgtg.backend.infraestructure.output.percistence.repositories.UserRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Autentica cada petición a partir del token JWT del encabezado Authorization.
 * Si el token es válido, registra al usuario en el contexto de seguridad; si
 * no hay token o es inválido, la petición continúa sin autenticar y Spring
 * Security responde 401 en las rutas protegidas.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Permiso adicional del usuario que debe cambiar la contraseña que le asignó
     * un administrador: con él solo puede consultar sus datos, cambiarla y cerrar
     * sesión (ver SecurityConfig).
     */
    public static final String CAMBIO_PASSWORD_PENDIENTE = "CAMBIO_PASSWORD_PENDIENTE";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CredencialRepository credencialRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String token = tokenDe(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null)
            autenticar(token, request);
        filterChain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        try {
            String email = jwtService.getUsernameFromToken(token);
            String sesion = jwtService.getSesion(token);
            // La sesión queda en la auditoría aunque esté cerrada o el usuario inactivo: así se ve
            // si alguien intenta usar un token que ya no debería servir
            if (sesion != null)
                request.setAttribute(AuditoriaFilter.ATRIBUTO_SESION, sesion);
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
            CredencialEntity credencial = credencialRepository.findById(user.getCusuari()).orElse(null);
            // Un usuario inactivado, una sesión cerrada o un token emitido antes del último cambio de
            // contraseña dejan de servir
            if (!user.isEnabled() || credencial == null || !jwtService.isTokenValid(token, user)
                    || jwtService.isSesionCerrada(sesion)
                    || jwtService.getVersionCredencial(token) != credencial.getVersion())
                return;
            List<GrantedAuthority> permisos = new ArrayList<>(user.getAuthorities());
            if (credencial.isDebeCambiar())
                permisos.add(new SimpleGrantedAuthority(CAMBIO_PASSWORD_PENDIENTE));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                    permisos);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Token inválido, expirado o de un usuario que ya no existe: se continúa sin autenticar
            SecurityContextHolder.clearContext();
        }
    }

    /** @return el token del encabezado {@code Authorization: Bearer ...}, o null si no lo trae */
    public static String tokenDe(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
