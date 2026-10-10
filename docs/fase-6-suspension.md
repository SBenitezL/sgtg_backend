# Fase 6: Suspensión

Este documento describe la Fase 6: clases del dominio, reglas, operaciones, la API y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md). La suspensión actúa sobre la `Ejecucion` descrita en [fase-3-aprobacion-consejo.md](fase-3-aprobacion-consejo.md), [fase-4-en-curso.md](fase-4-en-curso.md) y [fase-5-prorroga.md](fase-5-prorroga.md).

Con el trabajo en `EN_CURSO` o `PRORROGA`, el director puede solicitar la suspensión con el documento adjunto. El Consejo de Facultad la decide con la resolución adjunta.

Si la aprueba:
- Rige desde la **fecha de la decisión** y el trabajo pasa a `SUSPENDIDO`.
- Se **congelan los días que faltaban** del plazo.
- La suspensión dura como máximo **hasta el fin del período académico siguiente**. El trabajo se reanuda a más tardar el primer día del período posterior.

La reanuda el director con un documento adjunto o, al llegar la fecha máxima, la **tarea diaria** automáticamente. Al reanudar, el trabajo vuelve a la etapa en la que estaba, y el **nuevo fin = fecha de reanudación + días congelados**.

Artículos de la Resolución 231 de 2020 que cubre: **27 y 57**.

```
EN_CURSO / PRORROGA ──solicitar──► (pendiente) ──Consejo APROBADO──► SUSPENDIDO ──reanudar (director o automática)──► etapa previa
                                        │                                                           (fin = reanudación + días congelados)
                                        └──Consejo NO_APROBADO──► sigue igual (puede volver a solicitar)
```

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| 1 | La solicita el director con la ruta del documento. | Usuario |
| 2 | El Consejo decide igual que en las fases anteriores (ruta de la resolución obligatoria, comentario opcional). | Usuario |
| 3 | Una suspensión **negada no consume** la única oportunidad. Solo puede haber una solicitud pendiente. | Usuario |
| 4 | Períodos académicos de 6 meses: enero–junio y julio–diciembre (`PeriodoAcademico`). | Usuario |
| 5 | La suspensión cubre el resto del período en que se aprueba y el **período siguiente completo**. Por ejemplo, si se aprueba el 10-oct-2026, se reanuda a más tardar el 1-jul-2027. | Usuario |
| 6 | Rige desde la fecha de la decisión del Consejo. | Usuario |
| 7 | La reanuda el director con la ruta de un documento. | Usuario |
| 8 | Si llega la fecha máxima sin reanudar, la tarea diaria la reanuda **en esa fecha**, aunque se ejecute después. Si con eso el plazo ya venció, el trabajo se cierra por vencimiento en la misma verificación. | Plan (caso no definido 2) |
| 9 | Solo se puede solicitar dentro del plazo de ejecución. El Consejo no puede aprobarla si en la fecha de su decisión el plazo ya terminó (no habría días que congelar). | Propia |
| 10 | Mientras hay una suspensión **pendiente**, la tarea diaria no cierra el trabajo por vencimiento, igual que con la prórroga. | Propia |

## Clases del dominio

### `PeriodoAcademico (anio, semestre)` — `comun`
- `de(fecha)`: devuelve el semestre 1 si la fecha cae entre enero y junio, y el 2 si cae entre julio y diciembre.
- `siguiente()`, `inicio()` y `fin()`.
- `toString()` produce el formato `2026-2`.

### `Suspension` (record)
| Componente | Descripción |
|---|---|
| `rutaSolicitud`, `fechaSolicitud` | Solicitud del director; la ruta es obligatoria |
| `decision` | Decisión del Consejo; `null` mientras esté pendiente |
| `etapaPrevia` | `EN_CURSO` o `PRORROGA`, a la que vuelve al reanudar (solo si se aprobó) |
| `diasRestantes` | Días del plazo que faltaban en la fecha de la decisión (> 0) |
| `fechaMaximaReanudacion` | `PeriodoAcademico.de(inicio).siguiente().fin() + 1 día` |
| `fechaReanudacion`, `rutaReanudacion` | Reanudación. Una ruta `null` indica que fue automática |

Al crearse valida:
- La decisión: que la emita el Consejo, que tenga la ruta de la resolución, que el resultado sea `APROBADO` o `NO_APROBADO` y que su fecha no sea anterior a la solicitud.
- Que si se aprobó, tenga todos los datos de la suspensión.
- Que la reanudación quede entre el inicio y la fecha máxima.

Métodos:
- `solicitar(...)`.
- `resolver(decision, etapaPrevia, diasRestantes)`, que lanza `plazo_vencido` si no quedan días.
- `reanudar(fecha, ruta)`.
- `estaPendiente()`, `fueAprobada()`, `estaActiva()`, `fechaInicio()` y `fueReanudadaAutomaticamente()`.

### `Ejecucion` (cambios)
- Tiene la lista `suspensiones`.
- **`solicitarSuspension`** rechaza la solicitud si hay otra pendiente (`solicitud_pendiente`), si ya hubo una aprobada (`ya_realizada`) o si la fecha cae fuera de [notificación, fin] (`fuera_de_plazo`).
- **Decisión:** `suspensionResuelta(decision, etapaActual)` calcula los días restantes y valida. `aplicarSuspension` la registra.
- **Reanudación:** `suspensionReanudada(fecha, ruta)` valida. `aplicarReanudacion` registra la reanudación y cambia el periodo con `Periodo.reanudarDesde(fecha, dias)`.
- **Consultas:** `suspensionPendiente()`, `suspensionActiva()`, `tieneSuspensionPendiente()` y `getSuspensiones()`. `DatosEjecucion` incluye `suspensiones`.

### `TrabajoGrado` (cambios)
| Operación | Regla |
|---|---|
| `solicitarSuspension(ruta, hoy)` | En `EN_CURSO` o `PRORROGA` |
| `resolverSuspension(decision, responsable, ahora)` | En `EN_CURSO` o `PRORROGA`. Si se aprueba: `→ SUSPENDIDO`. Valida todo antes de modificar |
| `reanudar(rutaDocumento, responsable, ahora)` | Solo `SUSPENDIDO`. Documento obligatorio (`documento_reanudacion_obligatorio`). Vuelve a la etapa previa |
| `verificarVencimientos(ahora)` | En `SUSPENDIDO`, desde la fecha máxima: reanuda automáticamente (responsable `SISTEMA`) y después aplica el vencimiento. No cierra con una suspensión pendiente |

Estando `SUSPENDIDO` no se permiten informes, prórroga ni otra suspensión.

## Aplicación e infraestructura

| Método y ruta (`/api/v1/sgtg/trabajos-grado`) | Permiso | Cuerpo |
|---|---|---|
| `POST /{id}/suspension` | Director (caso de uso) | `{ "rutaDocumento": "solicitudes/2026/suspension.pdf" }` |
| `POST /{id}/suspension/decisiones-consejo` | `CONSEJO` | `{ "resultado": "APROBADO", "rutaResolucion": "...", "observaciones": "opcional" }` |
| `POST /{id}/suspension/reanudacion` | Director (caso de uso) | `{ "rutaDocumento": "solicitudes/2027/reanudacion.pdf" }` |

- Las fechas de la solicitud, la decisión y la reanudación son las del día (`Clock`).
- `DocumentoAdjuntoDTORequest` es un DTO genérico con solo la ruta de un documento.
- **Respuesta:** `ejecucion.suspensiones` incluye solicitud, decisión, `fechaInicio`, `etapaPrevia`, `diasRestantes`, `fechaMaximaReanudacion`, `fechaReanudacion`, `rutaReanudacion` y `reanudacionAutomatica`.
- **Tarea diaria:** `verificarVencimientos()` ahora también recorre los trabajos `SUSPENDIDO` y cuenta los reanudados y los cerrados.
- **Persistencia:** `ejecucion_suspensiones` (`SuspensionEmbeddable`, con la decisión en columnas `decision_*`).

## Plan de pruebas

```bash
./mvnw test -Dtest='EtapaSuspensionTest,PeriodoAcademicoTest'   # dominio
./mvnw test -Dtest='ManageTrabajoGrado*Test'                    # caso de uso y persistencia
./mvnw test -Dtest=FlujoAnteproyectoIntegracionTest             # punta a punta por HTTP
```

### `EtapaSuspensionTest` (19 pruebas) y `PeriodoAcademicoTest` (3)
| Prueba | Qué verifica |
|---|---|
| `aprobadaRigeDesdeLaDecisionYCongelaLosDiasRestantes` | Aprobada el 10-oct-2026: 71 días congelados, máximo 1-jul-2027, `SUSPENDIDO` |
| `negadaSigueEnCursoYNoConsumeLaOportunidad` | Se vuelve a solicitar y se aprueba |
| `soloSeSuspendeUnaVez` | `ya_realizada` |
| `noSeSolicitaOtraMientrasHayaUnaPendiente` | `solicitud_pendiente` |
| `laSolicitudExigeElDocumentoYEstarDentroDelPlazo` | Documento; antes de la notificación o después del fin |
| `laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido` | Sin solicitud, sin ruta, otro órgano, resultado inválido, fecha anterior |
| `noSeApruebaSiElPlazoYaTermino` | `plazo_vencido` |
| `elDirectorReanudaYElFinEsLaReanudacionMasLosDiasCongelados` | 1-feb-2027 + 71 = 13-abr-2027 |
| `laReanudacionExigeDocumentoYUnaSuspensionVigente` | Fuera de `SUSPENDIDO` y sin documento |
| `desdeProrrogaVuelveAProrroga` | Vuelve a la etapa previa con 133 días congelados |
| `suspendidoNoAdmiteProrrogaNiInformes` | `etapa_invalida` |
| `alLlegarLaFechaMaximaSeReanudaAutomaticamente` | El 30-jun no reanuda; el 1-jul reanuda (fin 10-sep-2027, responsable `SISTEMA`) |
| `siLaVerificacionLlegaTardeReanudaEnLaFechaMaximaYAplicaElVencimiento` | Reanuda el 1-jul y queda `NO_APROBADO` en la misma verificación |
| `unaSuspensionPendienteDetieneElVencimiento` | No cierra mientras el Consejo no decida |
| `laFechaMaximaEsElInicioDelPeriodoPosteriorAlSiguiente` (×4) | Límites 15-ene, 30-jun, 1-jul, 31-dic |
| `laSuspensionSeReconstruyeConSuEstado` | `DatosEjecucion` de ida y vuelta |
| `PeriodoAcademicoTest` | Período de una fecha, límites, siguiente y semestre inválido |

### Otras pruebas
- **`ManageTrabajoGradoCUImplAdapterTest` (+2):**
  - Solicitud y reanudación solo del director, con la fecha máxima calculada desde el reloj y el fin recalculado.
  - La verificación diaria reanuda una suspensión vencida.
- **`ManageTrabajoGradoGatewayImplAdapterTest`:**
  - La práctica guarda una suspensión negada y otra aprobada y reanudada.
  - El fin del plazo incluye los 60 días suspendida.
- **`FlujoAnteproyectoIntegracionTest`:** por HTTP, una suspensión durante la prórroga vuelve a `PRORROGA`. Un estudiante no reanuda (406) y no se suspende dos veces.

Total del proyecto: **274 pruebas** (antes eran 250).

### Thunder Client
- **Carpeta 4:** la práctica, en prórroga, solicita la suspensión. El Consejo la niega, se solicita de nuevo y la aprueba (`SUSPENDIDO`). No se puede radicar un informe y el director la reanuda (vuelve a `PRORROGA`).
- **Carpeta 5 (permisos):** gana 5 pruebas: solo el director, documento obligatorio, un docente no decide, solo una vez y no se reanuda un trabajo no suspendido.

En total son 98 peticiones, validadas contra Docker con MySQL. La reanudación automática no se prueba en Thunder porque depende del paso del tiempo.

### Qué no se prueba en esta fase
- La sustentación, que se solicita desde `EN_CURSO` o `PRORROGA`: Fase 7.
