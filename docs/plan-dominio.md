# Plan de desarrollo del dominio: Trabajo de Grado

Fuente de las reglas: **Resolución 8.4.3-90.2/231 de 2020** del Consejo de Facultad FIET (Universidad del Cauca), que reglamenta las modalidades Trabajo de Investigación (TI) y Práctica Profesional (PP). Los artículos citados abajo son de esa resolución.

El alcance actual es **solo la capa de dominio** (`domain/`): POJOs y records sin Spring ni JPA, probados con JUnit puro.

## Estado por fase

| Fase | Etapa | Estado |
|---|---|---|
| 0 | Base común | Completa |
| 1 | Propuesta | Completa |
| 2 | Anteproyecto y evaluación | Pendiente |
| 3 | Aprobación del Consejo | Pendiente |
| 4 | En curso | Pendiente |
| 5 | Prórroga | Pendiente |
| 6 | Suspensión | Pendiente |
| 7 | Sustentación | Pendiente |
| 8 | Cancelación | Pendiente |

## Decisiones de diseño

- **Codirector:** siempre es un docente de la universidad (planta, ocasional o cátedra); nunca un estudiante de posgrado ni una persona externa.
- **Agregado:** la raíz es `TrabajoGrado`, una clase abstracta con dos subclases: `TrabajoInvestigacion` y `PracticaProfesional`. Cada etapa es una entidad dentro del agregado, y solo el agregado cambia de etapa.
- **Lenguaje:** los nombres de etapas, calificaciones y motivos siguen el vocabulario del reglamento (APLAZADO, NO_APROBADO, Consejo de Facultad, Comité de Programa, Departamento).
- **Etapas finales:** `FINALIZADO`, `NO_APROBADO` y `CANCELADO`. La causa se guarda en `MotivoCierre`. No existe `ABANDONADO`: el reglamento trata el vencimiento de plazos como NO APROBADO.
- **Un NO APROBADO cierra el trabajo.** El estudiante inicia un trabajo de grado **nuevo**; no se vuelve a `PROPUESTA` dentro del mismo.
- **Concepto sobre la propuesta:** el Comité de Programa solo emite `APROBADO` o `NO_APROBADO`. Si no es aprobada, sigue en `PROPUESTA` y se radica una nueva versión.
- **Decisiones con archivo de soporte:** toda `Decision` puede llevar la ruta de un archivo (ej. el acta) en el servidor de archivos (`rutaArchivo`, opcional).
- **Usuarios:** tienen tipo de documento (`TipoDocumentoIdentidad`: CC, CE, PASAPORTE) y número de documento (solo dígitos, único junto con el tipo), y el nombre separado en primer y segundo nombre, primer y segundo apellido (los segundos son opcionales). Los participantes copian el nombre como `NombrePersona`.
- **Sin devoluciones manuales.** Solo se retrocede cuando el reglamento lo indica: correcciones del anteproyecto, Consejo que no aprueba y reanudación tras una suspensión.
- **Participantes por composición, no herencia de `Usuario`.** Una persona puede cumplir roles distintos en trabajos distintos (director en uno, evaluador en otro). Cada `Participante` referencia al usuario por `usuarioId` y copia los datos que necesita el dominio, nunca la contraseña.
  - Al crearse desde un `Usuario` se valida que tenga un rol del sistema permitido: `Estudiante` ← ESTUDIANTE; `Director` ← DOCENTE, DIRECTOR o CODIRECTOR; `Evaluador` ← DOCENTE, EVALUADOR o JURADO; `Asesor` ← ASESOR o DOCENTE.
  - Solo `Asesor` puede existir sin usuario (externo sin cuenta).
  - Los datos académicos viven en **perfiles** separados de `Usuario`: `PerfilDocente` (vinculación vigente y departamento) y `PerfilEstudiante` (programa). No se usan roles para esto, porque los roles son permisos y la vinculación es un dato único por persona.
  - `TipoVinculacion` es un enum porque el reglamento lo define y el código depende de él. `Departamento` y `Programa` son catálogos que se registran como datos.
  - El perfil guarda solo la vinculación vigente, sin historial. `Director` y `Estudiante` copian los datos del perfil al asignarse, así el trabajo conserva cómo estaban en ese momento.
  - Los permisos sobre un trabajo concreto ("solo su director puede…") se validan contra el agregado, no contra el rol global.
- **Documentos:** se cargan a un servidor de archivos; el dominio solo guarda la **URL** (http o https) en `Documento`.
- **Plazos:** los que tienen consecuencia en el proceso los aplica el dominio. Los de órganos o docentes (Comité, evaluadores, jurados) solo guardan la fecha límite, para alertas.
- **Días hábiles:** se calculan con la interfaz `domain/services/CalendarioHabil`; la implementación con festivos va en infraestructura.
- **Prórroga:** se cuenta en **días calendario** (al menos 15 antes del fin).
- **Suspensión:** congela el tiempo restante. Al reanudar, fin = fecha de reanudación + días que faltaban.
- **Vencimientos:** `TrabajoGrado.verificarVencimientos(hoy)` aplica las reglas; una tarea programada en infraestructura lo invoca.
- **Errores:** el dominio lanza `ReglaNegocioException` con claves de `src/main/message.properties` (`error.dominio.*`).

### Casos no definidos por el reglamento (decididos)

1. Una prórroga **negada** consume la única oportunidad.
2. Una suspensión no reanudada al llegar su fecha máxima se **reanuda automáticamente** en `verificarVencimientos`.
3. Si el Consejo **no aprueba** el anteproyecto, vuelve a `ANTEPROYECTO`.
4. Si pasan los 60 días de `APLAZADO` sin segunda sesión, el trabajo queda `NO_APROBADO` con motivo `VENCIMIENTO_PLAZO`.

## Paquetes

```
domain/models/trabajo_grado/   agregado, etapas, Proceso, Documento, CambioEtapa y enums del proceso
domain/models/participantes/   Participante, Estudiante, Director, Evaluador, Asesor, PerfilDocente, PerfilEstudiante,
                               TipoVinculacion, Departamento, Programa
domain/models/comun/           Periodo, Decision, Organo, Concepto, ResultadoConcepto
domain/services/               CalendarioHabil
domain/exceptions/             ReglaNegocioException
```

`Documento` y `CambioEtapa` están en `trabajo_grado` porque dependen de `TipoDocumento` y `Proceso`.

## Transiciones (`Proceso`)

```
PROPUESTA ─► ANTEPROYECTO ─► EVALUACION ─► APROBACION_CONSEJO ─► EN_CURSO ─┬────────────► SUSTENTACION ─► FINALIZADO
                ▲   ▲            │                 │                       ├─► PRORROGA ─┘     │   ▲
                │   └────────────┘ (correcciones)  │                       │                   ▼   │
                └──────────────────────────────────┘ (Consejo no aprueba)  │                  APLAZADO
                                                      SUSPENDIDO ◄─────────┘ (vuelve a la etapa previa)

NO_APROBADO: desde EN_CURSO, PRORROGA, SUSTENTACION o APLAZADO.
CANCELADO:   desde cualquier etapa no final.
```

---

## Fase 0: Base común (completa)

Detalle de clases, funcionalidades y plan de pruebas: [fase-0-base-comun.md](fase-0-base-comun.md).

| Clase | Paquete | Descripción |
|---|---|---|
| `Proceso` | trabajo_grado | Etapas y `puedeAvanzarA()` / `esFinal()` |
| `MotivoCierre` | trabajo_grado | `VENCIMIENTO_PLAZO`, `SUSTENTACION_NO_APROBADA`, `INASISTENCIA_SUSTENTACION`, `CANCELACION_ESTUDIANTE`, `CANCELACION_DIRECTOR`; cada uno conoce su etapa final |
| `Modalidad` | trabajo_grado | `TRABAJO_DE_INVESTIGACION` (TI), `PRACTICA_PROFESIONAL` (PP) |
| `TipoDocumento` | trabajo_grado | Tipos con su letra de formato (A–H); `formato(Modalidad)` → "TI-A"; `aplicaA(Modalidad)` |
| `Documento` | trabajo_grado | `(tipo, url, version, fechaCarga)`, valida la URL |
| `Estado` | trabajo_grado | Estado de una entrega dentro de una etapa |
| `CambioEtapa` | trabajo_grado | Historial; solo acepta transiciones válidas |
| `Organo` | comun | `COMITE_PROGRAMA`, `DEPARTAMENTO`, `CONSEJO_FACULTAD` |
| `Decision` | comun | Decisión de un órgano (resultado, acta, ruta del archivo de soporte, observaciones, fecha) |
| `NombrePersona` | comun | Primer y segundo nombre, primer y segundo apellido (los segundos opcionales) |
| `Concepto` | comun | Opinión de una persona (evaluador, jurado o asesor) |
| `ResultadoConcepto` | comun | `APROBADO`, `CON_CORRECCIONES`, `RECHAZADO`, `EVALUADO`, `NO_APROBADO`. La propuesta solo admite `APROBADO` y `NO_APROBADO` |
| `Periodo` | comun | Rango de fechas: `desde`, `extenderMeses`, `diasRestantes`, `reanudarDesde`, `haTranscurrido`, `vencido` |
| `TipoVinculacion` | participantes | `PLANTA`, `OCASIONAL`, `CATEDRA`, `ESTUDIANTE_POSGRADO`; `requiereCodirector()` (Art. 6) |
| `Departamento`, `Programa` | participantes | Catálogos (`record` id + nombre); se registran como datos, no como enum |
| `PerfilDocente` | participantes | `usuarioId`, vinculación vigente y departamento. Separado de `Usuario` (autenticación) |
| `PerfilEstudiante` | participantes | `usuarioId` y programa. En la v2 tendrá los datos de elegibilidad |
| `Participante` | participantes | Rol de una persona en un trabajo; referencia a `Usuario` por `usuarioId`. Dos constructores: desde `Usuario` (valida roles) y de reconstrucción (para mappers) |
| `Estudiante`, `Director`, `Evaluador` | participantes | Requieren usuario. `Estudiante` y `Director` se crean desde `Usuario` + su perfil y copian programa, o vinculación y departamento, del momento de la asignación. `Director` tiene `requiereCodirector()` y `esDePlantaDe(departamento)` |
| `Asesor` | participantes | Usuario opcional; organización obligatoria si es externo |
| `CalendarioHabil` | services | `sumarDiasHabiles(desde, dias)` |

## Fase 1: Propuesta (Arts. 9–11, 29, 32–33) (completa)

Detalle de clases, reglas, operaciones y plan de pruebas: [fase-1-propuesta.md](fase-1-propuesta.md).

**Reglas**
- TI admite 1 o 2 estudiantes; PP exactamente 1, con asesor y carta de aceptación (URL).
- Si el director no es de planta, se exige un codirector de planta del mismo departamento (`Director.esDePlantaDe`).
- El Comité de Programa emite el concepto; la fecha límite es de 10 días hábiles.
- El Comité emite `APROBADO` o `NO_APROBADO`. Si no es aprobada, el trabajo sigue en `PROPUESTA` y se radica una nueva versión.

**Clases**
- `TrabajoGrado` (abstracta): título, objetivos, estudiantes, director, codirector, asesor, etapa, propuesta, historial y motivo de cierre. Valida al crearse: título y objetivos obligatorios, sin estudiantes repetidos, director obligatorio, codirector de planta del mismo departamento si el director no es de planta, codirector siempre docente de la universidad (no estudiante de posgrado) y distinto del director, y que ni director ni codirector sean estudiantes del trabajo.
- `TrabajoInvestigacion`: de 1 a 2 estudiantes; asesor opcional.
- `PracticaProfesional`: un solo estudiante, asesor con organización y `Documento` de tipo `CARTA_ACEPTACION`.
- `Propuesta`: versiones (formato A), decisiones del Comité, `Estado` de la versión actual y fecha límite del concepto (10 días hábiles). Sus métodos de modificación son de paquete: solo el agregado los invoca.

**Operaciones**
- Constructores de `TrabajoInvestigacion` y `PracticaProfesional`: el trabajo inicia en `PROPUESTA`.
- `radicarPropuesta(url, hoy, calendario)`: el agregado crea el `Documento` y numera la versión. Se permite la primera vez o después de un `NO_APROBADO`.
- `registrarDecisionComite(decision, responsable, ahora)`: solo acepta decisiones del Comité de Programa con resultado `APROBADO` o `NO_APROBADO`; la decisión puede llevar la ruta del archivo de soporte. Si es aprobatoria, avanza a `ANTEPROYECTO` y registra el `CambioEtapa`. El cambio se valida antes de modificar la propuesta, para no dejar el agregado a medias.
- `Propuesta.conceptoVencido(hoy)`: informativo, para alertas; no cambia la etapa.

**Reconstrucción desde persistencia:** implementada al iniciar la persistencia con `DatosTrabajoGrado` / `DatosPropuesta` y `TrabajoGrado.reconstruir(...)`, que no repite las reglas de creación. Cada fase que agregue estado al agregado debe ampliar esos records, la entidad `TrabajoGradoEntity` y `MapperTrabajoGrado`.

## Fase 2: Anteproyecto y evaluación (Arts. 12–13, 37–38)

**Reglas**
- El Departamento asigna 2 evaluadores distintos, que no pueden ser el director ni el codirector. Tienen 10 días hábiles.
- El anteproyecto no se rechaza.
- Con correcciones vuelve a `ANTEPROYECTO`: el director tiene 10 días hábiles para reenviarlo y se abre una nueva ronda.
- Si ambos evaluadores aprueban, pasa a `APROBACION_CONSEJO`.

**Clases:** `Anteproyecto` (versiones y rondas), `EvaluacionAnteproyecto` (2 evaluadores, conceptos, fechas; no admite `RECHAZADO`).

**Operaciones:** `radicarAnteproyecto`, `asignarEvaluadores(e1, e2, hoy, calendario)`, `registrarConceptoEvaluador(Concepto, hoy, calendario)`.

## Fase 3: Aprobación del Consejo (Art. 14)

**Reglas**
- La resolución del Consejo aprueba el anteproyecto.
- Desde la fecha de notificación empiezan los plazos (9 meses).
- Si el Consejo no aprueba, vuelve a `ANTEPROYECTO`.

**Clases:** `Decision` (resolución), `Ejecucion` (se crea aquí con `Periodo.desde(notificacion, 9)`).

**Operación:** `aprobarAnteproyecto(Decision, fechaNotificacion)`.

## Fase 4: En curso (Arts. 15, 17–18, 20–21, 43–45)

**Reglas**
- Cambio de título u objetivos: una sola vez, dentro de los primeros 6 meses, con decisión del Consejo.
- PP: informes mensuales con el aval del asesor.
- Vencimiento sin iniciar la sustentación → `NO_APROBADO` por `VENCIMIENTO_PLAZO`.

**Clases:** `Ejecucion`, `CambioObjetivos`, `InformeMensual` (solo PP).

**Operaciones:** `cambiarObjetivos`, `registrarInformeMensual`, `verificarVencimientos(hoy, calendario)`.

## Fase 5: Prórroga (Arts. 16, 41)

**Reglas**
- La solicita el director, una sola vez (una negada también cuenta), por máximo 3 meses.
- Debe pedirse con al menos 15 días calendario antes del fin.
- La decide el Consejo; si la aprueba, pasa a `PRORROGA` y se extiende el fin.

**Clases:** `Prorroga` (meses, fecha de solicitud, `Decision`, estado `SOLICITADA`/`APROBADA`/`NEGADA`).

**Operaciones:** `solicitarProrroga(meses, hoy)`, `resolverProrroga(Decision)`.

## Fase 6: Suspensión (Arts. 27, 57)

**Reglas**
- Una sola vez, hasta un período académico, con decisión del Consejo.
- Se puede suspender desde `EN_CURSO` o `PRORROGA`, y se vuelve a la misma etapa.
- Al suspender se guardan los días restantes; al reanudar, fin = reanudación + esos días.
- Si llega la fecha máxima sin reanudar, se reanuda automáticamente.

**Clases:** `Suspension` (etapa previa, fecha de inicio, días restantes, fecha máxima de reanudación, fecha de reanudación, `Decision`).

**Operaciones:** `suspender(Decision, fechaMaximaReanudacion, hoy)`, `reanudar(hoy)`.

## Fase 7: Sustentación (Arts. 19–25, 46–55)

**Reglas**
- Se solicita con el documento final (formato E), después de al menos 3 meses desde la aprobación y antes del vencimiento.
- PP exige además el concepto favorable del asesor.
- El Consejo asigna 2 jurados, fecha, hora y salón, con el paz y salvo (formato F).

| Calificación | Sesión 1 | Sesión 2 |
|---|---|---|
| `APROBADO` | Versión definitiva (H) → `FINALIZADO` | Igual |
| `APROBADO_CON_OBSERVACIONES` | 10 días hábiles para ajustes; si no se cumplen → `APLAZADO` | Igual |
| `APLAZADO` o inasistencia | `APLAZADO`, nueva sesión en ≤ 60 días calendario | `NO_APROBADO` |
| `NO_APROBADO` | `NO_APROBADO` | `NO_APROBADO` |

**Clases:** `Sustentacion` (sesión 1 o 2, documento final, paz y salvo, 2 jurados, fecha y hora, salón, calificación, observaciones, fecha límite de ajustes, asistencia, versión definitiva), `Calificacion`.

**Operaciones:** `solicitarSustentacion`, `registrarConceptoAsesor`, `programarSustentacion`, `calificarSustentacion`, `registrarInasistencia`, `cargarVersionDefinitiva`.

## Fase 8: Cancelación (Arts. 26, 56)

**Reglas**
- La solicita el estudiante (con visto bueno del director) o el director (con visto bueno del Comité).
- La decide el Consejo y se permite desde cualquier etapa no final.

**Clases:** `Cancelacion` (quién solicita, justificación, `Decision`).

**Operación:** `cancelar(Cancelacion)` → `CANCELADO`.

## Versión 2 (fuera de alcance)

- Elegibilidad del estudiante: ≥ 80% de créditos (Art. 8), sin matrícula condicional (Art. 5), trabajo de grado matriculado (Art. 14).
- Máximo 5 trabajos simultáneos por director (Art. 6).
- Segunda y única oportunidad tras un NO APROBADO (Arts. 16, 21).
- PP: no repetir la modalidad tras un NO APROBADO (Art. 50 §7).

Irán en un servicio de dominio, `ElegibilidadTrabajoGrado`, que recibe los conteos desde los casos de uso.
