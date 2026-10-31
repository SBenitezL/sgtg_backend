package com.sgtg.backend.infraestructure.input.auditoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.sgtg.backend.application.input.seguridad.ManageSeguridadCUIntPort;
import com.sgtg.backend.application.output.ManageAuditoriaGatewayIntPort;
import com.sgtg.backend.domain.models.auditoria.VerificacionCadena;

/**
 * Auditoría avanzada (H2, JWT real): cadena de sellos, identificador de la
 * petición, cambios antes → después y alertas de seguridad.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class SeguridadIntegracionTest {

    private static final String API = "/api/v1/sgtg";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ManageAuditoriaGatewayIntPort auditoria;

    @Autowired
    private ManageSeguridadCUIntPort seguridad;

    private String admin;
    private String sufijo;

    // ----- Apoyo -----

    private MvcResult enviar(MockHttpServletRequestBuilder peticion, String token, Object cuerpo) throws Exception {
        if (token != null)
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (cuerpo != null)
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        return mvc.perform(peticion).andReturn();
    }

    private String login(String email, String password) throws Exception {
        return JsonPath.read(enviar(post(API + "/auth/login"), null, Map.of("email", email, "password", password))
                .getResponse().getContentAsString(), "$.token");
    }

    private Map<String, Object> ultimo() {
        return jdbc.queryForMap("SELECT * FROM auditoria ORDER BY id DESC LIMIT 1");
    }

    /** Una operación cualquiera que queda en la auditoría (rechazada: el trabajo no existe) */
    private MvcResult operacion(MockHttpServletRequestBuilder peticion) throws Exception {
        return enviar(peticion, admin, Map.of("fechaHora", "2030-01-10T10:00:00", "lugar", "Sala 2"));
    }

    private Map<String, Object> datosDeUsuario(String nombre, String password) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", "S" + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", sufijo);
        datos.put("email", "seg" + sufijo + "@unicauca.edu.co");
        datos.put("password", password);
        datos.put("primerNombre", nombre);
        datos.put("primerApellido", "Prueba");
        datos.put("roles", List.of("DOCENTE"));
        return datos;
    }

    private long alertas(String tipo, String clave) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM alertas_seguridad WHERE tipo = ? AND clave = ?", Long.class,
                tipo, clave);
    }

    private long avisosAlAdministrador(String titulo) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notificaciones n JOIN users u ON u.cusuari = n.usuario_id"
                + " WHERE u.email = 'admin@sgtg.test' AND n.titulo = ? AND n.trabajo_id IS NULL", Long.class, titulo);
    }

    /** Vuelve a insertar un registro de la auditoría tal como estaba */
    private void restaurar(Map<String, Object> registro) {
        String columnas = String.join(", ", registro.keySet());
        String marcas = registro.keySet().stream().map(columna -> "?").collect(Collectors.joining(", "));
        jdbc.update("INSERT INTO auditoria (" + columnas + ") VALUES (" + marcas + ")", registro.values().toArray());
    }

    @BeforeEach
    void preparar() throws Exception {
        sufijo = String.valueOf(System.nanoTime());
        admin = login("admin@sgtg.test", "Admin123*");
    }

    // ----- Cadena de sellos -----

    @Test
    void cadaRegistroQuedaSelladoAContinuacionDelAnterior() throws Exception {
        operacion(put(API + "/trabajos-grado/999999/reuniones/1"));
        Map<String, Object> anterior = ultimo();
        operacion(put(API + "/trabajos-grado/999999/reuniones/2"));
        Map<String, Object> registro = ultimo();

        assertEquals(((Number) anterior.get("secuencia")).longValue() + 1, ((Number) registro.get("secuencia")).longValue());
        assertEquals(anterior.get("sello"), registro.get("sello_anterior"));
        assertEquals(64, ((String) registro.get("sello")).length());
        assertTrue(auditoria.verificarCadena().integra());
    }

    @Test
    void modificarUnRegistroEnLaBaseRompeLaCadenaYSeAvisaALosAdministradores() throws Exception {
        operacion(put(API + "/trabajos-grado/999999/reuniones/1"));
        Map<String, Object> registro = ultimo();
        Object id = registro.get("id");

        jdbc.update("UPDATE auditoria SET usuario = 'otro@unicauca.edu.co' WHERE id = ?", id);
        VerificacionCadena alterada = auditoria.verificarCadena();
        assertFalse(alterada.integra());
        assertTrue(alterada.falla().contains("(id " + id + ") fue modificado"));

        seguridad.verificarIntegridad();
        assertEquals(1, alertas("AUDITORIA_ALTERADA", "cadena"));
        assertEquals(1, avisosAlAdministrador("Alerta de seguridad: La auditoría fue alterada"));

        jdbc.update("UPDATE auditoria SET usuario = ? WHERE id = ?", registro.get("usuario"), id);
        assertTrue(auditoria.verificarCadena().integra());
    }

    @Test
    void borrarRegistrosEnLaBaseSeDetecta() throws Exception {
        operacion(put(API + "/trabajos-grado/999999/reuniones/1"));
        Map<String, Object> penultimo = ultimo();
        operacion(put(API + "/trabajos-grado/999999/reuniones/2"));
        Map<String, Object> ultimo = ultimo();

        // Uno del medio: falta su número
        jdbc.update("DELETE FROM auditoria WHERE id = ?", penultimo.get("id"));
        assertTrue(auditoria.verificarCadena().falla().startsWith("falta el registro " + penultimo.get("secuencia")));
        restaurar(penultimo);

        // El último: la cadena llega más lejos que los registros
        jdbc.update("DELETE FROM auditoria WHERE id = ?", ultimo.get("id"));
        assertTrue(auditoria.verificarCadena().falla().endsWith("se borraron los últimos registros"));
        restaurar(ultimo);

        assertTrue(auditoria.verificarCadena().integra());
    }

    // ----- Identificador de la petición -----

    @Test
    void cadaPeticionTieneSuIdentificadorEnLaRespuestaYEnLaAuditoria() throws Exception {
        String generado = operacion(put(API + "/trabajos-grado/999999/reuniones/1")).getResponse()
                .getHeader("X-Request-Id");
        assertNotNull(generado);
        assertEquals(generado, ultimo().get("solicitud"));

        // El de Nginx se conserva
        String deNginx = operacion(put(API + "/trabajos-grado/999999/reuniones/1").header("X-Request-Id",
                "a1b2c3d4e5f60718")).getResponse().getHeader("X-Request-Id");
        assertEquals("a1b2c3d4e5f60718", deNginx);
        assertEquals("a1b2c3d4e5f60718", ultimo().get("solicitud"));

        // Uno con caracteres extraños se reemplaza: no puede ensuciar el log
        String reemplazado = operacion(put(API + "/trabajos-grado/999999/reuniones/1").header("X-Request-Id",
                "malo<script>alert(1)</script>")).getResponse().getHeader("X-Request-Id");
        assertFalse(reemplazado.contains("malo"));
    }

    // ----- Cambios antes → después -----

    @Test
    void losCambiosDeUnUsuarioQuedanConSuValorAnteriorYNuevo() throws Exception {
        MvcResult creacion = enviar(post(API + "/usuarios"), admin, datosDeUsuario("Docente", "Clave123*"));
        assertEquals(201, creacion.getResponse().getStatus());
        long id = ((Number) JsonPath.read(creacion.getResponse().getContentAsString(), "$.id")).longValue();
        assertTrue(((String) ultimo().get("detalle")).startsWith("Usuario creado: " + id + " (seg" + sufijo));

        enviar(put(API + "/usuarios/" + id), admin, datosDeUsuario("Docenta", "Asignada1*"));
        String edicion = (String) ultimo().get("detalle");
        assertTrue(edicion.contains("usuario " + id + " · nombre: Docente Prueba → Docenta Prueba"));
        // De la contraseña solo queda que cambió, nunca su valor ni su hash
        assertTrue(edicion.contains("usuario " + id + " · contraseña: versión 0 → versión 1"));
        assertFalse(edicion.contains("Asignada1*"));

        enviar(put(API + "/usuarios/" + id + "/estado"), admin, Map.of("activo", false));
        assertEquals("usuario " + id + " · activo: sí → no", ultimo().get("detalle"));

        // Una operación rechazada no cambió nada
        enviar(put(API + "/usuarios/" + id + "/estado"), admin, Map.of());
        assertEquals(null, ultimo().get("detalle"));
    }

    // ----- Alertas -----

    @Test
    void muchosLoginsFallidosDesdeUnaIpSeAvisanALosAdministradores() throws Exception {
        String correo = "fuerza" + sufijo + "@ejemplo.com";
        for (int i = 0; i < 10; i++)
            mvc.perform(post(API + "/auth/login").with(peticion -> {
                peticion.setRemoteAddr("10.9.8.7");
                return peticion;
            }).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("email", correo, "password", "Incorrecta1*"))))
                    .andExpect(status().isUnauthorized());

        assertTrue(seguridad.detectarAlertas() >= 2);
        assertEquals(1, alertas("FUERZA_BRUTA_IP", "10.9.8.7"));
        assertEquals(1, alertas("CORREO_BLOQUEADO", correo));
        assertTrue(avisosAlAdministrador("Alerta de seguridad: Muchos inicios de sesión fallidos desde una misma IP") >= 1);

        // No se repite el aviso del mismo hecho
        seguridad.detectarAlertas();
        assertEquals(1, alertas("FUERZA_BRUTA_IP", "10.9.8.7"));
    }

    @Test
    void usarUnaSesionCerradaSeAvisa() throws Exception {
        String token = login("admin@sgtg.test", "Admin123*");
        String sesion = (String) ultimo().get("sesion");
        enviar(post(API + "/auth/logout"), token, null);

        assertEquals(401, enviar(put(API + "/trabajos-grado/999999/reuniones/1"), token, Map.of()).getResponse()
                .getStatus());
        seguridad.detectarAlertas();
        assertEquals(1, alertas("SESION_CERRADA_USADA", sesion));
    }
}
