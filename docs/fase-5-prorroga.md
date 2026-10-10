# Fase 5: Prórroga

Este documento describe la Fase 5: clases del dominio, reglas, operaciones, la API y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md). La prórroga extiende la `Ejecucion` descrita en [fase-3-aprobacion-consejo.md](fase-3-aprobacion-consejo.md) y [fase-4-en-curso.md](fase-4-en-curso.md).

Con el trabajo `EN_CURSO`, el director puede solicitar **una sola vez** una prórroga de **1 a 3 meses**. Adjunta el documento de la solicitud y debe hacerlo al menos **15 días calendario antes del fin** del plazo. El Consejo de Facultad la decide con la resolución adjunta:

- **Aprueba:** el fin del plazo se extiende por los meses solicitados y el trabajo pasa a `PRORROGA`.
- **No aprueba:** el trabajo sigue `EN_CURSO` con el plazo original, y la oportunidad queda consumida.

Aplica a ambas modalidades. La práctica profesional también puede extender su plazo, por ejemplo para la entrega de la monografía.

Artículos de la Resolución 231 de 2020 que cubre: **16 y 41**.

```
                 solicitarProrroga (≥ 15 días antes del fin)        Consejo APROBADO
EN_CURSO ──────────────────────────────────────────► EN_CURSO ──────────────────────► PRORROGA (fin + meses)
                                                 (prórroga pendiente)
                                                        │ Consejo NO_APROBADO
                                                        ▼
                                                     EN_CURSO (plazo original; no se puede volver a solicitar)
```

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| 1 | El Consejo decide igual que en las fases anteriores: `APROBADO`/`NO_APROBADO`, ruta de la resolución obligatoria y comentario opcional. | Usuario |
| 2 | El director pide de 1 a 3 meses. | Usuario |
| 3 | La solicitud lleva la ruta del documento (obligatoria). | Usuario |
| 4 | La práctica profesional también tiene prórroga. | Usuario |
| 5 | Una prórroga **negada también consume** la única oportunidad. | Plan (caso no definido 1) |
| 6 | El Consejo concede los meses solicitados; no hay concesión parcial. | Propia |
| 7 | Mientras la prórroga está **pendiente**, la tarea de vencimientos **no cierra** el trabajo aunque el plazo haya terminado. Así se evita cerrarlo antes de que el Consejo decida. Si la niega después del fin, el trabajo se cierra en la siguiente verificación diaria. | Propia |
| 8 | La aprobación puede llegar después del fin original. La extensión siempre se cuenta desde ese fin, no desde la fecha de la decisión. | Propia |

## Clases del dominio

### `Prorroga` (record)
Componentes: `meses`, `rutaSolicitud`, `fechaSolicitud` y `decision`. La decisión es `null` mientras está pendiente.

Al crearse valida:
- Que los meses estén entre 1 y 3 (`meses_invalidos`; constantes `MESES_MINIMOS` y `MESES_MAXIMOS`).
- Que tenga la ruta de la solicitud (`documento_obligatorio`).

Si trae decisión, también valida:
- Que la emita el Consejo (`organo_invalido`).
- Que tenga la ruta de la resolución (`resolucion_obligatoria`).
- Que el resultado sea `APROBADO` o `NO_APROBADO` (`resultado_invalido`).
- Que su fecha no sea anterior a la solicitud (`fecha_invalida`).

Métodos: `solicitar(...)`, `resolver(decision)` (`sin_solicitud_pendiente` si ya está resuelta), `estaPendiente()` y `fueAprobada()`.

### `Ejecucion` (cambios)
- El `periodo` ya no es final: se extiende con `Periodo.extenderMeses` cuando se aprueba la prórroga.
- **`solicitarProrroga(solicitud)`** rechaza la solicitud en estos casos:
  - Ya existe una, aunque haya sido negada (`ya_solicitada`).
  - La fecha es anterior a la notificación o posterior a `fechaLimiteSolicitudProrroga()` = fin − 15 días (`fuera_de_plazo`).
- **`prorrogaResuelta(decision)`** valida sin modificar nada. **`aplicarProrroga(resuelta)`** guarda la prórroga y, si se aprobó, extiende el fin.
- **Consultas:** `tieneProrrogaPendiente()`, `fechaLimiteSolicitudProrroga()` y `getProrroga()`. `DatosEjecucion` incluye `prorroga`, y `fechaFin` es el fin vigente, con la prórroga si se aprobó.

### `TrabajoGrado` (cambios)
| Operación | Regla |
|---|---|
| `solicitarProrroga(meses, rutaSolicitud, hoy)` | Solo `EN_CURSO` |
| `resolverProrroga(decision, responsable, ahora)` | Solo `EN_CURSO`. Valida todo antes de modificar. Si se aprueba, registra el cambio `EN_CURSO → PRORROGA` |
| `verificarVencimientos(ahora)` | No cierra si hay prórroga pendiente. En `PRORROGA` vence al terminar el plazo extendido |

En `PRORROGA` se siguen radicando informes mensuales (práctica profesional). El cambio de objetivos y una nueva prórroga ya no están permitidos.

## Aplicación e infraestructura

| Método y ruta (`/api/v1/sgtg/trabajos-grado`) | Permiso | Cuerpo |
|---|---|---|
| `POST /{id}/prorroga` | Director (caso de uso) | `{ "meses": 2, "rutaSolicitud": "solicitudes/2026/prorroga.pdf" }` |
| `POST /{id}/prorroga/decisiones-consejo` | `CONSEJO` | `{ "resultado": "APROBADO", "rutaResolucion": "...", "observaciones": "opcional" }` |

- El DTO valida `meses` (obligatorio, de 1 a 3 con `@Min`/`@Max`) y `rutaSolicitud` (`@NotBlank`).
- Las fechas de la solicitud y de la decisión son las del día (`Clock`).
- **Respuesta:** el bloque `ejecucion` incluye `fechaLimiteSolicitudProrroga` y `prorroga` (meses, ruta, fecha y `decision`). `fechaFin` refleja la extensión.
- **Persistencia:** `ProrrogaEmbeddable` va embebido en `trabajos_grado` con columnas `prorroga_*`, incluida la decisión (`prorroga_decision_*`). Todas son nulas si no se ha solicitado.

## Plan de pruebas

```bash
./mvnw test -Dtest=EtapaProrrogaTest                          # dominio
./mvnw test -Dtest='ManageTrabajoGrado*Test'                  # caso de uso y persistencia
./mvnw test -Dtest=FlujoAnteproyectoIntegracionTest           # punta a punta por HTTP
```

### `EtapaProrrogaTest` (15 pruebas, dominio)
| Prueba | Qué verifica |
|---|---|
| `aprobadaExtiendeElPlazoYPasaAProrroga` | Fin del 20-dic-2026 al 20-feb-2027; `PRORROGA`; rutas guardadas; historial con responsable |
| `negadaSigueEnCursoYConsumeLaUnicaOportunidad` | Plazo original, sin cambio de etapa; volver a solicitar responde `ya_solicitada` |
| `soloSeSolicitaUnaVez` | Una segunda solicitud con otra pendiente |
| `losMesesVanDeUnoATres` (×3) | 0, 4 y -1 |
| `laSolicitudExigeElDocumento` | `documento_obligatorio` |
| `seSolicitaConAlMenosQuinceDiasDeAnticipacion` | Límite 5-dic-2026 incluido; el 6-dic o antes de la notificación responde `fuera_de_plazo` |
| `laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido` | Sin solicitud, sin ruta, otro órgano, resultado inválido, fecha anterior; nada se modifica |
| `enProrrogaNoSeVuelveASolicitarNiADecidir` | `etapa_invalida` |
| `elVencimientoEsperaLaDecisionDelConsejo` | Con la prórroga pendiente no cierra; negada después del fin, cierra en la siguiente verificación |
| `enProrrogaVenceAlTerminarElPlazoExtendido` | Cierra el día siguiente al fin extendido, desde `PRORROGA` |
| `laPracticaProfesionalTambienTieneProrrogaYSigueRadicandoInformes` | 3 meses (fin 20-jun-2027) e informe en `PRORROGA` |
| `laProrrogaSeReconstruyeConSuDecision` | `DatosEjecucion` de ida y vuelta |

### Otras pruebas
- **`ManageTrabajoGradoCUImplAdapterTest` (+1):** solo el director solicita; el Consejo aprueba con la fecha del reloj y el fin se extiende 2 meses.
- **`ManageTrabajoGradoGatewayImplAdapterTest`:** la práctica guarda y recupera una prórroga negada con su decisión.
- **`FlujoAnteproyectoIntegracionTest`:** por HTTP se verifica:
  - 4 meses responde 400.
  - Solicitud de 2 meses pendiente.
  - El Consejo aprueba: `PRORROGA` y `fechaFin` + 2 meses.
  - Una segunda solicitud responde 400.

Total del proyecto: **250 pruebas** (antes eran 235).

### Thunder Client
- **Carpeta 4:** la práctica solicita 3 meses de prórroga, el Consejo la aprueba (`PRORROGA`) y se radica un informe durante la prórroga.
- **Carpeta 5 (permisos):** gana 5 pruebas:
  - Más de 3 meses.
  - Solo el director solicita.
  - Un docente no decide.
  - Sin solicitud pendiente.
  - No se solicita estando en prórroga.

En total son 87 peticiones, validadas contra Docker con MySQL.

### Qué no se prueba en esta fase
- La suspensión, que se permite desde `EN_CURSO` o `PRORROGA` y congela el tiempo restante: Fase 6.
