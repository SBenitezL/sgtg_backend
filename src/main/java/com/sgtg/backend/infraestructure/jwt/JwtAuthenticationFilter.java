package com.sgtg.backend.infraestructure.jwt;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sgtg.backend.infraestructure.input.auditoria.AuditoriaFilter;
import com.sgtg.backend.infraestructure.output.auth.JwtService;

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

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

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
            UserDetails user = userDetailsService.loadUserByUsername(email);
            // Un usuario inactivado o una sesión cerrada dejan de poder usar el token
            if (!user.isEnabled() || !jwtService.isTokenValid(token, user) || jwtService.isSesionCerrada(sesion))
                return;
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                    user.getAuthorities());
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
