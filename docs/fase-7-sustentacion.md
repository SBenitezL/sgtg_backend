# Fase 7: Sustentación

Este documento describe la Fase 7: clases del dominio, reglas, operaciones, la API y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md). La sustentación parte de la `Ejecucion` (fecha mínima y fin del plazo), descrita en [fase-3-aprobacion-consejo.md](fase-3-aprobacion-consejo.md) y [fase-4-en-curso.md](fase-4-en-curso.md).

Artículos de la Resolución 231 de 2020 que cubre: **19 a 25 y 46 a 55**.

## Flujo

1. **Solicitud (director).**
   - El director adjunta el documento final (formato E) y el paz y salvo (formato F). En práctica profesional también adjunta el concepto favorable del asesor.
   - Se solicita desde `EN_CURSO` o `PRORROGA`, después de los meses mínimos (3 en investigación, 6 en práctica) y antes del fin del plazo.
   - El trabajo pasa a `SUSTENTACION`, lo que detiene el vencimiento del plazo de ejecución.
2. **Programación (Consejo de Facultad):** dos jurados, fecha, hora y salón.
3. **Calificación (cada jurado).** Con las dos calificaciones rige la **menos favorable**:

| Resultado | Sesión 1 | Sesión 2 |
|---|---|---|
| `APROBADO` | Un jurado carga la versión definitiva (formato H) → `FINALIZADO` | Igual |
| `APROBADO_CON_OBSERVACIONES` | El director radica los ajustes en 10 días hábiles y los jurados los verifican. Si ambos cumplen: aprobada. Si alguno no cumple, o no se radican a tiempo: se trata como `APLAZADO` | Igual |
| `APLAZADO` o inasistencia | `APLAZADO`: segunda sesión en ≤ 60 días calendario | `NO_APROBADO` |
| `NO_APROBADO` | `NO_APROBADO` | `NO_APROBADO` |

4. **Tarea diaria.**
   - Si vencen los 10 días hábiles de los ajustes, la sustentación se aplaza.
   - Si pasan los 60 días del aplazamiento sin segunda sesión, el trabajo queda `NO_APROBADO` con motivo `VENCIMIENTO_PLAZO`.

```
EN_CURSO/PRORROGA ─solicitar─► SUSTENTACION (SOLICITADA ─programar─► PROGRAMADA ─calificar─► …)
                                   │   ▲                                  │
                       aplazada ▼   │ programar 2.ª sesión (≤ 60 días)     ├─► APROBADA ─versión definitiva─► FINALIZADO
                              APLAZADO ─(60 días sin sesión)─► NO_APROBADO └─► NO_APROBADA ─────────────────► NO_APROBADO
```

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| 1 | El director adjunta el documento final y el paz y salvo al solicitar (solo rutas). | Usuario |
| 2 | En práctica profesional, el director adjunta la ruta del concepto favorable del asesor. | Usuario |
| 3 | El Consejo programa jurados, fecha, hora y salón. Los jurados pueden ser los evaluadores del anteproyecto. | Usuario |
| 4 | Cada jurado registra su calificación. | Usuario |
| 5 | El director radica los ajustes y los jurados los verifican. | Usuario |
| 6 | La versión definitiva la cargan los jurados (uno de ellos basta). | Usuario |
| 7 | La inasistencia la registran los jurados (uno de ellos basta), con observaciones como soporte. | Usuario |
| 8 | Si las calificaciones no coinciden, rige la **menos favorable** (`NO_APROBADO` < `APLAZADO` < `APROBADO_CON_OBSERVACIONES` < `APROBADO`). Está en `Calificacion.menosFavorable`. | **Propia, por confirmar** |
| 9 | Los jurados no pueden ser director, codirector, asesor ni estudiantes del trabajo (igual que los evaluadores). | Propia |
| 10 | No se puede solicitar la sustentación con una prórroga o una suspensión pendientes de decisión. | Propia |
| 11 | La sesión no puede programarse en una fecha pasada. Los jurados registran la calificación o la inasistencia desde la fecha de la sesión. | Propia |
| 12 | No se registra inasistencia si un jurado ya calificó la sesión. La segunda sesión puede tener jurados distintos. | Propia |
| 13 | Si los ajustes no se radican a tiempo, los 60 días de la segunda sesión cuentan desde el día siguiente al vencimiento de los ajustes. | Propia |

## Clases del dominio

```
domain/models/trabajo_grado/
├── Sustentacion           (etapa: solicitud, sesiones, calificaciones, ajustes, versión definitiva)
├── DatosSustentacion      (record: estado persistido)
├── EstadoSustentacion     (SOLICITADA, PROGRAMADA, EN_AJUSTES, AJUSTES_RADICADOS, APROBADA, APLAZADA, FINALIZADA, NO_APROBADA)
├── Calificacion           (enum con menosFavorable)
├── SesionSustentacion     (record: número, 2 jurados, fecha y hora, salón, fecha de programación)
├── CalificacionJurado     (record: sesión, jurado, calificación, observaciones, ruta del acta, fecha)
└── RegistroJurado         (record: inasistencia o verificación de ajustes)
```

Los jurados se modelan con `Evaluador`. Sus roles permitidos ya incluyen `JURADO`, `DOCENTE` y `EVALUADOR`.

### `Sustentacion`
- **`solicitar(rutaFinal, rutaPazYSalvo, rutaConceptoAsesor, fecha, requiereConcepto)`**: valida que estén los documentos.
- **`programar(sesion)`**:
  - Solo con el estado `SOLICITADA` (primera sesión) o `APLAZADA` (segunda sesión, a más tardar en `fechaLimiteSegundaSesion`).
  - Errores: `programacion_no_permitida` o `segunda_sesion_fuera_de_plazo`.
- **`calificar(calificacion, calendario)`**:
  - Exige que el emisor sea jurado de la sesión actual, que no haya calificado ya y que la fecha no sea anterior a la sesión.
  - Con la segunda calificación aplica la tabla del flujo.
- **`registrarInasistencia(registro)`**: aplaza la sesión 1; en la sesión 2 cierra con motivo `INASISTENCIA_SUSTENTACION`.
- **`radicarAjustes(ruta, fecha)`**: solo en `EN_AJUSTES` y dentro de `fechaLimiteAjustes`.
- **`verificarAjustes(registro)`**: con las dos verificaciones queda `APROBADA` si ambas cumplen, o se aplaza si alguna no.
- **`cargarVersionDefinitiva(jurado, ruta, fecha)`**: solo en `APROBADA` → `FINALIZADA`.
- **`verificarPlazos(hoy)`**: aplica los vencimientos de los ajustes y de la segunda sesión.
- **Consultas:** `sesionActual()`, `jurado(usuarioId)`, `esJurado(usuarioId)`, `calificacionesSesionActual()` y `datos()`.

Cada operación valida todo antes de modificar. El motivo del cierre queda en `motivoNoAprobacion`.

### `TrabajoGrado` (cambios)
| Operación | Regla |
|---|---|
| `solicitarSustentacion(rutaFinal, rutaPazYSalvo, rutaConceptoAsesor, responsable, ahora)` | `EN_CURSO`/`PRORROGA`. Meses mínimos (`antes_de_tiempo`), antes del fin (`plazo_vencido`), sin decisiones pendientes (`decision_pendiente`). `→ SUSTENTACION` |
| `programarSustentacion(jurado1, jurado2, fechaHora, salon, responsable, ahora)` | `SUSTENTACION` o `APLAZADO` (este último `→ SUSTENTACION`). Jurados sin conflicto (`jurado_con_conflicto`) |
| `calificarSustentacion(jurado, calificacion, observaciones, rutaActa, ahora, calendario)` | `SUSTENTACION` |
| `registrarInasistencia(jurado, observaciones, ahora)` | `SUSTENTACION` |
| `radicarAjustesSustentacion(ruta, ahora)` / `verificarAjustesSustentacion(jurado, cumple, observaciones, ahora)` | `SUSTENTACION` |
| `cargarVersionDefinitiva(jurado, ruta, ahora)` | `SUSTENTACION` → `FINALIZADO` |
| `verificarVencimientos(ahora)` | En `SUSTENTACION`/`APLAZADO` aplica `Sustentacion.verificarPlazos` |

- Después de cada operación, `sincronizarConSustentacion` lleva el trabajo a `APLAZADO`, `FINALIZADO` o `NO_APROBADO`, con el motivo que corresponda.
- `participa` incluye a los jurados de cualquier sesión.
- `DatosTrabajoGrado` tiene un componente nuevo, `sustentacion`. Se conserva un constructor sin él para los trabajos que aún no la han solicitado.

## Aplicación e infraestructura

| Método y ruta (`/api/v1/sgtg/trabajos-grado/{id}/sustentacion…`) | Permiso | Cuerpo |
|---|---|---|
| `POST /sustentacion` | Director | `{ "rutaDocumentoFinal", "rutaPazYSalvo", "rutaConceptoAsesor" (PP) }` |
| `POST /sustentacion/programacion` | `CONSEJO` | `{ "jurado1Id", "jurado2Id", "fechaHora": "2026-10-20T14:00:00", "salon" }` |
| `POST /sustentacion/calificaciones` | Jurado de la sesión actual | `{ "calificacion", "observaciones", "rutaActa" }` |
| `POST /sustentacion/inasistencia` | Jurado de la sesión actual | `{ "observaciones" }` |
| `POST /sustentacion/ajustes` | Director | `{ "rutaDocumento" }` |
| `POST /sustentacion/ajustes/verificaciones` | Jurado de la sesión actual | `{ "cumple": true, "observaciones" }` |
| `POST /sustentacion/version-definitiva` | Jurado de la sesión actual | `{ "rutaDocumento" }` |

- **Permisos:** el caso de uso identifica al jurado con `Sustentacion.jurado(usuarioId)`. Si el usuario no es jurado de la sesión actual, responde `solo_jurado` (406). Los jurados se crean desde su `Usuario` (roles `DOCENTE`, `EVALUADOR` o `JURADO`).
- **Fechas:** todas son las del día (`Clock`).
- **Respuesta:** un bloque `sustentacion` con estado, documentos, sesiones (con jurados), calificaciones, inasistencias, verificaciones, ajustes, plazos, versión definitiva y motivo del cierre.
- **Tarea diaria:** ahora también recorre los trabajos `SUSTENTACION` y `APLAZADO`.
- **Persistencia:**
  - Columnas `sustentacion_*` en `trabajos_grado`. Las rutas son de 500 caracteres porque la fila de MySQL está cerca de su límite.
  - Tablas `sustentacion_sesiones` (jurados en columnas `jurado1_*`/`jurado2_*`), `sustentacion_calificaciones`, `sustentacion_inasistencias` y `sustentacion_verificaciones`. El jurado de cada registro se resuelve contra los de su sesión.
  - La consulta "mis trabajos" incluye a los jurados.

## Plan de pruebas

```bash
./mvnw test -Dtest=EtapaSustentacionTest                       # dominio
./mvnw test -Dtest='ManageTrabajoGrado*Test'                   # caso de uso y persistencia
./mvnw test -Dtest=FlujoSustentacionIntegracionTest            # punta a punta por HTTP con reloj ajustable
```

### `EtapaSustentacionTest` (21 pruebas, dominio)
| Prueba | Qué verifica |
|---|---|
| `laSolicitudPasaASustentacionYDetieneElVencimiento` | `SUSTENTACION`/`SOLICITADA`; el fin del plazo ya no cierra |
| `seSolicitaDespuesDeLosMesesMinimosYAntesDelFin` | 19-jun no; 21-dic no; 20-jun sí |
| `exigeDocumentoFinalPazYSalvoYEnPracticaElConceptoDelAsesor` | Los tres documentos |
| `noSeSolicitaConUnaProrrogaPendiente` | `decision_pendiente` |
| `elConsejoProgramaJuradosFechaYSalon` | Sesión 1, jurados como participantes; no se reprograma |
| `laProgramacionValidaJuradosYFecha` | Conflicto, repetidos, faltante, salón, fecha pasada |
| `ambosApruebanYUnJuradoCargaLaVersionDefinitiva` | `FINALIZADO`; solo un jurado de la sesión |
| `rigeLaCalificacionMenosFavorable` (×3) | `EN_AJUSTES`, `APLAZADO`, `NO_APROBADO` |
| `noAprobadoCierraElTrabajo` | Motivo `SUSTENTACION_NO_APROBADA` |
| `laCalificacionValidaJuradoFechaYObservaciones` | No jurado, antes de la sesión, sin observaciones, repetida |
| `conObservacionesElDirectorRadicaAjustesYLosJuradosLosAprueban` | Límite 3-sep-2026; aprobada y finalizada |
| `siLosAjustesNoCumplenSeAplaza` | `APLAZADO`; segunda sesión hasta el 1-nov |
| `siNoSeRadicanLosAjustesATiempoSeAplaza` | Rechazo fuera de plazo; la tarea diaria aplaza (`SISTEMA`) |
| `aplazadoSeProgramaUnaSegundaSesionDentroDeSesentaDias` | Límite 19-oct; sesión 2 aprobada |
| `unSegundoAplazamientoCierraElTrabajo` | `NO_APROBADO` |
| `sinSegundaSesionEnSesentaDiasQuedaNoAprobadoPorVencimiento` | `VENCIMIENTO_PLAZO` desde `APLAZADO` |
| `laInasistenciaAplazaLaPrimeraSesionYCierraLaSegunda` | `INASISTENCIA_SUSTENTACION` |
| `noSeRegistraInasistenciaEnUnaSesionYaCalificada` | `sesion_ya_calificada` |
| `laSustentacionSeReconstruyeConSuEstado` | `DatosSustentacion` de ida y vuelta |

### Otras pruebas
- **`ManageTrabajoGradoCUImplAdapterTest` (+2):**
  - Flujo con reloj de julio: solo el director solicita; el Consejo programa a evaluadores como jurados; solo un jurado califica y carga la versión definitiva.
  - Un usuario sin rol válido no puede ser jurado.
- **`ManageTrabajoGradoGatewayImplAdapterTest` (+1):**
  - Guarda y recupera dos sesiones con jurados distintos, calificaciones con acta, la inasistencia, los plazos y el motivo.
  - Los jurados ven el trabajo entre los suyos.
- **`FlujoSustentacionIntegracionTest` (2, nueva):** por HTTP con un `Clock` ajustable (`@Primary`).
  - Flujo completo: con observaciones, ajustes verificados, versión definitiva y `FINALIZADO`, con 406/403 por permisos.
  - Inasistencia que aplaza, segunda sesión fuera de los 60 días rechazada y segunda inasistencia que deja `NO_APROBADO`.

Total del proyecto: **300 pruebas** (antes eran 274).

### Reloj de desarrollo
El flujo exige que pasen meses, así que para recorrerlo con Thunder hay un reloj ajustable:
- **`RelojAjustable`** (`infraestructure/configuration`) es el `Clock` de la aplicación: el del sistema más un desplazamiento que en producción siempre es cero.
- **`RelojController`** (`/api/v1/sgtg/reloj`, solo `ADMINISTRADOR`) solo existe con `sgtg.reloj.ajustable=true` (`SGTG_RELOJ_AJUSTABLE`). `docker-compose` lo activa por defecto; nunca debe activarse en producción. Endpoints:
  - `GET`: fecha actual.
  - `POST /avanzar {"dias": n}`.
  - `DELETE`: vuelve a la fecha real.
  - `POST /tareas/vencimientos`: ejecuta ahora la verificación diaria.
- **`RelojControllerIntegracionTest` (2):** adelantar, validar y restablecer el reloj, y que sin token no se puede. `FlujoPropuestaIntegracionTest` verifica que por defecto no existe (404).

### Thunder Client
Carpeta nueva **"6. Sustentación (reloj ajustable)"**, con 40 peticiones:
- **Reglas y permisos:** solicitud antes de los meses mínimos, sin paz y salvo, solo el director solicita, un docente no programa, no se programa sin solicitud, solo los jurados califican.
- **A. Investigación:** de la propuesta a `EN_CURSO`, +100 días, solicitud, programación (un jurado y un evaluador), calificaciones (rige `APROBADO_CON_OBSERVACIONES`), ajustes verificados y versión definitiva → `FINALIZADO`.
- **B. Práctica profesional:** +90 días, se exige el concepto del asesor, se aplaza, segunda sesión e inasistencia → `NO_APROBADO`.
- **C. Vencimiento:** +100 días; la verificación diaria cierra el trabajo de investigación de las carpetas 2 a 4 (`VENCIMIENTO_PLAZO`).
- Al final, y también al inicio de la colección, se restablece el reloj.

En total son 140 peticiones, validadas dos veces seguidas contra Docker con MySQL; también se verificó que MySQL crea las tablas y columnas de la sustentación.

### Qué no se prueba en esta fase
- La cancelación, que se permite desde cualquier etapa no final: Fase 8.

## Actualización 2026-10-13: jurado coordinador, nueva fecha y anexos

Decisiones del usuario al integrar el front (fase 6 del plan de integración):

1. **Solicitud:** el director adjunta la **monografía** (nueva, obligatoria) y el formato E como dos PDF, el paz y salvo, el concepto del asesor en práctica profesional y **anexos** opcionales. Decanatura no registra el paz y salvo.
2. **Programación:** el Consejo indica el **jurado coordinador** y el otro jurado, la fecha, hora y salón, y adjunta el **formato E (obligatorio)**. Los **evaluadores del anteproyecto no pueden ser jurados del mismo trabajo** (sí de otros). Al programar, ambos reciben el rol `JURADO` y el coordinador además `JURADO_COORDINADOR` (rol nuevo).
3. **Solo el coordinador** califica (con el **acta formato G obligatoria**), registra la inasistencia, verifica los ajustes y carga la versión definitiva; el otro jurado solo consulta (406 `solo_jurado_coordinador`). Ya no hay dos calificaciones ni regla de "la menos favorable": la del coordinador define el resultado, y una sola verificación aprueba o aplaza.
4. **Nueva fecha de sustentación:** antes de la fecha designada, el director la solicita con un PDF; el Consejo la decide con la resolución y, si la aprueba, indica la nueva fecha y hora (y opcionalmente otro salón): la sesión se reprograma con los mismos jurados. Mientras está pendiente no se registra la calificación ni la inasistencia.
5. **Ajustes:** el director puede adjuntar **anexos** (PDF) con los ajustes; las reglas se mantienen.

| Elemento | Cambio |
|---|---|
| `TipoDocumento` | Nuevos `MONOGRAFIA`, `ANEXOS`, `SOLICITUD_NUEVA_FECHA` |
| `RoleAdmited` / `RoleType` | Nuevo `JURADO_COORDINADOR`; `Evaluador` lo acepta |
| `SesionSustentacion` | `+ rutaFormatoE`; el primer jurado es el coordinador (`coordinador()`, `esCoordinador(id)`, `reprogramada(...)`) |
| `SolicitudNuevaFecha` (nuevo) | `(sesion, rutaSolicitud, fechaSolicitud, fechaHoraAnterior, decision, nuevaFechaHora, nuevoSalon)` |
| `Sustentacion` | `solicitar(monografía, E, F, concepto, anexos…)`, `solicitarNuevaFecha`, `resolverNuevaFecha`, `radicarAjustes(ruta, anexos, fecha)`; registros solo del coordinador |
| `CalificacionJurado` | Acta obligatoria (`acta_obligatoria`) |
| `Calificacion` | Sin `menosFavorable` |
| `POST /sustentacion` | Partes `monografia`, `documentoFinal`, `pazYSalvo`, `conceptoAsesor` (PP) y `anexos` (opcional) |
| `POST /sustentacion/programacion` | Ahora multipart: `datos` `{ juradoCoordinadorId, juradoId, fechaHora, salon }` y `archivo` (formato E) |
| `POST /sustentacion/nueva-fecha` (nuevo) | Director, parte `archivo` |
| `POST /sustentacion/nueva-fecha/decisiones-consejo` (nuevo) | `CONSEJO`, `datos` `{ resultado, fechaHora, salon, observaciones }` y `archivo` (resolución) |
| `POST /sustentacion/ajustes` | Parte opcional `anexos` |
| Persistencia | Columnas TEXT `sustentacion_ruta_monografia`, `_ruta_anexos`, `_ruta_anexos_ajustes`; `sustentacion_sesiones.ruta_formatoe`; tabla `sustentacion_nuevas_fechas` |
| Respuesta | `sustentacion` con `rutaMonografia`, `rutaAnexos`, `rutaAnexosAjustes`, `solicitudesNuevaFecha[]`; cada sesión con `juradoCoordinadorId` y `rutaFormatoE` |

Pruebas: `EtapaSustentacionTest` (25), `ManageTrabajoGradoCUImplAdapterTest` (+3), gateway y `FlujoSustentacionIntegracionTest` reescritos. Total del proyecto: 382. Thunder: 199 peticiones.

## Actualización 2026-10-14: paz y salvo de Decanatura
El director ya no adjunta el paz y salvo a la solicitud: lo revisa Decanatura (lo aprueba o no, siempre con el formato
F) y el Consejo no programa sin paz y salvo aprobado. Detalle en [ajustes-2026-10-14.md](ajustes-2026-10-14.md).
