# Fase 3: Aprobación del Consejo de Facultad

Este documento describe la Fase 3: clases del dominio, reglas, operaciones, la API que las expone y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md). La etapa anterior, cuyos conceptos y versiones se reutilizan aquí, está en [fase-2-anteproyecto.md](fase-2-anteproyecto.md).

Cuando ambos evaluadores aprueban el anteproyecto, el trabajo queda en `APROBACION_CONSEJO`. El Consejo de Facultad emite su concepto y adjunta la resolución. La dinámica es la misma que la de los evaluadores:

- **Aprueba:** el trabajo pasa a `EN_CURSO`. La fecha de la decisión es la **fecha de notificación**, y desde ella corre el plazo de ejecución de la modalidad.
- **No aprueba:** el trabajo vuelve a `ANTEPROYECTO`. El director radica una nueva versión, que **vuelve a pasar por los mismos evaluadores**.

Artículo de la Resolución 231 de 2020 que cubre: **14** (y el plazo del Art. 15).

```
                         Consejo APROBADO (fecha = notificación)
APROBACION_CONSEJO ─────────────────────────────────────────────► EN_CURSO  (inicia la Ejecucion)
        │   ▲
        │   │ ambos evaluadores aprueban
        ▼   │
   ANTEPROYECTO ──radicar nueva versión──► EVALUACION
 (Consejo NO_APROBADO)                    (mismos evaluadores)
```

## Decisiones (confirmadas por el usuario)

| # | Decisión |
|---|---|
| 1 | El Consejo solo emite `APROBADO` o `NO_APROBADO`. |
| 2 | Si no aprueba, el trabajo vuelve a `ANTEPROYECTO` y la nueva versión pasa por los mismos evaluadores. |
| 3 | La fecha de notificación es la fecha en que se registra la resolución aprobatoria (la del día). Más adelante habrá un sistema de notificaciones. |
| 4 | Plazos de ejecución: investigación 9 meses; práctica profesional mínimo 6 meses y máximo 1 año. |
| 5 | La **ruta del documento de la resolución es obligatoria**: si no aprueba, el documento trae las correcciones; si aprueba, es el acta. Solo se guarda la ruta, como texto. |
| 6 | No se guarda el número de la resolución (`acta` queda en `null`). |
| 7 | El comentario es **opcional** incluso si no aprueba, porque la información puede venir en el documento. |

Hay una interpretación propia: en investigación, la fecha mínima para solicitar la sustentación es a los **3 meses**, la regla que el plan ya tenía para la Fase 7.

## Cambios en la base común

- **`Organo`:** tiene `exigeObservacionesSiNoAprueba()`. Es `true` para el Comité de Programa y el Departamento, y `false` para el Consejo de Facultad, que soporta sus decisiones en la resolución adjunta. `Decision` aplica la regla de observaciones obligatorias solo si el órgano la exige.
- **`Modalidad`:** tiene `getMesesMinimosEjecucion()` y `getMesesMaximosEjecucion()`. Los valores son TI = 3 y 9; PP = 6 y 12.

## Clases del dominio

```
domain/models/trabajo_grado/
├── Ejecucion         (tiempo de desarrollo; en las fases 4-6 tendrá prórroga y suspensión)
├── DatosEjecucion    (record: estado persistido)
└── Anteproyecto      (+ decisionesConsejo)
```

### `Ejecucion`
Se crea con `Ejecucion.iniciar(fechaNotificacion, modalidad)`, que solo invoca el agregado.

| Atributo / consulta | Descripción |
|---|---|
| `periodo` | `Periodo` desde la fecha de notificación hasta notificación + meses máximos |
| `getFechaNotificacion()`, `getFechaFin()` | Extremos del periodo |
| `fechaMinimaSustentacion` | Notificación + meses mínimos |
| `permiteSolicitarSustentacion(hoy)` | `true` desde la fecha mínima (la usará la Fase 7) |
| `vencida(hoy)`, `diasRestantes(hoy)` | Para la Fase 4 (vencimientos) y la Fase 5 (prórroga) |
| `datos()` | `DatosEjecucion` para persistir |

### `Anteproyecto` (cambios)
- `decisionesConsejo`: lista de `Decision` del Consejo de Facultad, en orden.
- `estadoTrasDecisionConsejo(decision)` valida sin modificar nada:
  - La emite el Consejo (`organo_invalido`).
  - La versión actual está `APROBADO` por los evaluadores (`decision_consejo_no_permitida`).
  - La ruta de la resolución existe (`resolucion_obligatoria`).
  - La fecha no es anterior a los conceptos de los evaluadores (`fecha_invalida`).
  - El resultado es `APROBADO` o `NO_APROBADO` (`resultado_consejo_invalido`).
- `registrarDecisionConsejo(decision)`: si no aprueba, el estado pasa a `NO_APROBADO`.
- Ahora también se puede radicar después de un `NO_APROBADO` del Consejo. Como los evaluadores ya están asignados, la nueva versión pasa directo a `EVALUACION`.

### `TrabajoGrado` (cambios)
- `ejecucion` es `null` hasta la aprobación; se consulta con `getEjecucion()`.
- `registrarDecisionConsejo(decision, responsable, ahora)`:
  1. Exige estar en `APROBACION_CONSEJO`.
  2. Valida la decisión.
  3. Si aprueba, crea la `Ejecucion` con la fecha de la decisión y la modalidad, y registra el cambio a `EN_CURSO`.
  4. Si no aprueba, registra el cambio a `ANTEPROYECTO`.

  Todo se valida antes de modificar el agregado.
- `DatosTrabajoGrado` tiene un nuevo componente, `ejecucion`, después de `anteproyecto`; es `null` si aún no hay ejecución. `DatosAnteproyecto` tiene `decisionesConsejo` después de `conceptos`.

## Aplicación e infraestructura

| Método y ruta | Rol | Cuerpo |
|---|---|---|
| `POST /api/v1/sgtg/trabajos-grado/{id}/anteproyecto/decisiones-consejo` | `CONSEJO` | `{ "resultado": "APROBADO", "rutaResolucion": "resoluciones/2026/res-100.pdf", "observaciones": "opcional" }` |

- **Caso de uso:** `registrarDecisionConsejo(trabajoId, usuarioId, resultado, rutaResolucion, observaciones)` crea la `Decision` del Consejo con la fecha del día (`Clock`), sin número de acta. El responsable del cambio de etapa es el código del usuario.
- **Validaciones del DTO:** `resultado` obligatorio; `rutaResolucion` obligatoria (`@NotBlank`).
- **Respuesta:**
  - `anteproyecto.decisionesConsejo`: órgano, resultado, ruta, observaciones y fecha.
  - Bloque nuevo `ejecucion`: `fechaNotificacion`, `fechaFin` y `fechaMinimaSustentacion`. Es `null` hasta la aprobación.
- **Persistencia:** nueva tabla `anteproyecto_decisiones_consejo` (`DecisionEmbeddable`) y columnas `ejecucion_fecha_notificacion`, `ejecucion_fecha_fin` y `ejecucion_fecha_minima_sustentacion` en `trabajos_grado`.

## Plan de pruebas

```bash
./mvnw test -Dtest=EtapaAprobacionConsejoTest                 # dominio
./mvnw test -Dtest='ManageTrabajoGrado*Test'                  # caso de uso y persistencia
./mvnw test -Dtest=FlujoAnteproyectoIntegracionTest           # punta a punta por HTTP
```

### `EtapaAprobacionConsejoTest` (13 pruebas, dominio)
| Prueba | Qué verifica |
|---|---|
| `aprobadoPasaAEnCursoEIniciaLosNueveMesesDeInvestigacion` | `EN_CURSO`; notificación 20-mar-2026, fin 20-dic-2026, mínima 20-jun-2026; ruta guardada sin acta; historial |
| `laPracticaProfesionalTieneDeSeisMesesAUnAnio` | Fin 20-mar-2027, mínima 20-sep-2026 |
| `noAprobadoVuelveAAnteproyectoYLaNuevaVersionPasaPorLosEvaluadores` | Ciclo completo: no aprueba → versión 2 → evaluadores → aprueba |
| `elComentarioEsOpcionalAunqueNoApruebe` | `NO_APROBADO` sin observaciones |
| `laRutaDeLaResolucionEsObligatoria` | Ruta nula o en blanco; no modifica nada |
| `elConsejoSoloApruebaONoAprueba` (×3) | `CON_CORRECCIONES`, `RECHAZADO` y `EVALUADO` se rechazan |
| `laDecisionDebeEmitirlaElConsejoDeFacultad` | Una decisión del Comité se rechaza |
| `laDecisionNoPuedeSerAnteriorALosConceptosDeLosEvaluadores` | `fecha_invalida` |
| `soloSeDecideEnLaEtapaDeAprobacionDelConsejo` | Una segunda decisión en `EN_CURSO` responde `etapa_invalida` |
| `laEjecucionInformaSusPlazos` | `permiteSolicitarSustentacion`, `vencida`, `diasRestantes` |
| `laEjecucionSeReconstruyeConSusFechas` | `DatosEjecucion` y su validación |

### Otras pruebas
- **`DecisionConceptoTest` (+1):** el Consejo no exige observaciones; el Departamento sí.
- **`ManageTrabajoGradoCUImplAdapterTest` (+2):**
  - Aprobación con la fecha del reloj como notificación y fin a 9 meses.
  - Un no aprobado sin comentario y nueva radicación hacia evaluación.
- **`ManageTrabajoGradoGatewayImplAdapterTest` (+1):** guarda y recupera las decisiones del Consejo y la ejecución; un trabajo sin aprobar no tiene ejecución.
- **`FlujoAnteproyectoIntegracionTest` (+1):** por HTTP se verifica:
  - 403 para un usuario sin rol `CONSEJO`.
  - 400 sin ruta.
  - No aprobado sin comentario, nueva versión, evaluadores y aprobado con el bloque `ejecucion`.
  - 400 ante una segunda decisión.

Total del proyecto: **215 pruebas** (antes eran 197).

### Thunder Client
- **Preparación:** se crea un miembro del Consejo.
- **Carpeta 3** ("Flujo del anteproyecto y aprobación del Consejo"): el Consejo no aprueba (sin comentario), se radica la versión 3, los evaluadores la aprueban y el Consejo aprueba (pasa a `EN_CURSO`).
- **Carpeta 4:** 403 para un docente, ruta obligatoria y decisión fuera de etapa.

En total son 60 peticiones, validadas contra Docker con MySQL.

### Qué no se prueba en esta fase
- Las notificaciones: la fecha de notificación es la del registro de la resolución.
- Las reglas de la etapa en curso (vencimiento, cambio de objetivos, informes mensuales): Fase 4.
