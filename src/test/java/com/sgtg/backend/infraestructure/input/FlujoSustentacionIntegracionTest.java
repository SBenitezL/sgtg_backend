package com.sgtg.backend.infraestructure.input;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
 * Prueba de punta a punta por HTTP de la Fase 7 (H2, JWT real): un trabajo en
 * curso solicita la sustentación, el Consejo la programa, los jurados la
 * califican con observaciones, el director radica los ajustes, los jurados los
 * verifican y cargan la versión definitiva. Usa un reloj ajustable para
 * avanzar los meses mínimos de ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = { "sgtg.admin.email=admin@sgtg.test", "sgtg.admin.password=Admin123*" })
class FlujoSustentacionIntegracionTest {

    private static final String API = "/api/v1/sgtg";
    private static final String TRABAJOS = API + "/trabajos-grado/";
    private static final String CLAVE = "Clave123*";
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    /** Lunes 2 de marzo de 2026 */
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 2);

    /** Reloj que la prueba puede mover; reemplaza al de la aplicación. */
    static class RelojAjustable extends Clock {
        private Instant instante;

        void fijar(LocalDate fecha) {
            this.instante = fecha.atTime(10, 0).atZone(BOGOTA).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return BOGOTA;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return Clock.fixed(this.instante, zona);
        }

        @Override
        public Instant instant() {
            return this.instante;
        }
    }

    @TestConfiguration
    static class ConfiguracionReloj {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            RelojAjustable reloj = new RelojAjustable();
            reloj.fijar(INICIO);
            return reloj;
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private RelojAjustable reloj;

    private String admin;
    private long departamentoId;
    private long programaId;
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

    private void perfilDocente(long usuarioId) throws Exception {
        enviar(put(API + "/perfiles/docentes/" + usuarioId), admin,
                Map.of("tipoVinculacion", "PLANTA", "departamentoId", departamentoId)).andExpect(status().isOk());
    }

    /**
     * Lleva un trabajo de investigación hasta EN_CURSO (notificado en la fecha
     * del reloj): propuesta, anteproyecto, evaluadores (V, W) y Consejo (F).
     */
    private long trabajoEnCurso() throws Exception {
        long docenteId = crearUsuario("D", "DOCENTE");
        long estudianteId = crearUsuario("E", "ESTUDIANTE");
        crearUsuario("C", "COMITE");
        long jefeId = crearUsuario("J", "JEFE_DEP");
        long evaluador1Id = crearUsuario("V", "EVALUADOR");
        long evaluador2Id = crearUsuario("W", "DOCENTE");
        crearUsuario("F", "CONSEJO");
        perfilDocente(docenteId);
        perfilDocente(jefeId);
        enviar(put(API + "/perfiles/estudiantes/" + estudianteId), admin, Map.of("programaId", programaId))
                .andExpect(status().isOk());

        String docente = login("D");
        long trabajoId = id(enviar(post(API + "/trabajos-grado/investigacion"), docente,
                Map.of("titulo", "Sistema de gestión", "objetivos", List.of("Objetivo general"),
                        "estudiantesIds", List.of(estudianteId)))
                .andExpect(status().isCreated()), "$.id");
        subir(TRABAJOS + trabajoId + "/propuesta", docente, null, archivo("propuesta.pdf"))
                .andExpect(status().isOk());
        subir(TRABAJOS + trabajoId + "/propuesta/decisiones", login("C"), Map.of("resultado", "APROBADO"))
                .andExpect(status().isOk());
        subir(TRABAJOS + trabajoId + "/anteproyecto", docente, null, archivo("anteproyecto.pdf"))
                .andExpect(status().isOk());
        enviar(post(TRABAJOS + trabajoId + "/anteproyecto/evaluadores"), login("J"),
                Map.of("evaluador1Id", evaluador1Id, "evaluador2Id", evaluador2Id)).andExpect(status().isOk());
        for (String evaluador : List.of("V", "W"))
            subir(TRABAJOS + trabajoId + "/anteproyecto/conceptos", login(evaluador), Map.of("resultado", "APROBADO"),
                    archivo("formato-b.pdf"))
                    .andExpect(status().isOk());
        subir(TRABAJOS + trabajoId + "/anteproyecto/remision", login("J"), null, archivo("formato-c.pdf"))
                .andExpect(status().isOk());
        subir(TRABAJOS + trabajoId + "/anteproyecto/decisiones-consejo", login("F"), Map.of("resultado", "APROBADO"),
                archivo("res-aprobacion.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("EN_CURSO"))
                .andExpect(jsonPath("$.ejecucion.fechaNotificacion").value(INICIO.toString()));
        return trabajoId;
    }

    @BeforeEach
    void preparar() throws Exception {
        reloj.fijar(INICIO);
        sufijo = String.valueOf(System.nanoTime());
        documentos = 0;
        admin = login("admin@sgtg.test", "Admin123*");
        departamentoId = id(enviar(post(API + "/catalogos/departamentos"), admin, Map.of("nombre", "Depto " + sufijo))
                .andExpect(status().isCreated()), "$.id");
        programaId = id(enviar(post(API + "/catalogos/programas"), admin, Map.of("nombre", "Programa " + sufijo))
                .andExpect(status().isCreated()), "$.id");
    }

    // ----- Pruebas -----

    @Test
    void flujoCompletoDeLaSustentacion() throws Exception {
        long trabajoId = trabajoEnCurso();
        long jurado1Id = crearUsuario("X", "JURADO");
        long evaluadorId = id(enviar(get(TRABAJOS + trabajoId), login("F"), null), "$.anteproyecto.evaluadores[0].usuarioId");
        String docente = login("D");
        String consejo = login("F");
        String sustentacion = TRABAJOS + trabajoId + "/sustentacion";
        MockMultipartFile[] solicitud = { pdf("documentoFinal", "tg.pdf"), pdf("pazYSalvo", "paz-y-salvo.pdf") };

        // Antes de los 3 meses no se puede solicitar (los archivos que se alcanzaron a guardar se eliminan)
        subir(sustentacion, docente, null, solicitud)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La sustentación solo puede solicitarse después de los meses mínimos de ejecución de la modalidad."));

        // 6 de julio: el director la solicita
        reloj.fijar(LocalDate.of(2026, 7, 6));
        subir(sustentacion, login("E"), null, solicitud).andExpect(status().isNotAcceptable());
        subir(sustentacion, docente, null, solicitud)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("SUSTENTACION"))
                .andExpect(jsonPath("$.sustentacion.estado").value("SOLICITADA"))
                .andExpect(jsonPath("$.sustentacion.rutaDocumentoFinal").value(trabajoId + "/TI-E/v1/tg.pdf"))
                .andExpect(jsonPath("$.sustentacion.rutaPazYSalvo").value(trabajoId + "/TI-F/v1/paz-y-salvo.pdf"));

        // El Consejo programa: un jurado nuevo y uno de los evaluadores del anteproyecto
        Map<String, Object> programacion = Map.of("jurado1Id", jurado1Id, "jurado2Id", evaluadorId,
                "fechaHora", "2026-07-15T14:00:00", "salon", "Salón 301");
        enviar(post(sustentacion + "/programacion"), docente, programacion).andExpect(status().isForbidden());
        enviar(post(sustentacion + "/programacion"), consejo, programacion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.sustentacion.sesiones[0].salon").value("Salón 301"))
                .andExpect(jsonPath("$.sustentacion.sesiones[0].jurados[0].usuarioId").value(jurado1Id));

        // 15 de julio: los jurados califican; rige la menos favorable
        reloj.fijar(LocalDate.of(2026, 7, 15));
        String jurado1 = login("X");
        String jurado2 = login("V");
        String calificaciones = sustentacion + "/calificaciones";
        subir(calificaciones, docente, Map.of("calificacion", "APROBADO"))
                .andExpect(status().isNotAcceptable());
        subir(calificaciones, jurado1, Map.of("calificacion", "APROBADO"), archivo("sesion-1.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.calificaciones[0].rutaActa")
                        .value(trabajoId + "/TI-G/v1/sesion-1.pdf"));
        subir(calificaciones, jurado2,
                Map.of("calificacion", "APROBADO_CON_OBSERVACIONES", "observaciones", "Ajustar conclusiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("EN_AJUSTES"))
                .andExpect(jsonPath("$.sustentacion.fechaLimiteAjustes").isNotEmpty());

        // El director radica los ajustes y los jurados los verifican
        reloj.fijar(LocalDate.of(2026, 7, 20));
        subir(sustentacion + "/ajustes", docente, null, archivo("tg.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("AJUSTES_RADICADOS"))
                .andExpect(jsonPath("$.sustentacion.rutaAjustes").value(trabajoId + "/AJUSTES_SUSTENTACION/v1/tg.pdf"));
        enviar(post(sustentacion + "/ajustes/verificaciones"), jurado1, Map.of("cumple", true))
                .andExpect(status().isOk());
        enviar(post(sustentacion + "/ajustes/verificaciones"), jurado2, Map.of("cumple", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("APROBADA"));

        // Un jurado carga la versión definitiva: el trabajo finaliza
        subir(sustentacion + "/version-definitiva", docente, null, archivo("tg.pdf"))
                .andExpect(status().isNotAcceptable());
        subir(sustentacion + "/version-definitiva", jurado1, null, archivo("tg.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("FINALIZADO"))
                .andExpect(jsonPath("$.sustentacion.rutaVersionDefinitiva").value(trabajoId + "/TI-H/v1/tg.pdf"));

        // El jurado ve el trabajo entre los suyos y descarga sus archivos
        enviar(get(API + "/trabajos-grado/mis-trabajos"), jurado1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(trabajoId));
        enviar(get(TRABAJOS + trabajoId + "/archivos").param("ruta", trabajoId + "/TI-E/v1/tg.pdf"), jurado1, null)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void laInasistenciaAplazaYLaSegundaCierraElTrabajo() throws Exception {
        long trabajoId = trabajoEnCurso();
        long jurado1Id = crearUsuario("X", "JURADO");
        long jurado2Id = crearUsuario("Y", "DOCENTE");
        String consejo = login("F");
        String sustentacion = TRABAJOS + trabajoId + "/sustentacion";
        reloj.fijar(LocalDate.of(2026, 7, 6));
        subir(sustentacion, login("D"), null, pdf("documentoFinal", "tg.pdf"), pdf("pazYSalvo", "tg.pdf"))
                .andExpect(status().isOk());
        enviar(post(sustentacion + "/programacion"), consejo, Map.of("jurado1Id", jurado1Id, "jurado2Id", jurado2Id,
                "fechaHora", "2026-07-06T16:00:00", "salon", "Salón 301")).andExpect(status().isOk());

        enviar(post(sustentacion + "/inasistencia"), login("X"), Map.of("observaciones", "No se presentó"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("APLAZADO"))
                .andExpect(jsonPath("$.sustentacion.fechaLimiteSegundaSesion").value("2026-09-04"));

        // Segunda sesión fuera de los 60 días: no se permite
        enviar(post(sustentacion + "/programacion"), consejo, Map.of("jurado1Id", jurado1Id, "jurado2Id", jurado2Id,
                "fechaHora", "2026-09-05T16:00:00", "salon", "Salón 205"))
                .andExpect(status().isBadRequest());
        enviar(post(sustentacion + "/programacion"), consejo, Map.of("jurado1Id", jurado1Id, "jurado2Id", jurado2Id,
                "fechaHora", "2026-08-03T16:00:00", "salon", "Salón 205"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("SUSTENTACION"));

        reloj.fijar(LocalDate.of(2026, 8, 3));
        enviar(post(sustentacion + "/inasistencia"), login("Y"), Map.of("observaciones", "No se presentó de nuevo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("NO_APROBADO"))
                .andExpect(jsonPath("$.motivoCierre").value("INASISTENCIA_SUSTENTACION"));
    }
}
