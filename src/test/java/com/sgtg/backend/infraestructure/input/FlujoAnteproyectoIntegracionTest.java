package com.sgtg.backend.infraestructure.input;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Prueba de punta a punta por HTTP de la Fase 2 (H2, JWT real): con la
 * propuesta aprobada, el director radica el anteproyecto, el jefe del
 * departamento asigna los evaluadores y estos emiten su concepto, con una
 * ronda de correcciones, hasta que pasa a aprobación del Consejo. Cubre también
 * la Fase 3 (el Consejo de Facultad decide con la resolución adjunta) y la
 * Fase 4 (cambio de título u objetivos con el trabajo en curso), la Fase 5
 * (prórroga), la Fase 6 (suspensión y reanudación) y la Fase 8 (cancelación).
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
        subir(TRABAJOS + trabajoId + "/propuesta", docente, null, archivo("propuesta.pdf"))
                .andExpect(status().isOk());
        subir(TRABAJOS + trabajoId + "/propuesta/decisiones", login("C"),
                Map.of("resultado", "APROBADO", "acta", "Acta 01"), archivo("acta-01.pdf"))
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

        // El director radica el anteproyecto: queda en revisión esperando evaluadores
        subir(TRABAJOS + trabajoId + "/anteproyecto", docente, null, archivo("anteproyecto.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"))
                // El estado es de la versión: queda en revisión aunque aún no haya evaluadores
                .andExpect(jsonPath("$.anteproyecto.estado").doesNotExist())
                .andExpect(jsonPath("$.anteproyecto.versiones[0].estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.anteproyecto.versiones[0].tipo").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.anteproyecto.versiones[0].ruta")
                        .value(trabajoId + "/ANTEPROYECTO/v1/Anteproyecto - v1.pdf"));

        // El jefe de departamento ve el trabajo pendiente y asigna los evaluadores
        enviar(get(API + "/trabajos-grado").param("etapa", "ANTEPROYECTO"), jefe, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) trabajoId)));
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), jefe,
                Map.of("evaluador1Id", evaluador1Id, "evaluador2Id", evaluador2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"))
                .andExpect(jsonPath("$.anteproyecto.versiones[0].estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.anteproyecto.evaluadores[1].usuarioId").value(evaluador2Id))
                .andExpect(jsonPath("$.anteproyecto.fechaLimiteEvaluacion").isNotEmpty())
                .andExpect(jsonPath("$.anteproyecto.diasHabilesRestantesEvaluacion").value(10))
                .andExpect(jsonPath("$.anteproyecto.opcionesPlazo.length()").value(0))
                .andExpect(jsonPath("$.historial[1].responsable").value("J" + sufijo));

        // Pedir correcciones exige observaciones
        String evaluador1 = login("V");
        String evaluador2 = login("W");
        // El docente asignado como evaluador recibe el rol de contexto EVALUADOR
        enviar(get(API + "/usuarios/me"), evaluador2, null)
                .andExpect(jsonPath("$.roles", hasItem("EVALUADOR")));
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", evaluador2, Map.of("resultado", "CON_CORRECCIONES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Las observaciones son obligatorias cuando el concepto no es aprobatorio."));

        // Un evaluador aprueba con el formato B y el otro pide correcciones
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", evaluador1, Map.of("resultado", "APROBADO"),
                archivo("formato-b.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"))
                .andExpect(jsonPath("$.anteproyecto.conceptos[0].formato.formato").value("TI-B"))
                .andExpect(jsonPath("$.anteproyecto.conceptos[0].formato.ruta")
                        .value(trabajoId + "/TI-B/v1/Formato B - v1.pdf"))
                // El concepto se emitió dentro de los 10 días hábiles desde la asignación
                .andExpect(jsonPath("$.anteproyecto.conceptos[0].aTiempo").value(true))
                .andExpect(jsonPath("$.anteproyecto.conceptos[0].fechaLimite").isNotEmpty());
        // El formato B es obligatorio
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", evaluador2,
                Map.of("resultado", "CON_CORRECCIONES", "observaciones", "Ajustar el cronograma"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El concepto del evaluador debe ir acompañado del formato B."));
        // Con el formato B y, opcionalmente, el documento con sus anotaciones
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", evaluador2,
                Map.of("resultado", "CON_CORRECCIONES", "observaciones", "Ajustar el cronograma"),
                archivo("formato-b.pdf"), pdf("anotaciones", "notas.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anteproyecto.conceptos[1].anotaciones.ruta")
                        .value(trabajoId + "/ANOTACIONES_EVALUACION/v1/Anotaciones del evaluador - v1.pdf"))
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.anteproyecto.versiones[0].estado").value("CON_CORRECCIONES"))
                .andExpect(jsonPath("$.anteproyecto.fechaLimiteCorrecciones").isNotEmpty());

        // El director radica la versión corregida: vuelve directo a evaluación
        subir(TRABAJOS + trabajoId + "/anteproyecto", docente, null, archivo("anteproyecto_v2.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"))
                .andExpect(jsonPath("$.anteproyecto.versiones[0].estado").value("CON_CORRECCIONES"))
                .andExpect(jsonPath("$.anteproyecto.versiones[1].estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.anteproyecto.versiones[1].version").value(2))
                .andExpect(jsonPath("$.anteproyecto.versiones[1].ruta")
                        .value(trabajoId + "/ANTEPROYECTO/v2/Anteproyecto - v2.pdf"));

        // Ambos aprueban la versión 2: queda en el Departamento hasta que el jefe la remita
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", evaluador1, Map.of("resultado", "APROBADO"),
                archivo("formato-b.pdf"))
                .andExpect(status().isOk());
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", evaluador2, Map.of("resultado", "APROBADO"),
                archivo("formato-b.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"))
                .andExpect(jsonPath("$.anteproyecto.versiones[1].estado").value("APROBADO"))
                .andExpect(jsonPath("$.anteproyecto.conceptos.length()").value(4));

        // Solo el jefe de departamento remite al Consejo, y con el formato C
        subir(TRABAJOS + trabajoId + "/anteproyecto/remision", docente, null, archivo("formato-c.pdf"))
                .andExpect(status().isForbidden());
        subir(TRABAJOS + trabajoId + "/anteproyecto/remision", jefe, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La remisión al Consejo de Facultad requiere el formato C."));
        subir(TRABAJOS + trabajoId + "/anteproyecto/remision", jefe, null, archivo("formato-c.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("APROBACION_CONSEJO"))
                .andExpect(jsonPath("$.anteproyecto.remisiones[0].formato").value("TI-C"))
                .andExpect(jsonPath("$.anteproyecto.remisiones[0].version").value(2))
                .andExpect(jsonPath("$.anteproyecto.remisiones[0].ruta").value(trabajoId + "/TI-C/v1/Formato C - v1.pdf"))
                // En la aprobación del Consejo, la respuesta trae los plazos que puede fijar y su fecha de fin
                .andExpect(jsonPath("$.anteproyecto.opcionesPlazo.length()").value(2))
                .andExpect(jsonPath("$.anteproyecto.opcionesPlazo[0].meses").value(6))
                .andExpect(jsonPath("$.anteproyecto.opcionesPlazo[1].meses").value(9))
                .andExpect(jsonPath("$.anteproyecto.opcionesPlazo[1].fechaFin")
                        .value(java.time.LocalDate.now().plusMonths(9).toString()))
                .andExpect(jsonPath("$.anteproyecto.diasHabilesRestantesEvaluacion").doesNotExist());

        // El evaluador ve el trabajo entre los suyos
        enviar(get(API + "/trabajos-grado/mis-trabajos"), evaluador1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(trabajoId));
    }

    /** Ambos evaluadores aprueban la versión actual y el jefe la remite al Consejo con el formato C. */
    private void evaluadoresAprueban(long trabajoId) throws Exception {
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", login("V"), Map.of("resultado", "APROBADO"),
                archivo("formato-b.pdf"))
                .andExpect(status().isOk());
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", login("W"), Map.of("resultado", "APROBADO"),
                archivo("formato-b.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"));
        subir(TRABAJOS + trabajoId + "/anteproyecto/remision", login("J"), null, archivo("formato-c.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("APROBACION_CONSEJO"));
    }

    @Test
    void elConsejoDecideSobreElAnteproyecto() throws Exception {
        long trabajoId = trabajoEnAnteproyecto();
        long evaluador1Id = crearUsuario("V", "EVALUADOR");
        long evaluador2Id = crearUsuario("W", "EVALUADOR");
        crearUsuario("F", "CONSEJO");
        String docente = login("D");
        subir(TRABAJOS + trabajoId + "/anteproyecto", docente, null, archivo("anteproyecto.pdf"))
                .andExpect(status().isOk());
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), login("J"),
                Map.of("evaluador1Id", evaluador1Id, "evaluador2Id", evaluador2Id)).andExpect(status().isOk());
        evaluadoresAprueban(trabajoId);
        String consejo = login("F");
        String decisiones = TRABAJOS + trabajoId + "/anteproyecto/decisiones-consejo";

        // Solo el rol CONSEJO decide y el archivo de la resolución es obligatorio
        subir(decisiones, docente, Map.of("resultado", "APROBADO"), archivo("1.pdf"))
                .andExpect(status().isForbidden());
        subir(decisiones, consejo, Map.of("resultado", "APROBADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El documento de la resolución del Consejo de Facultad es obligatorio."));

        // No aprueba, sin comentario: vuelve a anteproyecto
        subir(decisiones, consejo, Map.of("resultado", "NO_APROBADO"), archivo("res-correcciones.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.anteproyecto.versiones[0].estado").value("NO_APROBADO"))
                .andExpect(jsonPath("$.anteproyecto.decisionesConsejo[0].organo").value("CONSEJO_FACULTAD"))
                .andExpect(jsonPath("$.anteproyecto.decisionesConsejo[0].rutaArchivo")
                        .value(trabajoId + "/RESOLUCION/v1/Resolucion - v1.pdf"))
                .andExpect(jsonPath("$.ejecucion").isEmpty());

        // La nueva versión vuelve a los evaluadores
        subir(TRABAJOS + trabajoId + "/anteproyecto", docente, null, archivo("anteproyecto_v2.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EVALUACION"));
        evaluadoresAprueban(trabajoId);

        // El Consejo aprueba: pasa a en curso con el plazo de investigación
        subir(decisiones, consejo, Map.of("resultado", "APROBADO", "observaciones", "Aprobado en sesión ordinaria"),
                archivo("res-aprobacion.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El Consejo debe fijar el plazo de ejecución: 6 o 9 meses en "
                        + "trabajo de investigación; 6, 9 o 12 meses en práctica profesional."));
        subir(decisiones, consejo, Map.of("resultado", "APROBADO", "observaciones", "Aprobado en sesión ordinaria",
                "mesesEjecucion", 6), archivo("res-aprobacion.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EN_CURSO"))
                .andExpect(jsonPath("$.anteproyecto.decisionesConsejo.length()").value(2))
                .andExpect(jsonPath("$.anteproyecto.decisionesConsejo[1].rutaArchivo")
                        .value(trabajoId + "/RESOLUCION/v2/Resolucion - v2.pdf"))
                .andExpect(jsonPath("$.ejecucion.fechaNotificacion").isNotEmpty())
                .andExpect(jsonPath("$.ejecucion.fechaFin").value(java.time.LocalDate.now().plusMonths(6).toString()))
                .andExpect(jsonPath("$.ejecucion.fechaMinimaSustentacion").isNotEmpty())
                .andExpect(jsonPath("$.anteproyecto.opcionesPlazo.length()").value(0))
                .andExpect(jsonPath("$.historial[-1:].destino", hasItem("EN_CURSO")));

        // Ya no admite otra decisión (el archivo que se alcanzó a guardar se elimina)
        subir(decisiones, consejo, Map.of("resultado", "APROBADO"), archivo("2.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La operación no está permitida en la etapa actual del trabajo de grado."));

        // Fase 4: el director solicita el cambio de título y el Consejo lo aprueba
        String cambio = TRABAJOS + trabajoId + "/cambio-objetivos";
        enviar(post(cambio), login("E"), Map.of("titulo", "T", "objetivos", List.of("O"), "justificacion", "J"))
                .andExpect(status().isForbidden());
        enviar(post(cambio), docente, Map.of("titulo", "Sistema de gestión y sustentaciones",
                "objetivos", List.of("Objetivo general"), "justificacion", "Se amplía el alcance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejecucion.cambiosObjetivos[0].tituloAnterior").value("Sistema de gestión"))
                .andExpect(jsonPath("$.ejecucion.cambiosObjetivos[0].decision").isEmpty())
                .andExpect(jsonPath("$.ejecucion.fechaLimiteCambioObjetivos").isNotEmpty());
        subir(cambio + "/decisiones-consejo", docente, Map.of("resultado", "APROBADO"), archivo("3.pdf"))
                .andExpect(status().isForbidden());
        subir(cambio + "/decisiones-consejo", consejo, Map.of("resultado", "APROBADO"), archivo("res-cambio.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Sistema de gestión y sustentaciones"))
                .andExpect(jsonPath("$.etapa").value("EN_CURSO"))
                .andExpect(jsonPath("$.ejecucion.cambiosObjetivos[0].decision.resultado").value("APROBADO"))
                // La iteración del archivo rechazado quedó libre
                .andExpect(jsonPath("$.ejecucion.cambiosObjetivos[0].decision.rutaArchivo")
                        .value(trabajoId + "/RESOLUCION/v3/Resolucion - v3.pdf"));
        // Solo una vez
        enviar(post(cambio), docente, Map.of("titulo", "Otro", "objetivos", List.of("O"), "justificacion", "J"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("El título u objetivos del trabajo de grado solo pueden cambiarse una vez."));

        // En investigación los informes mensuales los radica el estudiante, no el director
        subir(TRABAJOS + trabajoId + "/informes-mensuales", docente, null, archivo("abril.pdf"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "Los informes mensuales los radican los estudiantes del trabajo")));

        // Fase 5: el director solicita 2 meses de prórroga y el Consejo la aprueba
        String prorroga = TRABAJOS + trabajoId + "/prorroga";
        subir(prorroga, docente, Map.of("meses", 4), archivo("p.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meses").value("La prórroga debe ser de 1 a 3 meses"));
        String fechaFin = JsonPath.read(subir(prorroga, docente, Map.of("meses", 2), archivo("prorroga.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejecucion.prorroga.meses").value(2))
                .andExpect(jsonPath("$.ejecucion.prorroga.decision").isEmpty())
                .andReturn().getResponse().getContentAsString(), "$.ejecucion.fechaFin");
        subir(prorroga + "/decisiones-consejo", consejo, Map.of("resultado", "APROBADO"), archivo("res-prorroga.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("PRORROGA"))
                .andExpect(jsonPath("$.ejecucion.prorroga.decision.resultado").value("APROBADO"))
                .andExpect(jsonPath("$.ejecucion.fechaFin")
                        .value(java.time.LocalDate.parse(fechaFin).plusMonths(2).toString()));
        // Solo una vez
        subir(prorroga, docente, Map.of("meses", 1), archivo("p2.pdf"))
                .andExpect(status().isBadRequest());

        // Fase 6: suspensión durante la prórroga; al reanudar vuelve a PRORROGA
        String suspension = TRABAJOS + trabajoId + "/suspension";
        subir(suspension, docente, null, archivo("suspension.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("PRORROGA"))
                .andExpect(jsonPath("$.ejecucion.suspensiones[0].decision").isEmpty());
        subir(suspension + "/decisiones-consejo", consejo, Map.of("resultado", "APROBADO"),
                archivo("res-suspension.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("SUSPENDIDO"))
                .andExpect(jsonPath("$.ejecucion.suspensiones[0].etapaPrevia").value("PRORROGA"))
                .andExpect(jsonPath("$.ejecucion.suspensiones[0].fechaMaximaReanudacion").isNotEmpty())
                .andExpect(jsonPath("$.ejecucion.suspensiones[0].diasRestantes").isNumber());
        subir(suspension + "/reanudacion", login("E"), null, archivo("r.pdf"))
                .andExpect(status().isForbidden());
        subir(suspension + "/reanudacion", docente, null, archivo("reanudacion.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("PRORROGA"))
                .andExpect(jsonPath("$.ejecucion.suspensiones[0].reanudacionAutomatica").value(false));
        // Solo una vez
        subir(suspension, docente, null, archivo("s2.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El trabajo de grado solo puede suspenderse una vez."));
    }

    @Test
    void elDirectorSolicitaLaCancelacionYElConsejoLaDecide() throws Exception {
        long trabajoId = trabajoEnAnteproyecto();
        crearUsuario("F", "CONSEJO");
        String docente = login("D");
        String consejo = login("F");
        String cancelacion = TRABAJOS + trabajoId + "/cancelacion";

        subir(cancelacion, login("E"), null, archivo("c.pdf"))
                .andExpect(status().isForbidden());
        subir(cancelacion, docente, null)
                .andExpect(status().isBadRequest());
        subir(cancelacion, docente, null, archivo("cancelacion-1.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelaciones[0].etapaAlSolicitar").value("ANTEPROYECTO"))
                .andExpect(jsonPath("$.cancelaciones[0].decision").isEmpty());
        subir(cancelacion, docente, null, archivo("otra.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Ya hay una solicitud de cancelación pendiente de decisión del Consejo de Facultad."));

        // Mientras está pendiente el trabajo sigue: el director radica el anteproyecto
        subir(TRABAJOS + trabajoId + "/anteproyecto", docente, null, archivo("anteproyecto.pdf"))
                .andExpect(status().isOk());

        // El Consejo no la aprueba; se vuelve a solicitar y la aprueba
        subir(cancelacion + "/decisiones-consejo", docente, Map.of("resultado", "APROBADO"), archivo("x.pdf"))
                .andExpect(status().isForbidden());
        subir(cancelacion + "/decisiones-consejo", consejo, Map.of("resultado", "NO_APROBADO"),
                archivo("res-negada.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("ANTEPROYECTO"));
        subir(cancelacion, docente, null, archivo("cancelacion-2.pdf"))
                .andExpect(status().isOk());
        subir(cancelacion + "/decisiones-consejo", consejo, Map.of("resultado", "APROBADO"),
                archivo("res-cancelacion.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCierre").value("CANCELACION_DIRECTOR"))
                .andExpect(jsonPath("$.cancelaciones.length()").value(2));
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
        subir(TRABAJOS + trabajoId + "/anteproyecto", login("E"), null, archivo("anteproyecto.pdf"))
                .andExpect(status().isForbidden());
        subir(TRABAJOS + trabajoId + "/anteproyecto", login("D"), null, archivo("anteproyecto.pdf"))
                .andExpect(status().isOk());

        // Solo el rol JEFE_DEP asigna, y solo el del departamento del director
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), login("X"), evaluadores)
                .andExpect(status().isForbidden());
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), login("O"), evaluadores)
                .andExpect(status().isForbidden())
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
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", login("X"), Map.of("resultado", "APROBADO"))
                .andExpect(status().isForbidden());
        subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", login("V"),
                Map.of("resultado", "RECHAZADO", "observaciones", "No cumple"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El concepto de un evaluador sobre el anteproyecto solo puede ser APROBADO o CON_CORRECCIONES."));

        // La consulta por etapa es solo para roles de consulta general y exige una etapa válida
        enviar(get(API + "/trabajos-grado").param("etapa", "ANTEPROYECTO"), login("E"), null)
                .andExpect(status().isForbidden());
        enviar(get(API + "/trabajos-grado").param("etapa", "NO_EXISTE"), jefe, null)
                .andExpect(status().isBadRequest());
        // Sin etapa lista todos los trabajos
        enviar(get(API + "/trabajos-grado"), jefe, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) trabajoId)));
    }
}
