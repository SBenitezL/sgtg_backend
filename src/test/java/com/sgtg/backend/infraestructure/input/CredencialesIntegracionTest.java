package com.sgtg.backend.infraestructure.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Credenciales separadas de los usuarios, cambio obligatorio de la contraseña
 * asignada por un administrador, historial de contraseñas y tokens que dejan de
 * servir al cambiarla (H2, JWT real).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*",
        "sgtg.seguridad.cambio-password-obligatorio=true" })
class CredencialesIntegracionTest {

    private static final String API = "/api/v1/sgtg";
    private static final String CAMBIO = API + "/usuarios/me/password";
    private static final String ASIGNADA = "Asignada1*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    private String admin;
    private String sufijo;

    // ----- Apoyo -----

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, Object cuerpo)
            throws Exception {
        if (token != null)
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (cuerpo != null)
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        return mvc.perform(peticion);
    }

    private String login(String email, String password) throws Exception {
        return JsonPath.read(enviar(post(API + "/auth/login"), null, Map.of("email", email, "password", password))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private String cambiar(String token, String actual, String nueva) throws Exception {
        return JsonPath.read(enviar(put(CAMBIO), token, Map.of("passwordActual", actual, "passwordNueva", nueva))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private String correo() {
        return "cred" + sufijo + "@unicauca.edu.co";
    }

    private long crearDocente() throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", "C" + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", sufijo);
        datos.put("email", correo());
        datos.put("password", ASIGNADA);
        datos.put("primerNombre", "Docente");
        datos.put("primerApellido", "Prueba");
        datos.put("roles", List.of("DOCENTE"));
        return ((Number) JsonPath.read(enviar(post(API + "/usuarios"), admin, datos)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id"))
                .longValue();
    }

    /** Claims del token, sin verificar la firma (eso ya lo hace el backend). */
    private static Map<String, Object> claims(String token) throws Exception {
        String carga = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        return new ObjectMapper().readValue(carga, Map.class);
    }

    @BeforeEach
    void preparar() throws Exception {
        sufijo = String.valueOf(System.nanoTime());
        admin = login("admin@sgtg.test", "Admin123*");
    }

    // ----- Pruebas -----

    @Test
    void laContrasenaSeGuardaAparteDeLosDatosDelUsuario() throws Exception {
        long id = crearDocente();

        Integer columnas = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns"
                + " WHERE LOWER(table_name) = 'users' AND LOWER(column_name) = 'password'", Integer.class);
        assertEquals(0, columnas);
        // Las credenciales y el historial no pueden quedar de un usuario que no existe
        Integer llaves = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints"
                + " WHERE constraint_type = 'FOREIGN KEY'"
                + " AND LOWER(table_name) IN ('credenciales', 'historial_passwords')", Integer.class);
        assertEquals(2, llaves);
        Map<String, Object> credencial = jdbc.queryForMap(
                "SELECT password_hash, debe_cambiar, version FROM credenciales WHERE usuario_id = ?", id);
        assertTrue(((String) credencial.get("password_hash")).startsWith("$2a$"));
        assertEquals(true, credencial.get("debe_cambiar"));
        assertEquals(0, ((Number) credencial.get("version")).intValue());
    }

    @Test
    void conLaContrasenaAsignadaSoloPuedeCambiarla() throws Exception {
        crearDocente();
        String token = login(correo(), ASIGNADA);
        assertEquals(true, claims(token).get("cambioPassword"));

        // Puede ver sus datos y cerrar sesión, pero nada más
        enviar(get(API + "/usuarios/me"), token, null).andExpect(status().isOk());
        enviar(get(API + "/usuarios"), token, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("GC-0012"))
                .andExpect(jsonPath("$.message")
                        .value("Debes cambiar la contraseña que te asignaron antes de continuar."));
        enviar(get(API + "/notificaciones"), token, null).andExpect(status().isForbidden());

        String nuevo = cambiar(token, ASIGNADA, "MiClave2026*");

        assertFalse(claims(nuevo).containsKey("cambioPassword"));
        enviar(get(API + "/usuarios"), nuevo, null).andExpect(status().isOk());
        // El token con el que entró ya no sirve
        enviar(get(API + "/usuarios/me"), token, null).andExpect(status().isUnauthorized());
        // En el siguiente ingreso ya no se le pide
        assertFalse(claims(login(correo(), "MiClave2026*")).containsKey("cambioPassword"));
    }

    @Test
    void cuandoElAdministradorAsignaOtraContrasenaSeCierranLasSesiones() throws Exception {
        long id = crearDocente();
        String token = cambiar(login(correo(), ASIGNADA), ASIGNADA, "MiClave2026*");

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", "C" + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", sufijo);
        datos.put("email", correo());
        datos.put("password", "Restablecida1*");
        datos.put("primerNombre", "Docente");
        datos.put("primerApellido", "Prueba");
        datos.put("roles", List.of("DOCENTE"));
        enviar(put(API + "/usuarios/" + id), admin, datos).andExpect(status().isOk());

        enviar(get(API + "/usuarios/me"), token, null).andExpect(status().isUnauthorized());
        // Con la contraseña restablecida vuelve a tener que cambiarla
        assertEquals(true, claims(login(correo(), "Restablecida1*")).get("cambioPassword"));
    }

    @Test
    void noSePuedeRepetirNingunaDeLasUltimasCincoContrasenas() throws Exception {
        crearDocente();
        String token = login(correo(), ASIGNADA);
        String actual = ASIGNADA;
        for (String nueva : List.of("Segunda22*", "Tercera33*", "Cuarta444*", "Quinta555*")) {
            token = cambiar(token, actual, nueva);
            actual = nueva;
        }

        // La asignada es la quinta más reciente (con la actual): todavía no se puede usar
        enviar(put(CAMBIO), token, Map.of("passwordActual", actual, "passwordNueva", ASIGNADA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La contraseña nueva no puede ser ninguna de las últimas 5 que has usado."));
        token = cambiar(token, actual, "Sexta6666*");
        // Ya salió de las últimas cinco
        cambiar(token, "Sexta6666*", ASIGNADA);
        Integer historial = jdbc.queryForObject("SELECT COUNT(*) FROM historial_passwords h"
                + " JOIN users u ON u.cusuari = h.usuario_id WHERE u.email = ?", Integer.class, correo());
        assertEquals(4, historial);
    }

    @Test
    void laContrasenaDebeCumplirLaPolitica() throws Exception {
        crearDocente();
        String token = login(correo(), ASIGNADA);
        enviar(put(CAMBIO), token, Map.of("passwordActual", ASIGNADA, "passwordNueva", "sinmayuscula1*"))
                .andExpect(status().isBadRequest());
        enviar(put(CAMBIO), token, Map.of("passwordActual", ASIGNADA, "passwordNueva", "Clave" + sufijo + "*"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith(
                        "La contraseña es fácil de adivinar")));
    }
}
