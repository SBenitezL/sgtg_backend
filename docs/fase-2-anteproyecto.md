# Fase 2: Anteproyecto y evaluación

Este documento describe la Fase 2: clases del dominio, reglas, operaciones, la API que las expone y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md). Las piezas que se reutilizan están en [fase-0-base-comun.md](fase-0-base-comun.md) (`Concepto`, `Evaluador`, `Documento`, `Estado`, `Proceso`) y [fase-1-propuesta.md](fase-1-propuesta.md) (agregado `TrabajoGrado`).

Con la propuesta aprobada, el trabajo está en `ANTEPROYECTO`. Después:

1. El director radica el anteproyecto ante el Departamento.
2. El jefe de departamento asigna dos evaluadores.
3. Cada evaluador emite su concepto.

Si los dos aprueban, el trabajo pasa a `APROBACION_CONSEJO`. Si alguno pide correcciones, vuelve a `ANTEPROYECTO`, y el director radica una versión corregida que revisan los mismos evaluadores.

Artículos de la Resolución 231 de 2020 que cubre: **12, 13, 37 y 38**.

```
            radicar (1.ª vez)           asignarEvaluadores              2 conceptos APROBADO
ANTEPROYECTO ──────────────► ANTEPROYECTO ──────────────► EVALUACION ─────────────────────► APROBACION_CONSEJO
 (sin estado)                 (EN_REVISION)               (EN_REVISION)
                                                            │     ▲
                              algún CON_CORRECCIONES        │     │ radicar versión corregida
                                                            ▼     │ (mismos evaluadores)
                                                         ANTEPROYECTO
                                                       (CON_CORRECCIONES)
```

## Decisiones (confirmadas por el usuario)

El reglamento no define estos puntos. Se propusieron antes de iniciar la fase y el usuario las confirmó:

| # | Decisión |
|---|---|
| 1 | El rol `JEFE_DEP` asigna los evaluadores, y solo en trabajos cuyo director pertenece a **su** departamento. Ese departamento sale de su perfil docente; por eso `JEFE_DEP` ahora puede tener perfil docente. |
| 2 | Los evaluadores son usuarios con rol `DOCENTE`, `EVALUADOR` o `JURADO`, de cualquier departamento. No pueden ser director, codirector, **asesor** ni estudiantes del trabajo. |
| 3 | Cada evaluador emite `APROBADO` o `CON_CORRECCIONES`. El anteproyecto no se rechaza (Art. 13). |
| 4 | El formato B es opcional: cada evaluador puede adjuntar la URL del suyo con su concepto. |
| 5 | Un resultado mixto (uno aprueba y otro pide correcciones) cuenta como correcciones. |
| 6 | Las versiones corregidas las revisan **los mismos** evaluadores, sin reasignación. Ambos emiten un concepto nuevo. |
| 7 | Los plazos de 10 días hábiles (evaluadores y director) son **informativos**: no cambian la etapa. |
| 8 | Si ambos aprueban, el trabajo pasa **automáticamente** a `APROBACION_CONSEJO`. No hay un paso explícito de remisión con formato C. |

## Clases del dominio

```
domain/models/trabajo_grado/
├── Anteproyecto          (etapa dentro del agregado)
├── ConceptoEvaluacion    (record: concepto de un evaluador sobre una versión)
└── DatosAnteproyecto     (record: estado persistido, para reconstruir)
```

### `Anteproyecto`
Guarda las versiones radicadas, los evaluadores, sus conceptos y los plazos. Las operaciones que lo modifican son de paquete: solo `TrabajoGrado` las invoca.

| Atributo | Descripción |
|---|---|
| `versiones` | `Documento` de tipo `ANTEPROYECTO`, numerados desde 1 |
| `evaluadores` | Los 2 `Evaluador` asignados; vacío hasta la asignación |
| `conceptos` | `ConceptoEvaluacion` de todas las versiones, en orden |
| `versiones` | Versiones radicadas (`VersionDocumento`: documento + estado). Cada una conserva el resultado de su revisión: `EN_REVISION`, `CON_CORRECCIONES`, `APROBADO` o `NO_APROBADO` (2026-10-11) |
| `getEstado()` | Estado de la versión actual (derivado); `null` sin radicar |
| `fechaLimiteEvaluacion` | Radicación o asignación + 10 días hábiles |
| `fechaLimiteCorrecciones` | Fecha del concepto que cerró la ronda + 10 días hábiles; `null` cuando no hay correcciones pendientes |

Consultas públicas:
- `tieneEvaluadores()`
- `evaluador(usuarioId)`
- `estaAprobado()`
- `conceptosVersionActual()`
- `versionActual()`
- `evaluacionVencida(hoy)` y `correccionesVencidas(hoy)` (informativas)

Constantes: `DIAS_HABILES_EVALUACION = 10`, `DIAS_HABILES_CORRECCIONES = 10`, `CANTIDAD_EVALUADORES = 2`.

### `ConceptoEvaluacion (version, concepto, formato)`
Concepto de un evaluador sobre una versión.
- `concepto` es el `Concepto` de la Fase 0, cuyo emisor es el `Evaluador`. Exige observaciones si no es aprobatorio.
- `formato` es opcional y, si existe, debe ser un `Documento` de tipo `EVALUACION_ANTEPROYECTO` (formato B, código `TI-B` o `PP-B`). Su versión coincide con la del anteproyecto evaluado.
- `emitidoPor(usuarioId)` indica si lo emitió ese usuario.

### `DatosAnteproyecto`
Record con el estado persistido. `DatosTrabajoGrado` tiene un nuevo componente, `anteproyecto`, después de `propuesta`. Si es `null`, equivale a `DatosAnteproyecto.vacio()`.

## Operaciones del agregado

Las tres reciben `responsable` (o lo toman del emisor del concepto) y `ahora`, porque pueden cambiar de etapa y registrar un `CambioEtapa`. Todas validan **antes** de modificar el estado, para no dejar el agregado a medias.

### `radicarAnteproyecto(url, responsable, ahora, calendario)`
- Solo en la etapa `ANTEPROYECTO`.
- Se permite la primera vez o con el estado en `CON_CORRECCIONES`. No se permite si la versión actual sigue pendiente (`radicacion_no_permitida`).
- **Primera versión:** queda `EN_REVISION` y espera los evaluadores; el plazo de evaluación empieza al asignarlos. No cambia de etapa.
- **Versión corregida:** como ya hay evaluadores, queda `EN_REVISION`, se calcula un plazo nuevo para ellos y el trabajo pasa a `EVALUACION`.

### `asignarEvaluadores(evaluador1, evaluador2, responsable, ahora, calendario)`
- Solo en `ANTEPROYECTO`, con la primera versión `EN_REVISION` y sin evaluadores previos (`asignacion_no_permitida`). No cambia el estado de la versión, solo calcula el plazo de los evaluadores.
- El caso de uso agrega el rol de contexto `EVALUADOR` a los dos evaluadores (2026-10-11).
- Exige exactamente dos evaluadores (`evaluadores_obligatorios`), que sean personas distintas (`evaluadores_repetidos`).
- Ninguno puede ser director, codirector, asesor ni estudiante del trabajo (`evaluador_con_conflicto`).
- La fecha no puede ser anterior a la radicación (`fecha_invalida`).
- Resultado: `EN_REVISION`, plazo de 10 días hábiles, y el trabajo pasa a `EVALUACION`.

### `registrarConceptoEvaluador(concepto, rutaFormato, rutaAnotaciones, ahora, calendario)`
- Solo en `EVALUACION`.
- El **formato B es obligatorio** (`formato_b_obligatorio`); el documento con las anotaciones del evaluador
  es opcional (tipo `ANOTACIONES_EVALUACION`, parte multipart `anotaciones`). (2026-10-12)
- El emisor debe ser uno de los evaluadores asignados (`evaluador_no_asignado`).
- Solo se admite `APROBADO` o `CON_CORRECCIONES` (`resultado_invalido`).
- Cada evaluador emite un concepto por versión (`concepto_repetido`).
- La fecha del concepto no puede ser anterior a la radicación de la versión (`fecha_invalida`).
- Al registrarse el segundo concepto de la versión:
  - Ambos `APROBADO` → estado `APROBADO`; el trabajo **sigue en `EVALUACION`** hasta que el Jefe de
    departamento lo remite al Consejo con el formato C (ver abajo).
  - Alguno `CON_CORRECCIONES` → estado `CON_CORRECCIONES`, plazo de 10 días hábiles para el director, y el trabajo vuelve a `ANTEPROYECTO`.
- El responsable del `CambioEtapa` (correcciones) es el código del evaluador que cerró la ronda.

### `remitirAnteproyectoAlConsejo(rutaFormatoC, responsable, ahora)` (2026-10-12)
- Paso **obligatorio** antes de la decisión del Consejo: el Jefe del departamento del director remite la
  versión que ambos evaluadores aprobaron, con el **formato C** (`REMISION_CONSEJO`, carpeta `TI-C`/`PP-C`).
- Solo en `EVALUACION`, con la versión actual `APROBADO` y aún no remitida (`remision_no_permitida`); sin
  formato C, `formato_c_obligatorio`.
- El trabajo pasa a `APROBACION_CONSEJO`; el responsable del `CambioEtapa` es el jefe.
- Cada remisión se guarda en `Anteproyecto.remisiones` con el número de la versión remitida. Tras un NO
  APROBADO del Consejo, la nueva versión aprobada se remite de nuevo.
- Endpoint: `POST /trabajos-grado/{id}/anteproyecto/remision` (multipart, parte `archivo`, rol `JEFE_DEP`).

### Consultas nuevas en `TrabajoGrado`
- `getAnteproyecto()`
- `esEvaluador(usuarioId)`
- `participa(usuarioId)`, que ahora también cuenta a los evaluadores. Así pueden consultar el trabajo y verlo en "mis trabajos".

## Aplicación e infraestructura

### Casos de uso (`ManageTrabajoGradoCUImplAdapter`)
| Operación | Permiso que valida el caso de uso |
|---|---|
| `radicarAnteproyecto(trabajoId, usuarioId, url)` | Ser el director del trabajo (`solo_director`, 406) |
| `asignarEvaluadores(trabajoId, usuarioId, evaluador1Id, evaluador2Id)` | El jefe necesita perfil docente (`jefe_sin_perfil`, 400) en el mismo departamento que el director (`otro_departamento`, 406). Cada evaluador se crea desde su `Usuario`, lo que valida su rol |
| `registrarConceptoEvaluador(trabajoId, usuarioId, resultado, observaciones, urlFormato)` | Ser evaluador asignado (`solo_evaluador`, 406). La fecha del concepto es la del día (`Clock`) |
| `listarPorEtapa(etapa)` | La etapa es obligatoria; el controlador restringe por rol |

### Endpoints (`/api/v1/sgtg/trabajos-grado`)
| Método y ruta | Rol | Cuerpo |
|---|---|---|
| `GET ?etapa=ANTEPROYECTO` | `COMITE`, `JEFE_DEP`, `CONSEJO`, `ADMINISTRADOR` | — |
| `POST /{id}/anteproyecto` | (director, en el caso de uso) | `{ "url": "https://..." }` |
| `POST /{id}/anteproyecto/evaluadores` | `JEFE_DEP` | `{ "evaluador1Id": 5, "evaluador2Id": 6 }` |
| `POST /{id}/anteproyecto/conceptos` | (evaluador asignado, en el caso de uso) | `{ "resultado": "CON_CORRECCIONES", "observaciones": "...", "urlFormato": "https://..." }` |

La respuesta `TrabajoGradoDTOResponse` incluye un bloque nuevo, `anteproyecto`, con estos campos:
- `estado`, `fechaLimiteEvaluacion`, `fechaLimiteCorrecciones`
- `versiones`
- `evaluadores`
- `conceptos`: versión, evaluador, resultado, observaciones, fecha y formato B

Un parámetro `etapa` ausente o inválido responde 400. `RestApiException` ahora maneja `MethodArgumentTypeMismatchException` y `MissingServletRequestParameterException`.

### Persistencia
`TrabajoGradoEntity` tiene tres columnas nuevas: `anteproyectoEstado`, `anteproyectoFechaLimiteEvaluacion` y `anteproyectoFechaLimiteCorrecciones`. Además tiene tres colecciones de valores:

| Tabla | Embeddable | Contenido |
|---|---|---|
| `anteproyecto_versiones` | `DocumentoEmbeddable` | Versiones radicadas |
| `anteproyecto_evaluadores` | `EvaluadorTrabajoEmbeddable` | Copia de los datos del evaluador al asignarse |
| `anteproyecto_conceptos` | `ConceptoEvaluacionEmbeddable` | Versión, usuario del evaluador, resultado, observaciones, fecha y URL del formato B |

Al leer, `MapperTrabajoGrado` resuelve el emisor de cada concepto contra los evaluadores asignados por `usuarioId`. La consulta `findByParticipante` incluye a los evaluadores. Se agrega `findByEtapaOrderByIdDesc`.

## Plan de pruebas

```bash
./mvnw test -Dtest=EtapaAnteproyectoTest                      # dominio
./mvnw test -Dtest='ManageTrabajoGrado*Test'                  # caso de uso y persistencia
./mvnw test -Dtest=FlujoAnteproyectoIntegracionTest           # punta a punta por HTTP
```

`DatosPrueba` agrega `evaluador(id)`, `URL_ANTEPROYECTO` y `URL_FORMATO_B`.

### `EtapaAnteproyectoTest` (23 pruebas, dominio)
| Prueba | Qué verifica |
|---|---|
| `laPrimeraVersionQuedaEnRevisionEsperandoLosEvaluadores` | `EN_REVISION`, versión 1 de tipo `ANTEPROYECTO`, sin plazo (no vence) ni cambio de etapa |
| `noSeEmiteConceptoMientrasNoHayEvaluadoresAsignados` | En revisión pero sin evaluadores: el concepto se rechaza por etapa |
| `soloSeRadicaEnLaEtapaDeAnteproyecto` | En `PROPUESTA` responde `etapa_invalida` |
| `noSeRadicaOtraVersionMientrasLaActualEstePendiente` | `radicacion_no_permitida` |
| `asignarEvaluadoresPasaAEvaluacionYCalculaElPlazo` | `EVALUACION`, `EN_REVISION`, plazo de 10 días hábiles, historial, evaluadores como participantes |
| `noSeAsignanEvaluadoresSinAnteproyectoRadicado` | `asignacion_no_permitida` |
| `seExigenDosEvaluadoresDistintos` | Falta uno o se repite la persona; no modifica nada |
| `laAsignacionNoPuedeSerAnteriorALaRadicacion` | `fecha_invalida` |
| `unEvaluadorNoPuedeTenerOtroPapelEnElTrabajo` (×4) | Director, codirector, asesor y estudiante |
| `elPrimerConceptoNoCambiaLaEtapa` | Sigue en `EVALUACION`; guarda el formato B |
| `siAmbosApruebanPasaAAprobacionDelConsejo` | Etapa, estado y responsable del cambio |
| `conCorreccionesVuelveAAnteproyectoYLaNuevaVersionLaRevisanLosMismosEvaluadores` | Ciclo completo con 2 versiones, 4 conceptos y plazos exactos |
| `soloLosEvaluadoresAsignadosEmitenConceptoUnaVezPorVersion` | `evaluador_no_asignado` y `concepto_repetido` |
| `elEvaluadorSoloApruebaOPideCorrecciones` (×3) | `RECHAZADO`, `EVALUADO` y `NO_APROBADO` se rechazan |
| `elConceptoNoPuedeSerAnteriorALaRadicacion` | `fecha_invalida` |
| `unFormatoConUrlInvalidaNoModificaElAnteproyecto` | El agregado no queda a medias |
| `noSeEmiteConceptoFueraDeLaEtapaDeEvaluacion` | `etapa_invalida` |
| `losPlazosVencidosSonInformativos` | `evaluacionVencida` y `correccionesVencidas`, sin cambio de etapa |
| `elAnteproyectoReconstruidoSigueOperando` | Reconstrucción con `DatosAnteproyecto` y cierre de la ronda |

### Caso de uso, persistencia y HTTP
- **`ManageTrabajoGradoCUImplAdapterTest` (8 nuevas):**
  - Solo el director radica.
  - El jefe del departamento del director asigna; uno de otro departamento recibe 406, y un jefe sin perfil recibe 400.
  - Un evaluador sin rol válido es rechazado.
  - Solo los evaluadores asignados emiten concepto.
  - `listarPorEtapa` exige la etapa.
- **`ManageTrabajoGradoGatewayImplAdapterTest` (2 nuevas):**
  - Guarda y recupera el anteproyecto con evaluadores, conceptos, formato B y plazos, y el trabajo leído sigue operando.
  - `listarPorEtapa` funciona, y los evaluadores aparecen como participantes.
- **`FlujoAnteproyectoIntegracionTest` (2):**
  - Flujo completo por HTTP con una ronda de correcciones.
  - Permisos: director, rol `JEFE_DEP`, departamento, conflicto de evaluador, evaluador no asignado, resultado inválido y consulta por etapa.

Total del proyecto: **197 pruebas** (antes eran 163).

### Thunder Client
La colección de `thunder-tests/` tiene una carpeta nueva, **3. Flujo del anteproyecto**, con 12 peticiones. La carpeta de permisos pasa a ser la 4 y gana 5 pruebas. En total son 50 peticiones.

### Qué no se prueba en esta fase
- La aprobación del Consejo de Facultad: ver [fase-3-aprobacion-consejo.md](fase-3-aprobacion-consejo.md).
- Alertas o tareas programadas sobre los plazos vencidos: el dominio solo expone `evaluacionVencida` y `correccionesVencidas`.
