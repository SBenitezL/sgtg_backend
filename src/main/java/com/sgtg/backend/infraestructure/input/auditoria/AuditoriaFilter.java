package com.sgtg.backend.infraestructure.input.auditoria;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgtg.backend.application.output.ManageAuditoriaGatewayIntPort;
import com.sgtg.backend.application.output.ManageInstantaneaGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.Instantanea;
import com.sgtg.backend.domain.models.auditoria.RegistroAuditoria;
import com.sgtg.backend.domain.models.auditoria.ResultadoAuditoria;
import com.sgtg.backend.infraestructure.output.percistence.entities.UserEntity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Auditoría de la API: registra cada operación que modifica datos (POST, PUT,
 * PATCH, DELETE), las descargas de documentos y los inicios de sesión
 * (exitosos o no, con el correo intentado y nunca la contraseña), con el
 * usuario, su sesión, la IP, el navegador, la operación y su resultado. Va después del
 * filtro JWT para conocer al usuario y registra al terminar la petición, con
 * el código de respuesta. Si no se puede registrar, la petición no falla.
 * <p>
 * En las operaciones exitosas sobre un usuario ({@code /usuarios/{id}...},
 * {@code /usuarios/me/password}) o un trabajo de grado
 * ({@code /trabajos-grado/{id}...}) guarda en el detalle qué cambió (antes →
 * después), y además lo que agregó la operación con {@link DetalleAuditoria}
 * (ej. los archivos con su huella SHA-256).
 * <p>
 * La IP es la del cliente cuando el backend está detrás de Nginx gracias a
 * {@code server.forward-headers-strategy}; quien llega directo al puerto del
 * backend podría falsearla, por eso en producción ese puerto no se expone.
 */
@Slf4j
public class AuditoriaFilter extends OncePerRequestFilter {

    private static final Set<String> METODOS_QUE_MODIFICAN = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String API = "/api/v1/sgtg/";
    private static final String LOGIN = API + "auth/login";
    /**
     * Atributo de la petición con el identificador de la sesión (jti del token):
     * lo pone el filtro JWT o, en un inicio de sesión exitoso, el login con la
     * sesión nueva. Así todas las acciones se pueden unir a su inicio de sesión.
     */
    public static final String ATRIBUTO_SESION = "sgtg.auditoria.sesion";
    private static final Pattern RUTA_USUARIO = Pattern.compile("^" + API + "usuarios/(\\d+)(/.*)?$");
    private static final String CAMBIO_PASSWORD = API + "usuarios/me/password";
    private static final Pattern RUTA_TRABAJO = Pattern.compile("^" + API + "trabajos-grado/(\\d+)(/.*)?$");
    private static final int LONGITUD_DETALLE = 4000;

    private final ManageAuditoriaGatewayIntPort auditoria;
    private final ManageInstantaneaGatewayIntPort instantaneas;
    private final ObjectMapper json;
    /** Reloj real: la auditoría no usa el reloj ajustable de desarrollo */
    private final Clock reloj;

    public AuditoriaFilter(ManageAuditoriaGatewayIntPort auditoria, ManageInstantaneaGatewayIntPort instantaneas,
            ObjectMapper json, Clock reloj) {
        this.auditoria = auditoria;
        this.instantaneas = instantaneas;
        this.json = json;
        this.reloj = reloj;
    }

    /** Usuario o trabajo de grado sobre el que opera la petición */
    private record Objetivo(String tipo, Long id) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        if (!ruta.startsWith(API))
            return true;
        boolean descarga = "GET".equals(request.getMethod()) && ruta.matches(".*/trabajos-grado/[^/]+/archivos$");
        return !METODOS_QUE_MODIFICAN.contains(request.getMethod()) && !descarga;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean login = LOGIN.equals(request.getRequestURI());
        // Solo en el inicio de sesión se guarda el cuerpo, para leer el correo intentado
        HttpServletRequest peticion = login ? new ContentCachingRequestWrapper(request, 4096) : request;
        Objetivo objetivo = objetivo(request);
        Instantanea antes = instantanea(objetivo);
        try {
            filterChain.doFilter(peticion, response);
        } finally {
            int estado = response.getStatus();
            List<String> detalle = new ArrayList<>();
            // Si la operación no se realizó, no cambió nada
            if (antes != null && ResultadoAuditoria.desde(estado) == ResultadoAuditoria.EXITOSA) {
                Instantanea despues = instantanea(objetivo);
                if (despues != null)
                    antes.cambiosHasta(despues).forEach(cambio -> detalle.add(objetivo.tipo() + " " + objetivo.id()
                            + " · " + cambio));
            }
            detalle.addAll(DetalleAuditoria.de(request));
            registrar(peticion, estado, login, detalle);
        }
    }

    private Objetivo objetivo(HttpServletRequest request) {
        if (!METODOS_QUE_MODIFICAN.contains(request.getMethod()))
            return null;
        String ruta = request.getRequestURI();
        UserEntity usuario = usuarioAutenticado();
        if (CAMBIO_PASSWORD.equals(ruta))
            return usuario == null ? null : new Objetivo("usuario", usuario.getCusuari());
        Matcher usuarioDeLaRuta = RUTA_USUARIO.matcher(ruta);
        if (usuarioDeLaRuta.matches())
            return new Objetivo("usuario", Long.valueOf(usuarioDeLaRuta.group(1)));
        Matcher trabajo = RUTA_TRABAJO.matcher(ruta);
        return trabajo.matches() ? new Objetivo("trabajo", Long.valueOf(trabajo.group(1))) : null;
    }

    /** Datos del objetivo; null si no hay objetivo, no existe o no se pudieron leer */
    private Instantanea instantanea(Objetivo objetivo) {
        if (objetivo == null)
            return null;
        try {
            return "usuario".equals(objetivo.tipo()) ? instantaneas.usuario(objetivo.id())
                    : instantaneas.trabajo(objetivo.id());
        } catch (RuntimeException e) {
            log.warn("No se pudieron leer los datos de {} {} para la auditoría", objetivo.tipo(), objetivo.id(), e);
            return null;
        }
    }

    private void registrar(HttpServletRequest request, int estado, boolean login, List<String> detalle) {
        try {
            UserEntity usuario = usuarioAutenticado();
            String correo = usuario != null ? usuario.getEmail() : login ? correoIntentado(request) : null;
            Map<String, String> variables = variablesDeRuta(request);
            // En microsegundos, la precisión con que la guarda la base: así el sello se puede recalcular
            auditoria.registrar(new RegistroAuditoria(LocalDateTime.now(reloj).truncatedTo(ChronoUnit.MICROS),
                    usuario == null ? null : usuario.getCusuari(), recortar(correo, 255),
                    usuario == null ? null : recortar(roles(usuario), 300),
                    request.getAttribute(ATRIBUTO_SESION) instanceof String sesion ? recortar(sesion, 36) : null,
                    request.getAttribute(CorrelacionFilter.ATRIBUTO) instanceof String solicitud
                            ? recortar(solicitud, 64) : null,
                    recortar(request.getRemoteAddr(), 64),
                    recortar(request.getHeader("User-Agent"), 500), request.getMethod(),
                    recortar(request.getRequestURI() + (request.getQueryString() == null ? ""
                            : "?" + request.getQueryString()), 1000),
                    recortar(operacion(request), 200), trabajoId(variables),
                    variables.isEmpty() ? null : recortar(variables.entrySet().stream()
                            .map(variable -> variable.getKey() + "=" + variable.getValue())
                            .collect(Collectors.joining(", ")), 500),
                    estado, ResultadoAuditoria.desde(estado),
                    detalle.isEmpty() ? null : recortar(String.join("\n", detalle), LONGITUD_DETALLE)));
        } catch (RuntimeException e) {
            log.error("No se pudo registrar la auditoría de {} {}", request.getMethod(), request.getRequestURI(), e);
        }
    }

    private static UserEntity usuarioAutenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof UserEntity usuario ? usuario : null;
    }

    private static String roles(UserEntity usuario) {
        return usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted()
                .collect(Collectors.joining(","));
    }

    /** Correo con el que se intentó iniciar sesión (del cuerpo ya leído); la contraseña nunca se guarda. */
    private String correoIntentado(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper cuerpo) || cuerpo.getContentAsByteArray().length == 0)
            return null;
        try {
            JsonNode email = json.readTree(cuerpo.getContentAsByteArray()).get("email");
            return email == null || email.isNull() ? null : email.asText();
        } catch (IOException e) {
            return null;
        }
    }

    /** Método del controlador que atendió la petición, ej. TrabajoGradoController.agendarReunion. */
    private static String operacion(HttpServletRequest request) {
        Object manejador = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (manejador instanceof HandlerMethod metodo)
            return metodo.getBeanType().getSimpleName() + "." + metodo.getMethod().getName();
        Object patron = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return patron == null ? null : patron.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> variablesDeRuta(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return variables instanceof Map<?, ?> mapa ? new TreeMap<>((Map<String, String>) mapa) : Map.of();
    }

    private static Long trabajoId(Map<String, String> variables) {
        try {
            return variables.containsKey("trabajoId") ? Long.valueOf(variables.get("trabajoId")) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
