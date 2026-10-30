package com.sgtg.backend.infraestructure.input;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Inicio de sesión, estado activo/inactivo y cambio de contraseña por HTTP
 * (H2, JWT real).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class AutenticacionIntegracionTest {

    private static final String API = "/api/v1/sgtg";
    private static final String CLAVE = "Clave123*";
    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";
    private static final String ACCESO_BLOQUEADO = "Demasiados intentos fallidos. Por seguridad, el acceso con este"
            + " correo quedó bloqueado durante 15 minutos.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String admin;
    /** Sufijo único por prueba: la base de datos se comparte entre las pruebas de la clase */
    private String sufijo;

    // ----- Apoyo -----

    private ResultActions intentarLogin(String email, String password) throws Exception {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("email", email);
        cuerpo.put("password", password);
        return enviar(post(API + "/auth/login"), null, cuerpo);
    }

    private String login(String email, String password) throws Exception {
        return JsonPath.read(intentarLogin(email, password).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.token");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, Object cuerpo)
            throws Exception {
        if (token != null)
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (cuerpo != null)
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        return mvc.perform(peticion);
    }

    private String correo() {
        return "docente" + sufijo + "@unicauca.edu.co";
    }

    /** Crea un docente como administrador y devuelve su id. */
    private long crearDocente() throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", "D" + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", sufijo);
        datos.put("email", correo());
        datos.put("password", CLAVE);
        datos.put("primerNombre", "Docente");
        datos.put("primerApellido", "Prueba");
        datos.put("roles", List.of("DOCENTE"));
        String respuesta = enviar(post(API + "/usuarios"), admin, datos)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @BeforeEach
    void preparar() throws Exception {
        sufijo = String.valueOf(System.nanoTime());
        admin = login("admin@sgtg.test", "Admin123*");
    }

    // ----- Pruebas -----

    @Test
    void elLoginFallidoRespondeIgualExistaONoElCorreo() throws Exception {
        crearDocente();
        intentarLogin(correo(), "Incorrecta1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("GC-0005"))
                .andExpect(jsonPath("$.message").value(CREDENCIALES_INVALIDAS));
        intentarLogin("nadie" + sufijo + "@unicauca.edu.co", CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(CREDENCIALES_INVALIDAS));
        // Datos vacíos: error de validación
        intentarLogin("", "").andExpect(status().isBadRequest());
    }

    @Test
    void elTokenDaAccesoALosDatosDelUsuario() throws Exception {
        crearDocente();
        String token = login(correo(), CLAVE);
        enviar(get(API + "/usuarios/me"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(correo()))
                .andExpect(jsonPath("$.roles[0]").value("DOCENTE"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void unUsuarioInactivoNoIniciaSesionYSuTokenDejaDeServir() throws Exception {
        long docenteId = crearDocente();
        String token = login(correo(), CLAVE);
        String estado = API + "/usuarios/" + docenteId + "/estado";

        // Solo el administrador cambia el estado
        enviar(put(estado), token, Map.of("activo", false)).andExpect(status().isForbidden());
        enviar(put(estado), admin, Map.of("activo", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // El token que ya tenía deja de servir y no puede volver a iniciar sesión
        enviar(get(API + "/usuarios/me"), token, null).andExpect(status().isUnauthorized());
        intentarLogin(correo(), CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tu cuenta se encuentra inhabilitada. Contacta al administrador."));
        // Con la contraseña incorrecta no se revela que la cuenta está inactiva
        intentarLogin(correo(), "Incorrecta1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(CREDENCIALES_INVALIDAS));

        // Al reactivarlo vuelve a iniciar sesión
        enviar(put(estado), admin, Map.of("activo", true)).andExpect(status().isOk());
        login(correo(), CLAVE);
    }

    @Test
    void elAdministradorNoSeInactivaASiMismo() throws Exception {
        String respuesta = enviar(get(API + "/usuarios/me"), admin, null).andReturn().getResponse()
                .getContentAsString();
        long adminId = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        enviar(put(API + "/usuarios/" + adminId + "/estado"), admin, Map.of("activo", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Un administrador no puede inactivar su propia cuenta."));
    }

    @Test
    void elUsuarioCambiaSuContrasena() throws Exception {
        crearDocente();
        String token = login(correo(), CLAVE);
        String cambio = API + "/usuarios/me/password";

        // La actual incorrecta responde 400 (no 401): la sesión sigue vigente
        enviar(put(cambio), token, Map.of("passwordActual", "Incorrecta1", "passwordNueva", "NuevaClave1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta."));
        enviar(put(cambio), token, Map.of("passwordActual", CLAVE, "passwordNueva", "corta"))
                .andExpect(status().isBadRequest());
        enviar(put(cambio), token, Map.of("passwordActual", CLAVE, "passwordNueva", "NuevaClave1"))
                .andExpect(status().isNoContent());

        intentarLogin(correo(), CLAVE).andExpect(status().isUnauthorized());
        login(correo(), "NuevaClave1");
        // Sin token no se puede cambiar
        enviar(put(cambio), null, Map.of("passwordActual", "NuevaClave1", "passwordNueva", "OtraClave12"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trasCincoFallosSeguidosElCorreoQuedaBloqueado() throws Exception {
        crearDocente();
        for (int i = 1; i < 5; i++)
            intentarLogin(correo(), "Incorrecta1").andExpect(jsonPath("$.message").value(CREDENCIALES_INVALIDAS));
        intentarLogin(correo(), "Incorrecta1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ACCESO_BLOQUEADO));
        // Ni con la contraseña correcta entra mientras dure el bloqueo
        intentarLogin(correo(), CLAVE)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ACCESO_BLOQUEADO));
        // Los demás usuarios no se ven afectados
        login("admin@sgtg.test", "Admin123*");
    }

    @Test
    void cerrarSesionInvalidaElTokenAunqueNoHayaVencido() throws Exception {
        crearDocente();
        String token = login(correo(), CLAVE);
        String otraSesion = login(correo(), CLAVE);
        enviar(get(API + "/usuarios/me"), token, null).andExpect(status().isOk());

        enviar(post(API + "/auth/logout"), token, null).andExpect(status().isNoContent());

        enviar(get(API + "/usuarios/me"), token, null).andExpect(status().isUnauthorized());
        // Solo se cierra esa sesión
        enviar(get(API + "/usuarios/me"), otraSesion, null).andExpect(status().isOk());
        // Sin token, o con uno ya cerrado, también responde 204
        enviar(post(API + "/auth/logout"), null, null).andExpect(status().isNoContent());
        enviar(post(API + "/auth/logout"), token, null).andExpect(status().isNoContent());
    }

    @Test
    void yaNoExistenLosEndpointsDePrueba() throws Exception {
        enviar(get(API + "/auth/"), null, null).andExpect(status().isNotFound());
        enviar(get(API + "/test/"), admin, null).andExpect(status().isNotFound());
    }
}
