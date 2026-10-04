package com.sgtg.backend.infraestructure.input;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
 * Prueba de punta a punta por HTTP de la Fase 2 (H2, JWT real): con la
 * propuesta aprobada, el director radica el anteproyecto, el jefe del
 * departamento asigna los evaluadores y estos emiten su concepto, con una
 * ronda de correcciones, hasta que pasa a aprobación del Consejo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class FlujoAnteproyectoIntegracionTest {

    private static final String API = "/api/v1/sgtg";
    private static final String TRABAJOS = API + "/trabajos-grado/";
    private static final String CLAVE = "Clave123*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String admin;
    private long departamentoId;
    private long programaId;
    /** Sufijo único por prueba: la base de datos se comparte entre las pruebas de la clase */
    private String sufijo;
    private int documentos;

    // ----- Apoyo -----

    private String login(String codigo) throws Exception {
        return login(correo(codigo), CLAVE);
    }

    private String login(String email, String password) throws Exception {
        String respuesta = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, Object cuerpo)
            throws Exception {
        if (token != null)
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (cuerpo != null)
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        return mvc.perform(peticion);
    }

    private long id(ResultActions resultado, String ruta) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), ruta)).longValue();
    }

    private String correo(String codigo) {
        return codigo.toLowerCase() + sufijo + "@unicauca.edu.co";
    }

    /** Crea un usuario como administrador y devuelve su id. */
    private long crearUsuario(String codigo, String... roles) throws Exception {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", codigo + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", sufijo + (++documentos));
        datos.put("email", correo(codigo));
        datos.put("password", CLAVE);
        datos.put("primerNombre", "Nombre");
        datos.put("primerApellido", codigo);
        datos.put("roles", List.of(roles));
        return id(enviar(post(API + "/usuarios"), admin, datos).andExpect(status().isCreated()), "$.id");
    }

    private void perfilDocente(long usuarioId, long departamento) throws Exception {
        enviar(put(API + "/perfiles/docentes/" + usuarioId), admin,
                Map.of("tipoVinculacion", "PLANTA", "departamentoId", departamento)).andExpect(status().isOk());
    }

    private long crearDepartamento(String nombre) throws Exception {
        return id(enviar(post(API + "/catalogos/departamentos"), admin, Map.of("nombre", nombre + " " + sufijo))
                .andExpect(status().isCreated()), "$.id");
    }

    /**
     * Crea director (D), estudiante (E), comité (C), jefe de departamento (J) y
     * evaluadores (V, W), y deja un trabajo con la propuesta aprobada.
     *
     * @return id del trabajo en la etapa de anteproyecto
     */
    private long trabajoEnAnteproyecto() throws Exception {
        long docenteId = crearUsuario("D", "DOCENTE");
        long estudianteId = crearUsuario("E", "ESTUDIANTE");
        crearUsuario("C", "COMITE");
        long jefeId = crearUsuario("J", "JEFE_DEP", "DOCENTE");
        perfilDocente(docenteId, departamentoId);
        perfilDocente(jefeId, departamentoId);
        enviar(put(API + "/perfiles/estudiantes/" + estudianteId), admin, Map.of("programaId", programaId))
                .andExpect(status().isOk());

        String docente = login("D");
        long trabajoId = id(enviar(post(API + "/trabajos-grado/investigacion"), docente,
                Map.of("titulo", "Sistema de gestión", "objetivos", List.of("Objetivo general"),
                        "estudiantesIds", List.of(estudianteId)))
                .andExpect(status().isCreated()), "$.id");
        enviar(post(TRABAJOS + trabajoId + "/propuesta"), docente,
                Map.of("url", "https://archivos.unicauca.edu.co/propuesta.pdf")).andExpect(status().isOk());
        enviar(post(TRABAJOS + trabajoId + "/propuesta/decisiones"), login("C"),
                Map.of("resultado", "APROBADO", "acta", "Acta 01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"));
        return trabajoId;
    }

    @BeforeEach
    void prepararCatalogos() throws Exception {
        sufijo = String.valueOf(System.nanoTime());
        documentos = 0;
        admin = login("admin@sgtg.test", "Admin123*");
        departamentoId = crearDepartamento("Depto");
        programaId = id(enviar(post(API + "/catalogos/programas"), admin, Map.of("nombre", "Programa " + sufijo))
                .andExpect(status().isCreated()), "$.id");
    }

    // ----- Pruebas -----

    @Test
    void flujoCompletoDelAnteproyecto() throws Exception {
        long trabajoId = trabajoEnAnteproyecto();
        long evaluador1Id = crearUsuario("V", "EVALUADOR");
        long evaluador2Id = crearUsuario("W", "DOCENTE");
        String docente = login("D");
        String jefe = login("J");

        // El director radica el anteproyecto: queda esperando evaluadores
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto"), docente,
                Map.of("url", "https://archivos.unicauca.edu.co/anteproyecto.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.anteproyecto.estado").value("RADICADO"))
                .andExpect(jsonPath("$.anteproyecto.versiones[0].tipo").value("ANTEPROYECTO"));

        // El jefe de departamento ve el trabajo pendiente y asigna los evaluadores
        enviar(get(API + "/trabajos-grado").param("etapa", "ANTEPROYECTO"), jefe, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) trabajoId)));
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), jefe,
                Map.of("evaluador1Id", evaluador1Id, "evaluador2Id", evaluador2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"))
                .andExpect(jsonPath("$.anteproyecto.estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.anteproyecto.evaluadores[1].usuarioId").value(evaluador2Id))
                .andExpect(jsonPath("$.anteproyecto.fechaLimiteEvaluacion").isNotEmpty())
                .andExpect(jsonPath("$.historial[1].responsable").value("J" + sufijo));

        // Pedir correcciones exige observaciones
        String evaluador1 = login("V");
        String evaluador2 = login("W");
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/conceptos"), evaluador2,
                Map.of("resultado", "CON_CORRECCIONES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Las observaciones son obligatorias cuando el concepto no es aprobatorio."));

        // Un evaluador aprueba con el formato B y el otro pide correcciones
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/conceptos"), evaluador1,
                Map.of("resultado", "APROBADO", "urlFormato", "https://archivos.unicauca.edu.co/formato-b.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"))
                .andExpect(jsonPath("$.anteproyecto.conceptos[0].formato.formato").value("TI-B"));
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/conceptos"), evaluador2,
                Map.of("resultado", "CON_CORRECCIONES", "observaciones", "Ajustar el cronograma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.anteproyecto.estado").value("CON_CORRECCIONES"))
                .andExpect(jsonPath("$.anteproyecto.fechaLimiteCorrecciones").isNotEmpty());

        // El director radica la versión corregida: vuelve directo a evaluación
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto"), docente,
                Map.of("url", "https://archivos.unicauca.edu.co/anteproyecto_v2.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"))
                .andExpect(jsonPath("$.anteproyecto.versiones[1].version").value(2));

        // Ambos aprueban la versión 2: pasa a aprobación del Consejo
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/conceptos"), evaluador1, Map.of("resultado", "APROBADO"))
                .andExpect(status().isOk());
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/conceptos"), evaluador2, Map.of("resultado", "APROBADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("APROBACION_CONSEJO"))
                .andExpect(jsonPath("$.anteproyecto.estado").value("APROBADO"))
                .andExpect(jsonPath("$.anteproyecto.conceptos.length()").value(4));

        // El evaluador ve el trabajo entre los suyos
        enviar(get(API + "/trabajos-grado/mis-trabajos"), evaluador1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(trabajoId));
    }

    @Test
    void permisosDeLaEtapaDeAnteproyecto() throws Exception {
        long trabajoId = trabajoEnAnteproyecto();
        long evaluador1Id = crearUsuario("V", "EVALUADOR");
        long evaluador2Id = crearUsuario("W", "EVALUADOR");
        crearUsuario("X", "DOCENTE");
        long otroJefeId = crearUsuario("O", "JEFE_DEP", "DOCENTE");
        perfilDocente(otroJefeId, crearDepartamento("Otro"));
        Map<String, Object> evaluadores = Map.of("evaluador1Id", evaluador1Id, "evaluador2Id", evaluador2Id);

        // Solo el director radica
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto"), login("E"),
                Map.of("url", "https://archivos.unicauca.edu.co/anteproyecto.pdf"))
                .andExpect(status().isNotAcceptable());
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto"), login("D"),
                Map.of("url", "https://archivos.unicauca.edu.co/anteproyecto.pdf"))
                .andExpect(status().isOk());

        // Solo el rol JEFE_DEP asigna, y solo el del departamento del director
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), login("X"), evaluadores)
                .andExpect(status().isForbidden());
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), login("O"), evaluadores)
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message", containsString(
                        "Solo el jefe del departamento del director puede asignar los evaluadores.")));

        // El director no puede ser evaluador de su propio trabajo
        String jefe = login("J");
        long directorId = id(enviar(get(TRABAJOS + trabajoId), jefe, null), "$.director.usuarioId");
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), jefe,
                Map.of("evaluador1Id", evaluador1Id, "evaluador2Id", directorId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GC-0004"));

        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), jefe, evaluadores)
                .andExpect(status().isOk());

        // Solo los evaluadores asignados emiten concepto, y solo APROBADO o CON_CORRECCIONES
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/conceptos"), login("X"), Map.of("resultado", "APROBADO"))
                .andExpect(status().isNotAcceptable());
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/conceptos"), login("V"),
                Map.of("resultado", "RECHAZADO", "observaciones", "No cumple"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El concepto de un evaluador sobre el anteproyecto solo puede ser APROBADO o CON_CORRECCIONES."));

        // La consulta por etapa es solo para roles de consulta general y exige una etapa válida
        enviar(get(API + "/trabajos-grado").param("etapa", "ANTEPROYECTO"), login("E"), null)
                .andExpect(status().isForbidden());
        enviar(get(API + "/trabajos-grado").param("etapa", "NO_EXISTE"), jefe, null)
                .andExpect(status().isBadRequest());
        enviar(get(API + "/trabajos-grado"), jefe, null)
                .andExpect(status().isBadRequest());
    }
}
