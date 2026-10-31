package com.sgtg.backend.infraestructure.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/**
 * Sesión con token de acceso corto y token de renovación en una cookie
 * HttpOnly que rota en cada uso (H2, JWT real).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class SesionIntegracionTest {

    private static final String API = "/api/v1/sgtg";
    private static final String COOKIE = "sgtg_renovacion";
    private static final String CLAVE = "Clave123*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String admin;
    private String sufijo;

    /** Respuesta de un login o una renovación: el token de acceso y el de renovación de la cookie */
    private record Sesion(String acceso, String renovacion, String setCookie) {
    }

    private MvcResult enviar(MockHttpServletRequestBuilder peticion, String token, Object cuerpo) throws Exception {
        if (token != null)
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (cuerpo != null)
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        return mvc.perform(peticion).andReturn();
    }

    private static Sesion sesion(MvcResult respuesta) throws Exception {
        assertEquals(200, respuesta.getResponse().getStatus());
        return new Sesion(JsonPath.read(respuesta.getResponse().getContentAsString(), "$.token"),
                respuesta.getResponse().getCookie(COOKIE).getValue(),
                respuesta.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    private Sesion login(String email, String password) throws Exception {
        return sesion(enviar(post(API + "/auth/login"), null, Map.of("email", email, "password", password)));
    }

    private MvcResult renovar(String renovacion) throws Exception {
        MockHttpServletRequestBuilder peticion = post(API + "/auth/renovar").header("X-Requested-With",
                "XMLHttpRequest");
        if (renovacion != null)
            peticion.cookie(new Cookie(COOKIE, renovacion));
        return mvc.perform(peticion).andReturn();
    }

    private int estado(String token) throws Exception {
        return enviar(get(API + "/usuarios/me"), token, null).getResponse().getStatus();
    }

    private String correo() {
        return "ses" + sufijo + "@unicauca.edu.co";
    }

    private long crearDocente() throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", "S" + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", sufijo);
        datos.put("email", correo());
        datos.put("password", CLAVE);
        datos.put("primerNombre", "Docente");
        datos.put("primerApellido", "Prueba");
        datos.put("roles", List.of("DOCENTE"));
        return ((Number) JsonPath.read(enviar(post(API + "/usuarios"), admin, datos).getResponse()
                .getContentAsString(), "$.id")).longValue();
    }

    @BeforeEach
    void preparar() throws Exception {
        sufijo = String.valueOf(System.nanoTime());
        admin = login("admin@sgtg.test", "Admin123*").acceso();
    }

    @Test
    void elLoginDejaElTokenDeRenovacionEnUnaCookieQueElNavegadorNoDejaLeer() throws Exception {
        String cookie = login("admin@sgtg.test", "Admin123*").setCookie();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Strict"));
        // Solo viaja a las rutas de autenticación, no en las demás peticiones
        assertTrue(cookie.contains("Path=/api/v1/sgtg/auth"));
        // Vive lo que dura la sesión como máximo: 12 horas
        assertTrue(cookie.matches(".*Max-Age=(43199|43200).*"));
    }

    @Test
    void laRenovacionDaUnTokenNuevoDeLaMismaSesionYRotaLaCookie() throws Exception {
        Sesion inicial = login("admin@sgtg.test", "Admin123*");
        String sesionId = jdbc.queryForObject("SELECT sesion FROM auditoria ORDER BY id DESC LIMIT 1", String.class);

        Sesion renovada = sesion(renovar(inicial.renovacion()));

        assertNotEquals(inicial.renovacion(), renovada.renovacion());
        assertEquals(200, estado(renovada.acceso()));
        // Es la misma sesión: la auditoría une el login y las renovaciones
        assertEquals(sesionId, jdbc.queryForObject("SELECT sesion FROM auditoria ORDER BY id DESC LIMIT 1",
                String.class));
        assertEquals("AuthController.renovar",
                jdbc.queryForObject("SELECT operacion FROM auditoria ORDER BY id DESC LIMIT 1", String.class));
    }

    @Test
    void sinLaCabeceraNoSePuedeRenovar() throws Exception {
        Sesion inicial = login("admin@sgtg.test", "Admin123*");
        // Un formulario de otro sitio no puede enviar la cabecera
        assertEquals(403, mvc.perform(post(API + "/auth/renovar").cookie(new Cookie(COOKIE, inicial.renovacion())))
                .andReturn().getResponse().getStatus());
        assertEquals(401, renovar(null).getResponse().getStatus());
        assertEquals(401, renovar("no-es-un-token").getResponse().getStatus());
    }

    @Test
    void reutilizarUnTokenDeRenovacionYaUsadoCierraLaSesionYSeAvisa() throws Exception {
        Sesion inicial = login("admin@sgtg.test", "Admin123*");
        String sesionId = inicial.renovacion().substring(0, 36);
        Sesion segunda = sesion(renovar(inicial.renovacion()));
        Sesion tercera = sesion(renovar(segunda.renovacion()));

        // El primero ya se usó dos veces atrás: alguien lo copió
        assertEquals(401, renovar(inicial.renovacion()).getResponse().getStatus());

        // La sesión queda cerrada para todos, también para el dueño
        assertEquals(401, renovar(tercera.renovacion()).getResponse().getStatus());
        assertEquals(401, estado(tercera.acceso()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM alertas_seguridad"
                + " WHERE tipo = 'TOKEN_RENOVACION_REUTILIZADO' AND clave = ?", Long.class, sesionId));
    }

    @Test
    void cerrarSesionConLaCookieSirveAunqueElTokenDeAccesoHayaVencido() throws Exception {
        Sesion inicial = login("admin@sgtg.test", "Admin123*");

        MvcResult cierre = mvc.perform(post(API + "/auth/logout").cookie(new Cookie(COOKIE, inicial.renovacion())))
                .andReturn();

        assertEquals(204, cierre.getResponse().getStatus());
        // La cookie se borra en el navegador
        assertTrue(cierre.getResponse().getHeader(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));
        assertEquals(401, renovar(inicial.renovacion()).getResponse().getStatus());
        assertEquals(401, estado(inicial.acceso()));
        // Con un secreto que no es el de la sesión no se cierra la de otro
        Sesion otra = login("admin@sgtg.test", "Admin123*");
        mvc.perform(post(API + "/auth/logout").cookie(new Cookie(COOKIE, otra.renovacion().substring(0, 37) + "x")));
        assertEquals(200, estado(otra.acceso()));
    }

    @Test
    void cambiarLaContrasenaCierraLasDemasSesionesYMantieneLaActual() throws Exception {
        crearDocente();
        Sesion actual = login(correo(), CLAVE);
        Sesion otroEquipo = login(correo(), CLAVE);

        String nuevo = JsonPath.read(enviar(put(API + "/usuarios/me/password"), actual.acceso(),
                Map.of("passwordActual", CLAVE, "passwordNueva", "Nueva2026*")).getResponse().getContentAsString(),
                "$.token");

        assertEquals(200, estado(nuevo));
        // La sesión actual se sigue renovando con su misma cookie
        assertEquals(200, estado(sesion(renovar(actual.renovacion())).acceso()));
        // La del otro equipo quedó cerrada
        assertEquals(401, estado(otroEquipo.acceso()));
        assertEquals(401, renovar(otroEquipo.renovacion()).getResponse().getStatus());
    }

    @Test
    void inactivarAUnUsuarioCierraSusSesiones() throws Exception {
        long id = crearDocente();
        Sesion docente = login(correo(), CLAVE);

        enviar(put(API + "/usuarios/" + id + "/estado"), admin, Map.of("activo", false));

        assertEquals(401, estado(docente.acceso()));
        assertEquals(401, renovar(docente.renovacion()).getResponse().getStatus());
    }
}
