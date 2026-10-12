# Fase 1: Propuesta y agregado `TrabajoGrado`

Este documento describe las clases de la Fase 1: qué representan, qué reglas aplican y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md), y las piezas base que se usan aquí (`Proceso`, `Documento`, `Decision`, participantes…) están descritas en [fase-0-base-comun.md](fase-0-base-comun.md).

En esta fase nace el **agregado `TrabajoGrado`**: un trabajo de grado se crea en la etapa `PROPUESTA`, radica su propuesta ante el Comité de Programa y, cuando el Comité la aprueba, pasa a `ANTEPROYECTO`.

Artículos de la Resolución 231 de 2020 que cubre: **6, 9, 10, 11, 29, 32 y 33**.

## Clases

```
domain/models/trabajo_grado/
├── TrabajoGrado           (abstracta, raíz del agregado)
│   ├── TrabajoInvestigacion
│   └── PracticaProfesional
└── Propuesta              (etapa dentro del agregado)
```

### `TrabajoGrado` (clase abstracta)
Raíz del agregado. Contiene todas las etapas del trabajo y es **la única clase que cambia la etapa**. Cada cambio queda registrado en el historial (`CambioEtapa`).

| Atributo | Descripción |
|---|---|
| `id` | Identificador; `null` mientras no se haya guardado |
| `titulo`, `objetivos` | Descripción del trabajo (Art. 10) |
| `estudiantes` | Quienes realizan el trabajo |
| `director` | Director, obligatorio |
| `codirector` | Opcional; obligatorio en ciertos casos (ver reglas) |
| `asesor` | Opcional en investigación, obligatorio en práctica profesional |
| `etapa` | Etapa actual (`Proceso`); inicia en `PROPUESTA` |
| `propuesta` | Etapa de propuesta |
| `historial` | Lista de `CambioEtapa` |
| `motivoCierre` | Por qué se cerró sin aprobarse; `null` mientras esté activo (se usará desde la Fase 4) |

`getModalidad()` es abstracto: lo define cada subclase.

### `TrabajoInvestigacion`
Modalidad de investigación (Capítulo II). Admite **1 o 2 estudiantes** (Art. 9) y un asesor opcional, que puede ser interno o externo (Art. 10).

### `PracticaProfesional`
Modalidad de práctica profesional (Capítulo III).
- Es **individual** (Art. 29): el constructor recibe un solo `Estudiante` y `getEstudiante()` lo devuelve.
- Exige un **asesor de la organización** (con organización registrada) y la **carta de aceptación** como `Documento` de tipo `CARTA_ACEPTACION` (Art. 32).

### `Propuesta`
Etapa de propuesta dentro del agregado (Arts. 10–11).

| Atributo | Descripción |
|---|---|
| `versiones` | Documentos radicados (formato A), numerados desde 1 |
| `decisiones` | Decisiones del Comité de Programa, una por versión evaluada |
| `estado` | `Estado` de la versión actual; `null` mientras no se haya radicado |
| `fechaLimiteConcepto` | Radicación + 10 días hábiles (Art. 11) |

**Sus métodos de modificación (`radicar`, `registrarDecision`) son de paquete**: solo `TrabajoGrado` puede llamarlos. Desde fuera solo se puede consultar: `estaAprobada()`, `versionActual()`, `conceptoVencido(hoy)` y los getters.

---

## Reglas de creación

Se validan en el constructor: si un `TrabajoGrado` existe, cumple todas estas reglas. Cada regla incumplida lanza `ReglaNegocioException` con la clave indicada (prefijo `error.dominio.`).

| Regla | Artículo | Clave del error |
|---|---|---|
| El título es obligatorio | 10 | `trabajo_grado.titulo_obligatorio` |
| Al menos un objetivo, ninguno vacío | 10 | `trabajo_grado.objetivos_obligatorios` |
| Al menos un estudiante | — | `trabajo_grado.estudiantes_obligatorios` |
| Un estudiante no puede estar dos veces | — | `trabajo_grado.estudiante_repetido` |
| El director es obligatorio | — | `trabajo_grado.director_obligatorio` |
| Si el director no es de planta (temporal o estudiante de posgrado), se exige un codirector **de planta del mismo departamento** | 6 | `trabajo_grado.codirector_de_planta_obligatorio` |
| El codirector siempre es **docente de la universidad**: nunca un estudiante de posgrado ni una persona externa | 6 (decisión del proyecto) | `trabajo_grado.codirector_debe_ser_docente` |
| El codirector no puede ser el director | — | `trabajo_grado.codirector_igual_director` |
| Ni el director ni el codirector pueden ser estudiantes del trabajo | — | `trabajo_grado.director_es_estudiante` |
| Investigación: máximo 2 estudiantes | 9 | `investigacion.cantidad_estudiantes` |
| Práctica profesional: asesor con organización | 32 | `practica.asesor_obligatorio` |
| Práctica profesional: carta de aceptación | 32 | `practica.carta_aceptacion_obligatoria` |

Que el codirector no pueda ser externo lo garantiza el modelo: `Director` siempre se crea a partir de un `Usuario` con rol docente y su `PerfilDocente`. La validación adicional excluye a los estudiantes de posgrado, que también pueden tener un perfil docente para poder dirigir.

---

## Operaciones del agregado

### `radicarPropuesta(url, hoy, calendario)`
Radica una versión de la propuesta ante el Comité de Programa.
- Solo en la etapa `PROPUESTA`.
- **El agregado crea el `Documento`** (tipo `PROPUESTA`) y numera la versión: quien llama solo envía la URL del archivo ya cargado al servidor.
- Se permite la primera vez y después de un `NO_APROBADO`. **No** se permite mientras haya una versión pendiente de concepto ni si la propuesta ya fue aprobada.
- Calcula la fecha límite del Comité con `CalendarioHabil` (10 días hábiles).

### `registrarDecisionComite(decision, responsable, ahora)`
Registra el concepto del Comité de Programa sobre la versión actual.
- Solo en la etapa `PROPUESTA`, y debe haber una versión pendiente de concepto.
- La decisión debe ser del **Comité de Programa** (`Organo.COMITE_PROGRAMA`) y con fecha igual o posterior a la radicación.
- Resultados aceptados: **solo `APROBADO` y `NO_APROBADO`**. Cualquier otro valor de `ResultadoConcepto` se rechaza (`propuesta.resultado_invalido`).
- La decisión puede llevar la ruta del archivo de soporte (`rutaArchivo`, por ejemplo el acta) en el servidor de archivos.

| Resultado | Estado de la propuesta | Etapa del trabajo |
|---|---|---|
| `APROBADO` | `APROBADO` | Avanza a `ANTEPROYECTO` y se registra el `CambioEtapa` con el responsable y la fecha |
| `NO_APROBADO` | `NO_APROBADO` | Sigue en `PROPUESTA`; se radica una nueva versión |

### `Propuesta.conceptoVencido(hoy)`
Indica si el Comité superó su plazo de 10 días hábiles mientras la versión sigue pendiente. **Es informativo**, para alertas: el reglamento no establece una consecuencia, así que no cambia la etapa.

### Flujo de estados de la propuesta
El estado pertenece a **cada versión** (`VersionPropuesta`: documento + estado) y se asigna automáticamente: al radicar (enviar) la versión queda **`EN_REVISION`** ante el Comité, y su concepto la deja **`APROBADO`** o **`NO_APROBADO`**. Cada versión conserva el resultado de su revisión (por ejemplo, v1 `NO_APROBADO` y v2 `APROBADO`). Solo la versión `EN_REVISION` recibe concepto; tras un `NO_APROBADO` se radica una versión nueva. `Propuesta.getEstado()` devuelve el estado de la versión actual (`null` si no se ha radicado ninguna).

En el API el estado va en `propuesta.versiones[i].estado`; la propuesta no tiene un `estado` propio.

```
            radicar                          decisión del Comité
(sin radicar) ──────► EN_REVISION ──────────────────────────────┬──► APROBADO ──► trabajo pasa a ANTEPROYECTO
                         ▲                                      └──► NO_APROBADO ──────┐
                         │            radicar nueva versión                            │
                         └─────────────────────────────────────────────────────────────┘
```

---

## Decisiones de diseño

- **Validar antes de modificar.** En `registrarDecisionComite`, si la decisión es aprobatoria, el `CambioEtapa` se construye (y por lo tanto se valida) **antes** de modificar la propuesta. Si falla (por ejemplo, sin responsable), ni la propuesta ni la etapa cambian. El agregado nunca queda a medias.
- **El historial solo registra transiciones válidas.** `CambioEtapa` rechaza saltos que `Proceso` no permite, y `aplicarCambio` verifica además que el origen coincida con la etapa actual.
- **Las etapas solo se modifican desde el agregado.** Los métodos que cambian `Propuesta` son de paquete, y las listas que expone el agregado (`getEstudiantes()`, `getObjetivos()`, `getHistorial()`, `getVersiones()`…) no se pueden modificar desde fuera.
- **No aprobada como entrega nueva.** En `Estado`, `NO_APROBADO` es final para *esa entrega*. La siguiente radicación inicia una entrega nueva, como versión siguiente del mismo documento. Así se cumple la regla de presentar una nueva propuesta sin forzar la máquina de estados.
- **Sin estado `RADICADO`.** Radicar la deja directamente `EN_REVISION` (2026-10-11): el Comité la revisa desde que se envía. El anteproyecto sigue la misma regla y `RADICADO` se retiró de `Estado`.

### Reconstrucción del agregado
Para guardar y recuperar un `TrabajoGrado` de la base de datos, los mappers lo reconstruyen con su etapa, historial y etapas ya cargadas **sin repetir las reglas de creación**. Por ejemplo, no se exige de nuevo un codirector si el docente ya cambió a planta.

Se implementó al empezar la persistencia:
- Los records `DatosTrabajoGrado` y `DatosPropuesta` contienen el estado persistido.
- `TrabajoGrado.reconstruir(datos)` crea la subclase según la modalidad, con constructores de paquete que no validan las reglas de creación.
- `ReconstruccionTrabajoGradoTest` (4 pruebas) verifica la reconstrucción y que el trabajo reconstruido siga operando.

Cada fase que agregue estado al agregado debe ampliar estos records, la entidad `TrabajoGradoEntity` y `MapperTrabajoGrado`.

---

## Plan de pruebas

### Cómo ejecutarlas
```bash
./mvnw test -Dtest='com.sgtg.backend.domain.**'
```

### Apoyo para las pruebas
- **`CalendarioHabilFalso`** (`src/test/.../domain/services`): considera hábiles los días de lunes a viernes, sin festivos. Las fechas esperadas se pueden calcular a mano y lo reutilizarán las fases siguientes.
- **`DatosPrueba`** (`src/test/.../trabajo_grado`): fábrica de datos de prueba (estudiantes, directores por vinculación y departamento, asesor de organización, carta de aceptación, decisiones del Comité y un trabajo de investigación válido). Las fechas parten del lunes 2 de marzo de 2026.
- Cada prueba de error verifica **la clave exacta** del mensaje, no solo el tipo de excepción. Así se confirma que falló la regla esperada y no otra.

### `CreacionTrabajoGradoTest` (12 pruebas)

| Prueba | Qué verifica |
|---|---|
| `trabajoNuevoIniciaEnPropuestaSinHistorial` | Etapa `PROPUESTA`, modalidad, historial vacío, propuesta sin radicar, sin motivo de cierre |
| `investigacionAdmiteUnoODosEstudiantes` | 1 y 2 son válidos; 3 y 0 se rechazan |
| `unEstudianteNoPuedeEstarDosVeces` | El mismo usuario dos veces en la lista |
| `tituloObjetivosYDirectorSonObligatorios` | Título en blanco, sin objetivos, objetivo en blanco, sin director |
| `directorTemporalRequiereCodirectorDePlantaDelMismoDepartamento` | Sin codirector, codirector de otro departamento y codirector no de planta se rechazan; de planta del mismo departamento es válido |
| `estudianteDePosgradoComoDirectorRequiereCodirector` | Director estudiante de posgrado sin codirector |
| `directorDePlantaPuedeTenerCodirectorOpcional` | Sin codirector o con un codirector docente temporal de otro departamento |
| `codirectorSoloPuedeSerDocenteDeLaUniversidad` | Un estudiante de posgrado como codirector se rechaza |
| `codirectorYEstudiantesDebenSerPersonasDistintasAlDirector` | Codirector igual al director; director que también es estudiante |
| `practicaProfesionalEsIndividualConAsesorYCarta` | Caso válido con un estudiante |
| `practicaProfesionalRechazaDatosFaltantes` | Sin estudiante, sin asesor, sin carta, o con un documento de otro tipo como carta |
| `lasListasExpuestasNoSePuedenModificar` | Estudiantes, objetivos e historial son de solo lectura |

### `EtapaPropuestaTest` (12 pruebas)

| Prueba | Qué verifica |
|---|---|
| `radicarCreaLaPrimeraVersionYCalculaElPlazoDelComite` | Versión 1, tipo `PROPUESTA`, estado `EN_REVISION`, fecha límite del 16-mar-2026 (10 días hábiles desde el lunes 2-mar) |
| `propuestaAprobadaAvanzaAAnteproyectoYQuedaEnElHistorial` | Etapa `ANTEPROYECTO` y `CambioEtapa` con origen, destino, responsable y fecha |
| `noAprobadaSeRadicaUnaNuevaVersionHastaSerAprobada` | Ciclo completo con 2 versiones y 2 decisiones; el historial solo registra el avance final |
| `elComiteSoloApruebaONoAprueba` | Cada resultado distinto de `APROBADO`/`NO_APROBADO` se rechaza y la propuesta queda sin cambios |
| `laDecisionGuardaLaRutaDelArchivoDeSoporte` | La ruta del archivo queda en la decisión |
| `noSeRadicaMientrasHayUnaVersionPendienteDeConcepto` | Doble radicación sin decisión |
| `noHayDecisionSinPropuestaRadicada` | Decisión antes de radicar |
| `soloElComiteDeProgramaEmiteConceptoSobreLaPropuesta` | Decisión del Consejo de Facultad |
| `laDecisionNoPuedeSerAnteriorALaRadicacion` | Decisión con fecha previa a la radicación |
| `sinResponsableNoSeAvanzaNiSeModificaLaPropuesta` | Validar antes de modificar: etapa, estado y decisiones quedan intactos |
| `despuesDeAprobadaNoSeOperaSobreLaPropuesta` | Radicar o decidir en `ANTEPROYECTO` |
| `radicarRequiereUnaUrlValida` | URL sin esquema; la propuesta queda sin radicar |
| `conceptoVencidoSoloMientrasEstaPendiente` | 16-mar no vencido, 17-mar vencido, y deja de estarlo cuando hay decisión |

### Casos límite destacados

| Regla | Caso | Resultado esperado |
|---|---|---|
| Plazo del Comité | Radicación el lunes 2-mar-2026 | Fecha límite 16-mar (salta dos fines de semana) |
| | Consulta el 16-mar | No vencido |
| | Consulta el 17-mar | Vencido |
| Fecha de la decisión | Un día antes de la radicación | Se rechaza |
| Atomicidad | Decisión aprobatoria sin responsable | Excepción; nada cambia |
| Codirector | Director de planta con codirector de cátedra de otro departamento | Válido (el codirector solo es obligatorio si el director no es de planta) |

### Total del dominio
Con esta fase el dominio tiene **119 pruebas**: 95 de la Fase 0 y 24 de la Fase 1 (12 de creación y 12 de propuesta).

### Qué no se prueba en esta fase
- **Quién puede ejecutar cada operación** (por ejemplo, que solo el director radique): por ahora lo controlará el caso de uso con los roles del sistema. Si se decide llevarlo al dominio, se agregará como regla del agregado.
- **Elegibilidad del estudiante y límite de 5 trabajos por director:** quedan para la versión 2.
