package com.sgtg.backend.infraestructure.input;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Prueba de punta a punta por HTTP (H2, JWT real): el administrador prepara
 * catálogos, usuarios y perfiles; el docente crea el trabajo y radica la
 * propuesta; el comité la aprueba. Verifica también permisos y mensajes de
 * error.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class FlujoPropuestaIntegracionTest {

    private static final String API = "/api/v1/sgtg";
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

    // ----- Apoyo -----

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

    /**
     * Petición multipart: el JSON {@code datos} (si no es null) en la parte
     * {@code datos} y los archivos en sus partes.
     */
    private ResultActions subir(String url, String token, Object datos, MockMultipartFile... archivos)
            throws Exception {
        MockMultipartHttpServletRequestBuilder peticion = multipart(url);
        if (datos != null)
            peticion.file(new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                    json.writeValueAsBytes(datos)));
        for (MockMultipartFile archivo : archivos)
            peticion.file(archivo);
        if (token != null)
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return mvc.perform(peticion);
    }

    /** PDF de prueba en la parte {@code archivo}. */
    private static MockMultipartFile archivo(String nombre) {
        return pdf("archivo", nombre);
    }

    private static MockMultipartFile pdf(String parte, String nombre) {
        return new MockMultipartFile(parte, nombre, MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII));
    }

    private long id(ResultActions resultado, String ruta) throws Exception {
        return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), ruta)).longValue();
    }

    /** Cuerpo para crear un usuario: el documento se deriva del sufijo para que sea único. */
    private Map<String, Object> datosUsuario(String codigo, String numeroDocumento, String rol) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", codigo + sufijo);
        datos.put("tipoDocumento", "CC");
        datos.put("numeroDocumento", numeroDocumento);
        datos.put("email", correo(codigo));
        datos.put("password", CLAVE);
        datos.put("primerNombre", "Nombre");
        datos.put("segundoNombre", codigo);
        datos.put("primerApellido", "Apellido");
        datos.put("roles", List.of(rol));
        return datos;
    }

    /** Número de documento único por prueba (solo dígitos). */
    private String documento(int n) {
        return sufijo + n;
    }

    /** Crea un usuario como administrador y devuelve su id. */
    private long crearUsuario(String codigo, String rol) throws Exception {
        return id(enviar(post(API + "/usuarios"), admin, datosUsuario(codigo, documento(codigo.charAt(0)), rol))
                .andExpect(status().isCreated()), "$.id");
    }

    private String correo(String codigo) {
        return codigo.toLowerCase() + sufijo + "@unicauca.edu.co";
    }

    @BeforeEach
    void prepararCatalogos() throws Exception {
        sufijo = String.valueOf(System.nanoTime());
        admin = login("admin@sgtg.test", "Admin123*");
        departamentoId = id(enviar(post(API + "/catalogos/departamentos"), admin, Map.of("nombre", "Depto " + sufijo))
                .andExpect(status().isCreated()), "$.id");
        programaId = id(enviar(post(API + "/catalogos/programas"), admin, Map.of("nombre", "Programa " + sufijo))
                .andExpect(status().isCreated()), "$.id");
    }

    // ----- Pruebas -----

    @Test
    void flujoCompletoDeLaPropuesta() throws Exception {
        long docenteId = crearUsuario("D", "DOCENTE");
        long estudianteId = crearUsuario("E", "ESTUDIANTE");
        crearUsuario("C", "COMITE");
        enviar(put(API + "/perfiles/docentes/" + docenteId), admin,
                Map.of("tipoVinculacion", "PLANTA", "departamentoId", departamentoId)).andExpect(status().isOk());
        enviar(put(API + "/perfiles/estudiantes/" + estudianteId), admin, Map.of("programaId", programaId))
                .andExpect(status().isOk());

        // El docente crea el trabajo y queda como director
        String docente = login(correo("D"), CLAVE);
        long trabajoId = id(enviar(post(API + "/trabajos-grado/investigacion"), docente,
                Map.of("titulo", "Sistema de gestión", "objetivos", List.of("Objetivo general"),
                        "estudiantesIds", List.of(estudianteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.etapa").value("PROPUESTA"))
                .andExpect(jsonPath("$.director.usuarioId").value(docenteId))
                .andExpect(jsonPath("$.director.tipoVinculacion").value("PLANTA")), "$.id");

        // Solo se aceptan documentos PDF
        subir(API + "/trabajos-grado/" + trabajoId + "/propuesta", docente, null, new MockMultipartFile("archivo",
                "propuesta.docx", "application/msword", "contenido".getBytes(StandardCharsets.US_ASCII)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solo se permiten documentos en formato PDF."));

        // Radica la propuesta: se guarda en la carpeta del formato y la respuesta solo trae la ruta
        subir(API + "/trabajos-grado/" + trabajoId + "/propuesta", docente, null, archivo("propuesta.pdf"))
                .andExpect(status().isOk())
                // El estado es de la versión: queda en revisión del Comité
                .andExpect(jsonPath("$.propuesta.estado").doesNotExist())
                .andExpect(jsonPath("$.propuesta.versiones[0].estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.propuesta.versiones[0].formato").value("TI-A"))
                .andExpect(jsonPath("$.propuesta.versiones[0].ruta").value(trabajoId + "/TI-A/v1/propuesta.pdf"));

        // El comité no la aprueba: sigue en propuesta y se radica una nueva versión
        String comite = login(correo("C"), CLAVE);
        subir(API + "/trabajos-grado/" + trabajoId + "/propuesta/decisiones", comite,
                Map.of("resultado", "NO_APROBADO", "acta", "Acta 01", "observaciones", "Precisar los objetivos"),
                archivo("acta-01.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("PROPUESTA"))
                .andExpect(jsonPath("$.propuesta.versiones[0].estado").value("NO_APROBADO"))
                .andExpect(jsonPath("$.propuesta.decisiones[0].rutaArchivo")
                        .value(trabajoId + "/ACTA_COMITE/v1/acta-01.pdf"));
        subir(API + "/trabajos-grado/" + trabajoId + "/propuesta", docente, null, archivo("propuesta_v2.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propuesta.versiones[0].estado").value("NO_APROBADO"))
                .andExpect(jsonPath("$.propuesta.versiones[1].estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.propuesta.versiones[1].version").value(2))
                .andExpect(jsonPath("$.propuesta.versiones[1].ruta").value(trabajoId + "/TI-A/v2/propuesta_v2.pdf"));

        // El comité aprueba: el trabajo pasa a anteproyecto
        subir(API + "/trabajos-grado/" + trabajoId + "/propuesta/decisiones", comite,
                Map.of("resultado", "APROBADO", "acta", "Acta 02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.propuesta.versiones[0].estado").value("NO_APROBADO"))
                .andExpect(jsonPath("$.propuesta.versiones[1].estado").value("APROBADO"))
                .andExpect(jsonPath("$.historial[0].destino").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.historial[0].responsable").value("C" + sufijo));

        // El director ganó el rol DIRECTOR al crear el trabajo
        enviar(get(API + "/usuarios/me"), docente, null)
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("DIRECTOR")));

        // El estudiante consulta su trabajo
        String estudiante = login(correo("E"), CLAVE);
        enviar(get(API + "/trabajos-grado/" + trabajoId), estudiante, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiantes[0].usuarioId").value(estudianteId))
                .andExpect(jsonPath("$.estudiantes[0].nombreCompleto").value("Nombre E Apellido"));
        enviar(get(API + "/trabajos-grado/mis-trabajos"), estudiante, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(trabajoId));

        // Los archivos se descargan por su ruta: el estudiante y el comité sí pueden
        String archivos = API + "/trabajos-grado/" + trabajoId + "/archivos";
        String ruta = trabajoId + "/TI-A/v1/propuesta.pdf";
        for (String usuario : List.of(estudiante, comite))
            enviar(get(archivos).param("ruta", ruta), usuario, null)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("propuesta.pdf")))
                    .andExpect(content().bytes("%PDF-1.4 prueba".getBytes(StandardCharsets.US_ASCII)));
        // Un usuario sin relación con el trabajo no tiene acceso
        crearUsuario("X", "DOCENTE");
        enviar(get(archivos).param("ruta", ruta), login(correo("X"), CLAVE), null)
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message", containsString("No tiene acceso a la información de este trabajo")));
        // Rutas de otro trabajo o inexistentes
        enviar(get(archivos).param("ruta", (trabajoId + 1) + "/TI-A/v1/propuesta.pdf"), estudiante, null)
                .andExpect(status().isNotFound());
        enviar(get(archivos).param("ruta", trabajoId + "/TI-A/v9/propuesta.pdf"), estudiante, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void lasReglasDeNegocioRespondenConElMensajeEnEspanol() throws Exception {
        long docenteId = crearUsuario("D", "DOCENTE");
        long estudianteId = crearUsuario("E", "ESTUDIANTE");
        enviar(put(API + "/perfiles/docentes/" + docenteId), admin,
                Map.of("tipoVinculacion", "CATEDRA", "departamentoId", departamentoId)).andExpect(status().isOk());
        enviar(put(API + "/perfiles/estudiantes/" + estudianteId), admin, Map.of("programaId", programaId))
                .andExpect(status().isOk());

        // Director de cátedra sin codirector: regla del Art. 6
        enviar(post(API + "/trabajos-grado/investigacion"), login(correo("D"), CLAVE),
                Map.of("titulo", "Título", "objetivos", List.of("Objetivo"), "estudiantesIds", List.of(estudianteId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GC-0004"))
                .andExpect(jsonPath("$.message")
                        .value("Si el director no es de planta, se requiere un codirector de planta del mismo departamento."));
    }

    @Test
    void sinTokenRespondeNoAutenticado() throws Exception {
        enviar(get(API + "/usuarios/me"), null, null).andExpect(status().isUnauthorized());
        enviar(get(API + "/usuarios/me"), "token-invalido", null).andExpect(status().isUnauthorized());
    }

    @Test
    void cadaOperacionExigeSuRol() throws Exception {
        crearUsuario("E", "ESTUDIANTE");
        String estudiante = login(correo("E"), CLAVE);

        // Un estudiante no crea trabajos, usuarios ni catálogos
        enviar(post(API + "/trabajos-grado/investigacion"), estudiante,
                Map.of("titulo", "T", "objetivos", List.of("O"), "estudiantesIds", List.of(1)))
                .andExpect(status().isForbidden());
        enviar(post(API + "/usuarios"), estudiante, datosUsuario("X", documento(9), "ADMINISTRADOR"))
                .andExpect(status().isForbidden());
        enviar(post(API + "/catalogos/programas"), estudiante, Map.of("nombre", "P"))
                .andExpect(status().isForbidden());
        // Pero sí consulta sus propios datos
        enviar(get(API + "/usuarios/me"), estudiante, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.primerNombre").value("Nombre"))
                .andExpect(jsonPath("$.segundoNombre").value("E"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void elRelojDeDesarrolloNoSeExponePorDefecto() throws Exception {
        enviar(post(API + "/reloj/avanzar"), admin, Map.of("dias", 1)).andExpect(status().isNotFound());
    }

    @Test
    void noExisteRegistroPublico() throws Exception {
        enviar(post(API + "/auth/register"), null, datosUsuario("X", documento(9), "ADMINISTRADOR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void elAdministradorValidaLosRolesAlCrearUsuarios() throws Exception {
        enviar(post(API + "/usuarios"), admin, datosUsuario("R", documento(1), "NO_EXISTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Uno o más roles indicados no existen."));
    }

    @Test
    void elNumeroDeDocumentoSoloAdmiteDigitos() throws Exception {
        enviar(post(API + "/usuarios"), admin, datosUsuario("N", "10A23", "ESTUDIANTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.numeroDocumento").value("El número de documento solo puede contener dígitos"));
    }

    @Test
    void noSeRepiteElDocumento() throws Exception {
        crearUsuario("A", "ESTUDIANTE");
        // Otro usuario (código y correo distintos) con el mismo tipo y número de documento
        Map<String, Object> repetido = datosUsuario("B", documento('A'), "ESTUDIANTE");
        enviar(post(API + "/usuarios"), admin, repetido)
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.errorCode").value("GC-0002"));
    }
}
