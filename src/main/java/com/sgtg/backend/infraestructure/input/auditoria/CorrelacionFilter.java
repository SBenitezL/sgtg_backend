package com.sgtg.backend.infraestructure.input.auditoria;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Identificador de cada petición ({@code X-Request-Id}): lo genera Nginx (o
 * este filtro si la petición llega sin él), va en la respuesta, en cada línea
 * del log del backend ({@code [solicitud]}) y en la auditoría. Con él se cruza
 * un registro de la auditoría con el log de Nginx y con el error exacto en el
 * log del backend. Va antes que la seguridad, para que también lo tengan las
 * peticiones rechazadas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelacionFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Request-Id";
    /** Atributo de la petición con el identificador, para la auditoría */
    public static final String ATRIBUTO = "sgtg.auditoria.solicitud";
    /** Clave en el contexto del log (MDC) */
    public static final String CLAVE_LOG = "solicitud";
    /** Solo letras, dígitos y guiones: lo que llega en la cabecera no puede ensuciar el log */
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String recibido = request.getHeader(CABECERA);
        String solicitud = recibido != null && VALIDO.matcher(recibido).matches() ? recibido
                : UUID.randomUUID().toString();
        request.setAttribute(ATRIBUTO, solicitud);
        response.setHeader(CABECERA, solicitud);
        MDC.put(CLAVE_LOG, solicitud);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_LOG);
        }
    }
}
