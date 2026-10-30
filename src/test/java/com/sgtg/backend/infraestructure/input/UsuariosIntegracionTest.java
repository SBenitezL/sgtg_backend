package com.sgtg.backend.infraestructure.input;

import static org.hamcrest.Matchers.contains;
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
 * Administración de usuarios por HTTP: creación con perfil académico, listado,
 * consulta y edición, y su efecto en la regla del codirector (Art. 6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class UsuariosIntegracionTest {

    private static final String API = "/api/v1/sgtg";
    private static final String CLAVE = "Clave123*";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String admin;
    private long departamentoId;
    private long programaId;
    private String sufijo;
    private int documentos;

    // ----- Apoyo -----

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, Object cuerpo)
            throws Exception {
        if (token != null)
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (cuerpo != null)
            peticion.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        return mvc.perform(peticion);
    }

    private String login(String email) throws Exception {
        return JsonPath.read(enviar(post(API + "/auth/login"), null, Map.of("email", email, "password", CLAVE))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    private long id(ResultActions resultado) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String correo(String codigo) {
        return codigo.toLowerCase() + sufijo + "@unicauca.edu.co";
    }

    private Map<String, Object> datos(String codigo, List<String> roles) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", codigo + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", sufijo + (++documentos));
        datos.put("email", correo(codigo));
        datos.put("password", CLAVE);
        datos.put("primerNombre", "Nombre");
        datos.put("primerApellido", codigo);
        datos.put("roles", roles);
        return datos;
    }

    private Map<String, Object> docente(String codigo, List<String> roles, String vinculacion) {
        Map<String, Object> datos = datos(codigo, roles);
        datos.put("tipoVinculacion", vinculacion);
        datos.put("departamentoId", departamentoId);
        return datos;
    }

    private long crear(Map<String, Object> datos) throws Exception {
        return id(enviar(post(API + "/usuarios"), admin, datos).andExpect(status().isCreated()));
    }

    @BeforeEach
    void preparar() throws Exception {
        sufijo = String.valueOf(System.nanoTime());
        documentos = 0;
        admin = JsonPath.read(enviar(post(API + "/auth/login"), null,
                Map.of("email", "admin@sgtg.test", "password", "Admin123*")).andReturn().getResponse()
                .getContentAsString(), "$.token");
        departamentoId = id(enviar(post(API + "/catalogos/departamentos"), admin, Map.of("nombre", "Depto " + sufijo)));
        programaId = id(enviar(post(API + "/catalogos/programas"), admin,
                Map.of("nombre", "Programa " + sufijo, "departamentoId", departamentoId)));
    }

    // ----- Pruebas -----

    @Test
    void cadaProgramaQuedaAdscritoAUnDepartamento() throws Exception {
        enviar(post(API + "/catalogos/programas"), admin, Map.of("nombre", "Sin depto " + sufijo))
                .andExpect(status().isBadRequest());
        enviar(post(API + "/catalogos/programas"), admin,
                Map.of("nombre", "Depto inexistente " + sufijo, "departamentoId", 999999))
                .andExpect(status().isNotFound());
        enviar(get(API + "/catalogos/programas"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + programaId + ")].departamento.id")
                        .value(contains((int) departamentoId)));
    }

    @Test
    void creaUsuariosConSuPerfilEnUnaSolaPeticion() throws Exception {
        enviar(post(API + "/usuarios"), admin, docente("D", List.of("DOCENTE", "DIRECTOR"), "OCASIONAL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoVinculacion").value("OCASIONAL"))
                .andExpect(jsonPath("$.departamento.id").value(departamentoId))
                .andExpect(jsonPath("$.programa").isEmpty());

        Map<String, Object> estudiante = datos("E", List.of("ESTUDIANTE"));
        estudiante.put("programaId", programaId);
        enviar(post(API + "/usuarios"), admin, estudiante)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.programa.id").value(programaId))
                .andExpect(jsonPath("$.tipoVinculacion").isEmpty());

        enviar(post(API + "/usuarios"), admin, datos("C", List.of("COMITE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departamento").isEmpty());
    }

    @Test
    void unPerfilInvalidoNoCreaElUsuario() throws Exception {
        Map<String, Object> sinDepartamento = datos("D", List.of("DIRECTOR"));
        sinDepartamento.put("tipoVinculacion", "PLANTA");
        enviar(post(API + "/usuarios"), admin, sinDepartamento)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El perfil docente requiere el tipo de vinculación y el departamento."));

        Map<String, Object> departamentoInexistente = docente("D", List.of("DIRECTOR"), "PLANTA");
        departamentoInexistente.put("departamentoId", 999999);
        enviar(post(API + "/usuarios"), admin, departamentoInexistente).andExpect(status().isNotFound());

        // El usuario no quedó creado: se puede crear con los mismos datos
        Map<String, Object> valido = docente("D", List.of("DIRECTOR"), "PLANTA");
        valido.put("numeroDocumento", sinDepartamento.get("numeroDocumento"));
        enviar(post(API + "/usuarios"), admin, valido).andExpect(status().isCreated());
    }

    @Test
    void listaUsuariosYSoloElAdministradorVeElDocumento() throws Exception {
        long estudianteId = crear(datos("E", List.of("ESTUDIANTE")));

        String filtro = "$[?(@.id == " + estudianteId + ")]";
        enviar(get(API + "/usuarios"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro + ".numeroDocumento").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath(filtro + ".numeroDocumento").value(org.hamcrest.Matchers
                        .contains(org.hamcrest.Matchers.notNullValue())));
        enviar(get(API + "/usuarios"), login(correo("E")), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro + ".email").value(org.hamcrest.Matchers.contains(correo("E"))))
                .andExpect(jsonPath(filtro + ".numeroDocumento").value(org.hamcrest.Matchers
                        .contains(org.hamcrest.Matchers.nullValue())));
        enviar(get(API + "/usuarios"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void elJefeGestionaLosUsuariosDeSuDepartamentoYSusProgramas() throws Exception {
        long jefeId = crear(docente("J", List.of("JEFE_DEP", "DOCENTE"), "PLANTA"));
        String jefe = login(correo("J"));
        Map<String, Object> datosEstudiante = datos("E", List.of("ESTUDIANTE"));
        datosEstudiante.put("programaId", programaId);
        long estudianteId = id(enviar(post(API + "/usuarios"), jefe, datosEstudiante).andExpect(status().isCreated()));

        // Ve al estudiante de su programa con su documento y lo puede modificar; a sí mismo (jefe) solo lo ve
        String estudiante = "$[?(@.usuario.id == " + estudianteId + ")]";
        enviar(get(API + "/usuarios/gestion"), jefe, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath(estudiante + ".modificable").value(contains(true)))
                .andExpect(jsonPath(estudiante + ".usuario.numeroDocumento")
                        .value(contains(datosEstudiante.get("numeroDocumento"))))
                .andExpect(jsonPath("$[?(@.usuario.id == " + jefeId + ")].modificable").value(contains(false)));
        enviar(get(API + "/usuarios/" + estudianteId), jefe, null).andExpect(status().isOk());

        // No asigna roles administrativos ni se modifica a sí mismo
        enviar(put(API + "/usuarios/" + estudianteId + "/roles"), jefe, Map.of("roles", List.of("ESTUDIANTE", "COMITE")))
                .andExpect(status().isForbidden());
        enviar(put(API + "/usuarios/" + jefeId + "/estado"), jefe, Map.of("activo", false))
                .andExpect(status().isBadRequest());
        enviar(put(API + "/usuarios/" + estudianteId + "/estado"), jefe, Map.of("activo", false))
                .andExpect(status().isOk());

        // Un docente sin rol administrativo no gestiona usuarios
        crear(docente("D", List.of("DOCENTE"), "PLANTA"));
        enviar(get(API + "/usuarios/gestion"), login(correo("D")), null).andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorEditaDatosRolesYPerfil() throws Exception {
        long docenteId = crear(docente("D", List.of("DOCENTE", "DIRECTOR"), "OCASIONAL"));
        String usuario = API + "/usuarios/" + docenteId;

        // Pasa a codirector: vinculación de planta; sin contraseña conserva la actual
        Map<String, Object> cambios = docente("D", List.of("DOCENTE", "CODIRECTOR"), "PLANTA");
        cambios.put("primerNombre", "Editado");
        cambios.put("password", "");
        enviar(put(usuario), admin, cambios)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primerNombre").value("Editado"))
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.containsInAnyOrder("DOCENTE", "CODIRECTOR")))
                .andExpect(jsonPath("$.tipoVinculacion").value("PLANTA"));
        enviar(get(usuario), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoVinculacion").value("PLANTA"));
        login(correo("D"));

        // Solo el administrador edita, y no puede repetir el correo de otro usuario
        enviar(put(usuario), login(correo("D")), cambios).andExpect(status().isForbidden());
        long otroId = crear(datos("O", List.of("DOCENTE")));
        Map<String, Object> repetido = datos("O", List.of("DOCENTE"));
        repetido.put("email", correo("D"));
        enviar(put(API + "/usuarios/" + otroId), admin, repetido).andExpect(status().isNotAcceptable());
    }

    @Test
    void unDirectorOcasionalNecesitaUnCodirectorDePlantaDeSuDepartamento() throws Exception {
        crear(docente("D", List.of("DOCENTE", "DIRECTOR"), "OCASIONAL"));
        long codirectorId = crear(docente("K", List.of("DOCENTE", "CODIRECTOR"), "PLANTA"));
        Map<String, Object> estudiante = datos("E", List.of("ESTUDIANTE"));
        estudiante.put("programaId", programaId);
        long estudianteId = crear(estudiante);
        String director = login(correo("D"));

        Map<String, Object> trabajo = new LinkedHashMap<>();
        trabajo.put("titulo", "Sistema de gestión");
        trabajo.put("objetivos", List.of("Objetivo general"));
        trabajo.put("estudiantesIds", List.of(estudianteId));
        enviar(post(API + "/trabajos-grado/investigacion"), director, trabajo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Si el director no es de planta, se requiere un codirector de planta del mismo departamento."));

        trabajo.put("codirectorId", codirectorId);
        enviar(post(API + "/trabajos-grado/investigacion"), director, trabajo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.director.tipoVinculacion").value("OCASIONAL"))
                .andExpect(jsonPath("$.codirector.usuarioId").value(codirectorId));
    }
}
