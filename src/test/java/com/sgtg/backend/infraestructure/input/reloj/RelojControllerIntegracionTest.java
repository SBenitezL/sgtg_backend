package com.sgtg.backend.infraestructure.input.reloj;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.sgtg.backend.infraestructure.configuration.RelojAjustable;

/** Reloj de desarrollo activo con sgtg.reloj.ajustable=true: solo el administrador lo adelanta. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*",
        "sgtg.reloj.ajustable=true" })
class RelojControllerIntegracionTest {

    private static final String RELOJ = "/api/v1/sgtg/reloj";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private RelojAjustable reloj;

    @AfterEach
    void restablecer() {
        reloj.restablecer();
    }

    private String tokenAdmin() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/sgtg/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "admin@sgtg.test", "password", "Admin123*"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(respuesta, "$.token");
    }

    @Test
    void elAdministradorAdelantaYRestableceElReloj() throws Exception {
        String admin = tokenAdmin();
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

        mvc.perform(post(RELOJ + "/avanzar").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dias\": 100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desplazamientoDias").value(100))
                .andExpect(jsonPath("$.fecha").value(hoy.plusDays(100).toString()));
        mvc.perform(post(RELOJ + "/avanzar").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dias\": 0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(RELOJ + "/tareas/vencimientos").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizados").isNumber());

        mvc.perform(delete(RELOJ).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desplazamientoDias").value(0));
    }

    @Test
    void sinTokenNoSeAdelanta() throws Exception {
        mvc.perform(post(RELOJ + "/avanzar").contentType(MediaType.APPLICATION_JSON).content("{\"dias\": 1}"))
                .andExpect(status().isUnauthorized());
    }
}
