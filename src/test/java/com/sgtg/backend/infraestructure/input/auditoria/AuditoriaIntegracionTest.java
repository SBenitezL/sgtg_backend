package com.sgtg.backend.infraestructure.input.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.sgtg.backend.infraestructure.output.auth.JwtService;
import com.sgtg.backend.infraestructure.output.percistence.repositories.AuditoriaRepository;

/** La auditoría registra quién hizo qué, cuándo, desde dónde y con qué resultado (H2, JWT real). */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class AuditoriaIntegracionTest {

    private static final String API = "/api/v1/sgtg";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwt;

    private Map<String, Object> ultimo() {
        return jdbc.queryForMap("SELECT * FROM auditoria ORDER BY id DESC LIMIT 1");
    }

    private long registros() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM auditoria", Long.class);
    }

    private String login(String email, String password) throws Exception {
        return mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "Navegador de prueba")
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void registraLosIniciosDeSesionConElCorreoYNuncaLaContrasena() throws Exception {
        login("nadie@unicauca.edu.co", "ClaveSecreta123*");
        Map<String, Object> fallido = ultimo();
        assertEquals("nadie@unicauca.edu.co", fallido.get("usuario"));
        assertNull(fallido.get("usuario_id"));
        assertEquals("NO_AUTENTICADO", fallido.get("resultado"));
        assertEquals("AuthController.login", fallido.get("operacion"));
        assertEquals("127.0.0.1", fallido.get("ip"));
        assertEquals("Navegador de prueba", fallido.get("agente_usuario"));
        assertFalse(fallido.values().stream().anyMatch(valor -> String.valueOf(valor).contains("ClaveSecreta123*")));

        login("admin@sgtg.test", "Admin123*");
        Map<String, Object> exitoso = ultimo();
        assertEquals("admin@sgtg.test", exitoso.get("usuario"));
        assertEquals("EXITOSA", exitoso.get("resultado"));
        assertEquals(200, exitoso.get("estado_http"));
    }

    @Test
    void registraLasOperacionesQueModificanConElUsuarioYElResultado() throws Exception {
        String token = JsonPath.read(login("admin@sgtg.test", "Admin123*"), "$.token");
        LocalDateTime antes = LocalDateTime.now().minusMinutes(1);

        // Una operación rechazada por regla de negocio (trabajo inexistente)
        mvc.perform(put(API + "/trabajos-grado/999999/reuniones/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaHora\":\"2030-01-10T10:00:00\",\"lugar\":\"Sala 2\"}"));
        Map<String, Object> registro = ultimo();
        assertEquals("admin@sgtg.test", registro.get("usuario"));
        assertTrue(String.valueOf(registro.get("roles")).contains("ADMINISTRADOR"));
        assertEquals("PUT", registro.get("metodo"));
        assertEquals("/api/v1/sgtg/trabajos-grado/999999/reuniones/1", registro.get("ruta"));
        assertEquals("TrabajoGradoController.agendarReunion", registro.get("operacion"));
        assertEquals(999999L, ((Number) registro.get("trabajo_id")).longValue());
        assertEquals("numero=1, trabajoId=999999", registro.get("parametros"));
        assertEquals("RECHAZADA", registro.get("resultado"));
        // Fecha real del servidor, no la del reloj ajustable
        assertTrue(((java.sql.Timestamp) registro.get("fecha")).toLocalDateTime().isAfter(antes));

        // Sin sesión también queda registrado el intento
        mvc.perform(put(API + "/trabajos-grado/1/reuniones/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        assertEquals("NO_AUTENTICADO", ultimo().get("resultado"));
        assertNull(ultimo().get("usuario_id"));
    }

    @Test
    void lasConsultasNoSeRegistranSalvoLasDescargas() throws Exception {
        String token = JsonPath.read(login("admin@sgtg.test", "Admin123*"), "$.token");
        long antes = registros();

        mvc.perform(get(API + "/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isOk());
        assertEquals(antes, registros());

        mvc.perform(get(API + "/trabajos-grado/999999/archivos?ruta=999999/TI-A/v1/a.pdf")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        assertEquals(antes + 1, registros());
        assertEquals("/api/v1/sgtg/trabajos-grado/999999/archivos?ruta=999999/TI-A/v1/a.pdf", ultimo().get("ruta"));
    }

    @Test
    void laSesionUneTodasLasAccionesDeUnInicioDeSesion() throws Exception {
        String token = JsonPath.read(login("admin@sgtg.test", "Admin123*"), "$.token");
        String sesion = jwt.getSesion(token);
        assertEquals(sesion, ultimo().get("sesion"));

        mvc.perform(put(API + "/trabajos-grado/999999/reuniones/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
        assertEquals(sesion, ultimo().get("sesion"));

        // El cierre de sesión queda registrado con el usuario
        mvc.perform(post(API + "/auth/logout?motivo=inactividad").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        Map<String, Object> cierre = ultimo();
        assertEquals("AuthController.logout", cierre.get("operacion"));
        assertEquals("admin@sgtg.test", cierre.get("usuario"));
        assertEquals("/api/v1/sgtg/auth/logout?motivo=inactividad", cierre.get("ruta"));
        assertEquals(sesion, cierre.get("sesion"));

        // Usar el token de una sesión cerrada queda registrado con esa sesión
        mvc.perform(put(API + "/trabajos-grado/999999/reuniones/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        assertEquals("NO_AUTENTICADO", ultimo().get("resultado"));
        assertEquals(sesion, ultimo().get("sesion"));
        assertNull(ultimo().get("usuario"));
    }

    @Test
    void elRepositorioSoloGuarda() {
        // Sin métodos de consulta, actualización ni borrado: la auditoría no se altera desde la aplicación
        List<String> metodos = java.util.Arrays.stream(AuditoriaRepository.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName).toList();
        assertEquals(List.of("save"), metodos);
        assertFalse(org.springframework.data.repository.CrudRepository.class.isAssignableFrom(AuditoriaRepository.class));
    }
}
