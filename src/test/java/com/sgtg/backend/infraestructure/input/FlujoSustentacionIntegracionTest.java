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
 * curso solicita la sustentación (monografía, formato E, paz y salvo y
 * anexos), el Consejo la programa con un jurado coordinador y el formato E, el
 * director pide una nueva fecha que el Consejo aprueba, el coordinador la
 * califica con observaciones, el director radica los ajustes, el coordinador
 * los verifica y carga la versión definitiva. Usa un reloj ajustable para
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
        long comiteId = crearUsuario("C", "COMITE");
        long jefeId = crearUsuario("J", "JEFE_DEP");
        long evaluador1Id = crearUsuario("V", "EVALUADOR");
        perfilDocente(evaluador1Id);
        long evaluador2Id = crearUsuario("W", "DOCENTE");
        perfilDocente(evaluador2Id);
        crearUsuario("F", "CONSEJO");
        crearUsuario("K", "DECANATURA");
        perfilDocente(docenteId);
        perfilDocente(comiteId);
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
        subir(TRABAJOS + trabajoId + "/propuesta/decisiones", login("C"), Map.of("resultado", "APROBADO"),
                archivo("acta.pdf"))
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
        subir(TRABAJOS + trabajoId + "/anteproyecto/decisiones-consejo", login("F"), Map.of("resultado", "APROBADO",
                "mesesEjecucion", 9),
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
        programaId = id(enviar(post(API + "/catalogos/programas"), admin,
                Map.of("nombre", "Programa " + sufijo, "departamentoId", departamentoId))
                .andExpect(status().isCreated()), "$.id");
    }

    // ----- Pruebas -----

    /** Partes de la solicitud de sustentación (sin anexos). */
    private static MockMultipartFile[] solicitud() {
        return new MockMultipartFile[] { pdf("monografia", "monografia.pdf"), pdf("documentoFinal", "tg.pdf") };
    }

    private static Map<String, Object> programacion(long coordinadorId, long juradoId, String fechaHora,
            String salon) {
        return Map.of("juradoCoordinadorId", coordinadorId, "juradoId", juradoId, "fechaHora", fechaHora, "salon",
                salon);
    }

    @Test
    void flujoCompletoDeLaSustentacion() throws Exception {
        long trabajoId = trabajoEnCurso();
        long coordinadorId = crearUsuario("X", "JURADO");
        perfilDocente(coordinadorId);
        long juradoId = crearUsuario("Y", "DOCENTE");
        perfilDocente(juradoId);
        long evaluadorId = id(enviar(get(TRABAJOS + trabajoId), login("F"), null), "$.anteproyecto.evaluadores[0].usuarioId");
        String docente = login("D");
        String consejo = login("F");
        String sustentacion = TRABAJOS + trabajoId + "/sustentacion";

        // Antes de los 3 meses no se puede solicitar (los archivos que se alcanzaron a guardar se eliminan)
        subir(sustentacion, docente, null, solicitud())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La sustentación solo puede solicitarse después de los meses mínimos de ejecución de la modalidad."));

        // 6 de julio: el director la solicita con monografía, formato E y anexos (sin paz y salvo)
        reloj.fijar(LocalDate.of(2026, 7, 6));
        subir(sustentacion, login("E"), null, solicitud()).andExpect(status().isForbidden());
        subir(sustentacion, docente, null, pdf("documentoFinal", "tg.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La monografía es obligatoria."));
        subir(sustentacion, docente, null, pdf("monografia", "monografia.pdf"), pdf("documentoFinal", "tg.pdf"),
                pdf("anexos", "anexos.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("SUSTENTACION"))
                .andExpect(jsonPath("$.sustentacion.estado").value("SOLICITADA"))
                .andExpect(jsonPath("$.sustentacion.rutaMonografia").value(trabajoId + "/MONOGRAFIA/v1/Monografia - v1.pdf"))
                .andExpect(jsonPath("$.sustentacion.rutaDocumentoFinal").value(trabajoId + "/TI-E/v1/Formato E - v1.pdf"))
                .andExpect(jsonPath("$.sustentacion.rutaPazYSalvo").doesNotExist())
                .andExpect(jsonPath("$.sustentacion.rutaAnexos").value(trabajoId + "/ANEXOS/v1/Anexos - v1.pdf"));

        // Decanatura revisa el paz y salvo: siempre con el formato F; si no lo aprueba, con observaciones
        String decanatura = login("K");
        String pazYSalvo = sustentacion + "/paz-y-salvo";
        Map<String, Object> programar = programacion(coordinadorId, juradoId, "2026-07-15T14:00:00", "Salón 301");
        subir(pazYSalvo, docente, Map.of("aprobado", true), archivo("f.pdf")).andExpect(status().isForbidden());
        subir(pazYSalvo, decanatura, Map.of("aprobado", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El paz y salvo (formato F) es obligatorio, tanto si se aprueba como si no."));
        subir(pazYSalvo, decanatura, Map.of("aprobado", false), archivo("f.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Si el paz y salvo no se aprueba, las observaciones son obligatorias."));
        subir(pazYSalvo, decanatura, Map.of("aprobado", false, "observaciones", "Deuda en biblioteca"),
                archivo("f-1.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.revisionesPazYSalvo[0].aprobado").value(false))
                .andExpect(jsonPath("$.sustentacion.solicitudNoAprobada").value(true))
                .andExpect(jsonPath("$.sustentacion.rutaPazYSalvo").doesNotExist());
        // Sin paz y salvo aprobado el Consejo no programa
        subir(sustentacion + "/programacion", consejo, programar, archivo("formato-e.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "La sustentación no se puede programar hasta que Decanatura apruebe el paz y salvo."));
        // La solicitud no sigue: Decanatura no la vuelve a revisar y el director radica una nueva
        subir(pazYSalvo, decanatura, Map.of("aprobado", true), archivo("paz-y-salvo.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El paz y salvo de esta solicitud no fue aprobado: el director debe radicar una nueva solicitud de sustentación."));
        subir(sustentacion, docente, null, pdf("monografia", "monografia-2.pdf"), pdf("documentoFinal", "tg-2.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("SUSTENTACION"))
                .andExpect(jsonPath("$.sustentacion.solicitudNoAprobada").value(false))
                .andExpect(jsonPath("$.sustentacion.solicitudesAnteriores[0].rutaDocumentoFinal")
                        .value(trabajoId + "/TI-E/v1/Formato E - v1.pdf"))
                .andExpect(jsonPath("$.sustentacion.rutaDocumentoFinal").value(trabajoId + "/TI-E/v2/Formato E - v2.pdf"));
        subir(pazYSalvo, decanatura, Map.of("aprobado", true), archivo("paz-y-salvo.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.revisionesPazYSalvo[1].aprobado").value(true))
                .andExpect(jsonPath("$.sustentacion.rutaPazYSalvo").value(trabajoId + "/TI-F/v2/Formato F - v2.pdf"));
        // Aprobado no se vuelve a revisar
        subir(pazYSalvo, decanatura, Map.of("aprobado", true), archivo("otro.pdf")).andExpect(status().isBadRequest());

        // El Consejo programa con el formato E: un evaluador del anteproyecto no puede ser jurado del mismo trabajo
        Map<String, Object> datos = programacion(coordinadorId, juradoId, "2026-07-15T14:00:00", "Salón 301");
        subir(sustentacion + "/programacion", docente, datos, archivo("formato-e.pdf"))
                .andExpect(status().isForbidden());
        subir(sustentacion + "/programacion", consejo,
                programacion(coordinadorId, evaluadorId, "2026-07-15T14:00:00", "Salón 301"), archivo("formato-e.pdf"))
                .andExpect(status().isBadRequest());
        subir(sustentacion + "/programacion", consejo, datos)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El formato E es obligatorio para programar la sustentación."));
        subir(sustentacion + "/programacion", consejo, datos, archivo("formato-e.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.sustentacion.sesiones[0].salon").value("Salón 301"))
                .andExpect(jsonPath("$.sustentacion.sesiones[0].juradoCoordinadorId").value(coordinadorId))
                .andExpect(jsonPath("$.sustentacion.sesiones[0].rutaFormatoE").value(trabajoId + "/TI-E/v3/Formato E - v3.pdf"));
        // Los jurados reciben sus roles
        enviar(get(API + "/usuarios/me"), login("X"), null)
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItems("JURADO", "JURADO_COORDINADOR")));
        enviar(get(API + "/usuarios/me"), login("Y"), null)
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem("JURADO")));

        // 10 de julio: el director pide una nueva fecha y el Consejo la aprueba (21 de julio)
        reloj.fijar(LocalDate.of(2026, 7, 10));
        subir(sustentacion + "/nueva-fecha", docente, null, archivo("nueva-fecha.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.solicitudesNuevaFecha[0].fechaHoraAnterior")
                        .value("2026-07-15T14:00:00"));
        subir(sustentacion + "/nueva-fecha/decisiones-consejo", consejo,
                Map.of("resultado", "APROBADO", "fechaHora", "2026-07-21T09:00:00"), archivo("res-nueva-fecha.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.sesiones[0].fechaHora").value("2026-07-21T09:00:00"))
                .andExpect(jsonPath("$.sustentacion.solicitudesNuevaFecha[0].decision.resultado").value("APROBADO"));

        // 21 de julio: solo el coordinador califica, con el acta
        reloj.fijar(LocalDate.of(2026, 7, 21));
        String coordinador = login("X");
        String jurado = login("Y");
        String calificaciones = sustentacion + "/calificaciones";
        Map<String, Object> conObservaciones = Map.of("calificacion", "APROBADO_CON_OBSERVACIONES", "observaciones",
                "Ajustar conclusiones");
        subir(calificaciones, docente, conObservaciones, archivo("acta.pdf")).andExpect(status().isForbidden());
        subir(calificaciones, jurado, conObservaciones, archivo("acta.pdf")).andExpect(status().isForbidden());
        subir(calificaciones, coordinador, conObservaciones)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El acta de sustentación (formato G) es obligatoria."));
        subir(calificaciones, coordinador, conObservaciones, archivo("sesion-1.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("EN_AJUSTES"))
                .andExpect(jsonPath("$.sustentacion.calificaciones[0].rutaActa")
                        .value(trabajoId + "/TI-G/v1/Formato G - v1.pdf"))
                .andExpect(jsonPath("$.sustentacion.fechaLimiteAjustes").isNotEmpty());

        // El director radica los ajustes con anexos y el coordinador los verifica
        reloj.fijar(LocalDate.of(2026, 7, 24));
        subir(sustentacion + "/ajustes", docente, null, archivo("tg.pdf"), pdf("anexos", "anexos-ajustes.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("AJUSTES_RADICADOS"))
                .andExpect(jsonPath("$.sustentacion.rutaAjustes").value(trabajoId + "/AJUSTES_SUSTENTACION/v1/Ajustes de la sustentacion - v1.pdf"))
                .andExpect(jsonPath("$.sustentacion.rutaAnexosAjustes")
                        .value(trabajoId + "/ANEXOS/v2/Anexos - v2.pdf"));
        enviar(post(sustentacion + "/ajustes/verificaciones"), jurado, Map.of("cumple", true))
                .andExpect(status().isForbidden());
        enviar(post(sustentacion + "/ajustes/verificaciones"), coordinador, Map.of("cumple", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sustentacion.estado").value("APROBADA"));

        // El coordinador carga la versión definitiva: el trabajo finaliza
        subir(sustentacion + "/version-definitiva", jurado, null, archivo("tg.pdf"))
                .andExpect(status().isForbidden());
        subir(sustentacion + "/version-definitiva", coordinador, null, archivo("tg.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("FINALIZADO"))
                .andExpect(jsonPath("$.sustentacion.rutaVersionDefinitiva").value(trabajoId + "/TI-H/v1/Formato H - v1.pdf"));

        // El otro jurado ve el trabajo entre los suyos y descarga sus archivos
        enviar(get(API + "/trabajos-grado/mis-trabajos"), jurado, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(trabajoId));
        enviar(get(TRABAJOS + trabajoId + "/archivos").param("ruta", trabajoId + "/TI-E/v1/Formato E - v1.pdf"), jurado, null)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void laInasistenciaAplazaYLaSegundaCierraElTrabajo() throws Exception {
        long trabajoId = trabajoEnCurso();
        long coordinadorId = crearUsuario("X", "JURADO");
        perfilDocente(coordinadorId);
        long juradoId = crearUsuario("Y", "DOCENTE");
        perfilDocente(juradoId);
        String consejo = login("F");
        String sustentacion = TRABAJOS + trabajoId + "/sustentacion";
        reloj.fijar(LocalDate.of(2026, 7, 6));
        subir(sustentacion, login("D"), null, solicitud()).andExpect(status().isOk());
        subir(sustentacion + "/paz-y-salvo", login("K"), Map.of("aprobado", true), archivo("f.pdf"))
                .andExpect(status().isOk());
        subir(sustentacion + "/programacion", consejo,
                programacion(coordinadorId, juradoId, "2026-07-06T16:00:00", "Salón 301"), archivo("e.pdf"))
                .andExpect(status().isOk());

        // El otro jurado no registra la inasistencia; el coordinador sí
        enviar(post(sustentacion + "/inasistencia"), login("Y"), Map.of("observaciones", "No se presentó"))
                .andExpect(status().isForbidden());
        enviar(post(sustentacion + "/inasistencia"), login("X"), Map.of("observaciones", "No se presentó"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("APLAZADO"))
                .andExpect(jsonPath("$.sustentacion.fechaLimiteSegundaSesion").value("2026-09-04"));

        // Segunda sesión fuera de los 60 días: no se permite; dentro sí, con el coordinador Y
        subir(sustentacion + "/programacion", consejo,
                programacion(juradoId, coordinadorId, "2026-09-05T16:00:00", "Salón 205"), archivo("e.pdf"))
                .andExpect(status().isBadRequest());
        subir(sustentacion + "/programacion", consejo,
                programacion(juradoId, coordinadorId, "2026-08-03T16:00:00", "Salón 205"), archivo("e.pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("SUSTENTACION"))
                .andExpect(jsonPath("$.sustentacion.sesiones[1].juradoCoordinadorId").value(juradoId));

        reloj.fijar(LocalDate.of(2026, 8, 3));
        enviar(post(sustentacion + "/inasistencia"), login("Y"), Map.of("observaciones", "No se presentó de nuevo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("NO_APROBADO"))
                .andExpect(jsonPath("$.motivoCierre").value("INASISTENCIA_SUSTENTACION"));
    }
}
