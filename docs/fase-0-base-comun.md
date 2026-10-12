# Fase 0: Base común del dominio

Este documento describe las clases de la Fase 0 del dominio de trabajo de grado: qué representa cada una, qué reglas aplica y cómo se prueba. El plan completo y las decisiones de diseño están en [plan-dominio.md](plan-dominio.md).

La Fase 0 no implementa todavía el flujo del trabajo de grado (eso empieza en la Fase 1 con el agregado `TrabajoGrado`). Construye las piezas que todas las etapas van a usar:

- el mapa de etapas,
- los participantes,
- los documentos,
- las decisiones y conceptos,
- el manejo de fechas.

## Principios comunes

- **Dominio puro:** ninguna clase usa Spring ni JPA. Se prueban con JUnit sin base de datos.
- **Validación al construir:** si un objeto existe, sus datos son válidos. Las reglas incumplidas lanzan `ReglaNegocioException`, cuyo mensaje es una clave de `src/main/message.properties` (`error.dominio.*`).
- **Objetos de valor inmutables:** `Periodo`, `Documento`, `Decision`, `Concepto`, `CambioEtapa`, `Departamento` y `Programa` son `record`. Para "modificarlos" se crea uno nuevo (ej. `periodo.extenderMeses(3)` devuelve otro `Periodo`).
- **Fechas como parámetro:** ningún método llama a `LocalDate.now()`; la fecha actual se recibe como argumento. Así las pruebas pueden simular cualquier día.
- **Lenguaje del reglamento:** los nombres siguen la Resolución 231 de 2020 (APLAZADO, NO_APROBADO, Comité de Programa, Consejo de Facultad…).

## Paquetes

```
domain/
├── exceptions/ReglaNegocioException
├── services/CalendarioHabil
└── models/
    ├── trabajo_grado/   Proceso, MotivoCierre, Modalidad, TipoDocumento, Documento, Estado, CambioEtapa
    ├── participantes/   Participante, Estudiante, Director, Evaluador, Asesor,
    │                    PerfilDocente, PerfilEstudiante, TipoVinculacion, Departamento, Programa
    └── comun/           Periodo, Decision, Organo, Concepto, ResultadoConcepto
```

`Usuario` y `Role` (autenticación) siguen en `domain/models` y no cambiaron. `Propuesta` se movió a `trabajo_grado` sin modificarse; se reescribe en la Fase 1.

---

## 1. Excepciones y servicios

### `ReglaNegocioException`
Excepción no verificada que lanza el dominio cuando se incumple una regla. Su mensaje es la clave del texto en `message.properties`. Por ejemplo, `error.dominio.proceso.transicion_invalida` corresponde a "La transición entre las etapas indicadas no está permitida".

> Pendiente para infraestructura: registrarla en `RestApiException` para que responda con un error de negocio y no con un 500.

### `CalendarioHabil` (interfaz)
`LocalDate sumarDiasHabiles(LocalDate desde, int dias)`: calcula plazos en días hábiles (10 días del Comité, 10 de los evaluadores, 15 de los jurados, etc.).

El dominio solo define el contrato. La implementación con los festivos de Colombia pertenece a infraestructura, y en las pruebas se usará una versión simulada.

---

## 2. Paquete `trabajo_grado`

### `Proceso` (enum): las etapas del trabajo de grado

| Etapa | Significado | Artículo |
|---|---|---|
| `PROPUESTA` | Idea presentada al Comité de Programa; se queda aquí si no es aprobada | 10–11 |
| `ANTEPROYECTO` | Radicación ante el Departamento | 12–13 |
| `EVALUACION` | Dos evaluadores revisan; solo aprueban o piden correcciones | 13 |
| `APROBACION_CONSEJO` | Espera la resolución del Consejo de Facultad | 14 |
| `EN_CURSO` | Desarrollo: 9 meses desde la notificación | 15 |
| `PRORROGA` | Hasta 3 meses adicionales, una sola vez | 16 |
| `SUSPENDIDO` | Suspensión única, hasta un período académico | 27 |
| `SUSTENTACION` | Solicitud, programación y calificación | 19–23 |
| `APLAZADO` | Primera sustentación aplazada; nueva sesión en ≤ 60 días | 23 |
| `FINALIZADO` | Aprobado, con versión definitiva | 23 |
| `NO_APROBADO` | Cerrado por vencimiento, calificación o inasistencia | 16, 21, 23, 25 |
| `CANCELADO` | Cancelado por el Consejo de Facultad | 26 |

**Funcionalidades**
- `puedeAvanzarA(destino)`: indica si la transición existe en el mapa del proceso. No revisa condiciones del negocio; por ejemplo, "ambos evaluadores aprobaron" lo validará el agregado.
- `esFinal()`: verdadero para `FINALIZADO`, `NO_APROBADO` y `CANCELADO`, que no admiten más transiciones.

**Mapa de transiciones**
```
PROPUESTA ─► ANTEPROYECTO ─► EVALUACION ─► APROBACION_CONSEJO ─► EN_CURSO ─┬────────────► SUSTENTACION ─► FINALIZADO
                ▲   ▲            │                 │                       ├─► PRORROGA ─┘     │   ▲
                │   └────────────┘ (correcciones)  │                       │                   ▼   │
                └──────────────────────────────────┘ (Consejo no aprueba)  │                  APLAZADO
                                                      SUSPENDIDO ◄─────────┘ (vuelve a la etapa previa)

NO_APROBADO: desde EN_CURSO, PRORROGA, SUSTENTACION o APLAZADO.
CANCELADO:   desde cualquier etapa no final.
```

### `MotivoCierre` (enum)
Guarda **por qué** terminó un trabajo que no se aprobó. Cada motivo conoce su etapa final (`getEtapaCierre()`).

| Motivo | Etapa final |
|---|---|
| `VENCIMIENTO_PLAZO` | `NO_APROBADO` |
| `SUSTENTACION_NO_APROBADA` | `NO_APROBADO` |
| `INASISTENCIA_SUSTENTACION` | `NO_APROBADO` |
| `CANCELACION_ESTUDIANTE` | `CANCELADO` |
| `CANCELACION_DIRECTOR` | `CANCELADO` |

### `Modalidad` (enum)
`TRABAJO_DE_INVESTIGACION` (prefijo **TI**) y `PRACTICA_PROFESIONAL` (prefijo **PP**), según el Art. 3. El prefijo se usa para construir el código de los formatos.

### `TipoDocumento` (enum)
Tipos de documento que se cargan durante el proceso. Los que corresponden a un formato del reglamento conocen su letra:

| Tipo | Formato | Solo PP |
|---|---|---|
| `PROPUESTA` | A | |
| `ANTEPROYECTO` | — | |
| `EVALUACION_ANTEPROYECTO` | B | |
| `REMISION_CONSEJO` | C | |
| `INFORME_MENSUAL` | D | Sí |
| `DOCUMENTO_FINAL` | E | |
| `PAZ_Y_SALVO` | F | |
| `ACTA_SUSTENTACION` | G | |
| `VERSION_DEFINITIVA` | H | |
| `CARTA_ACEPTACION` | — | Sí |
| `RESOLUCION` | — | |

**Funcionalidades**
- `formato(modalidad)`: devuelve el código completo; por ejemplo, `PROPUESTA` en investigación es `"TI-A"`. Devuelve vacío si no es un formato o no aplica a la modalidad.
- `aplicaA(modalidad)`: indica si el documento se usa en esa modalidad. El informe mensual y la carta de aceptación solo existen en práctica profesional.

### `Documento` (record)
Una versión de un archivo cargado al servidor de archivos. **El dominio solo guarda la URL**, no el archivo.

`(TipoDocumento tipo, String url, int version, LocalDate fechaCarga)`

**Reglas**
- Tipo, URL y fecha son obligatorios.
- La URL debe ser `http` o `https` y tener servidor.
- La versión es mayor o igual a 1.

**Funcionalidades**
- `Documento.inicial(tipo, url, fecha)`: crea la versión 1.
- `nuevaVersion(url, fecha)`: crea la siguiente versión con el mismo tipo; se usa tras correcciones.

### `Estado` (enum)
Estado de **una entrega dentro de una etapa**. Es independiente de la etapa del trabajo: por ejemplo, el trabajo puede estar en `PROPUESTA` con la propuesta `NO_APROBADO`.

`EN_REVISION → CON_CORRECCIONES | APROBADO | RECHAZADO | NO_APROBADO`, y `CON_CORRECCIONES → EN_REVISION` al radicar una nueva versión. La entrega no tiene estado antes de enviarse; al enviarla queda `EN_REVISION` (desde 2026-10-11 ya no existe `RADICADO`).

**Funcionalidades:** `puedeCambiarA(destino)` y `esFinal()` (`APROBADO`, `RECHAZADO` y `NO_APROBADO`).

### `CambioEtapa` (record)
Una entrada del historial del trabajo: `(origen, destino, fecha y hora, responsable, observación)`.

**Reglas**
- Etapas, fecha y responsable son obligatorios.
- **Solo acepta transiciones válidas según `Proceso`.** El historial no puede registrar un salto imposible.

---

## 3. Paquete `participantes`

### Composición en lugar de herencia
Los participantes **no heredan de `Usuario`**. Una misma persona puede ser directora de un trabajo, evaluadora de otro y jurado de un tercero. Por eso cada participante representa **el papel de una persona en un trabajo concreto**:
- Referencia al usuario por `usuarioId`.
- Copia los datos que el dominio necesita (código, nombre como `NombrePersona` y correo).
- **Nunca copia la contraseña.**

```
Usuario (autenticación) ──usuarioId──► Participante ─┬─ Estudiante  (+ Programa)
        │                                            ├─ Director    (+ TipoVinculacion, Departamento)
        ├── PerfilDocente    (vinculación, depto.)   ├─ Evaluador
        └── PerfilEstudiante (programa)              └─ Asesor      (+ organización; usuario opcional)
```

### `Participante` (clase abstracta)
**Dos formas de construirse**
- **Desde un `Usuario`:** se usa al asignar a una persona a un trabajo. Valida que el usuario tenga alguno de los roles del sistema que habilitan ese papel.
- **Con los datos sueltos:** para que los mappers de infraestructura reconstruyan el objeto desde la base de datos.

**Funcionalidades**
- `esMismaPersona(otro)`: compara por `usuarioId`, o por código si alguno no tiene cuenta. Sirve para reglas como "el evaluador no puede ser el director".
- `tieneUsuario()`, `getNombreCompleto()` y los getters.

**Reglas:** código, nombre (primer nombre y primer apellido) y correo son obligatorios, y el usuario es obligatorio salvo para el asesor.

| Clase | Roles del sistema permitidos | Usuario | Datos propios |
|---|---|---|---|
| `Estudiante` | ESTUDIANTE | Obligatorio | `Programa` (desde `PerfilEstudiante`) |
| `Director` | DOCENTE, DIRECTOR, CODIRECTOR | Obligatorio | `TipoVinculacion` y `Departamento` (desde `PerfilDocente`) |
| `Evaluador` | DOCENTE, EVALUADOR, JURADO | Obligatorio | — (se usa para evaluadores y jurados) |
| `Asesor` | ASESOR, DOCENTE | **Opcional** | Organización (obligatoria si es externo) y bandera `externo` |

**Funcionalidades de `Director`**
- `requiereCodirector()`: verdadero si su vinculación no es de planta (Art. 6).
- `esDePlantaDe(departamento)`: verdadero si es de planta y está adscrito a ese departamento. Sirve para validar que el codirector exigido por el Art. 6 sea "profesor de planta del respectivo departamento".
- **Copia de la vinculación:** guarda la vinculación y el departamento **del momento de la asignación**. Si el docente cambia de vinculación después, el trabajo conserva la que tenía.

### `PerfilDocente` y `PerfilEstudiante`
Datos académicos separados de `Usuario`, que solo tiene información de autenticación. No se manejan como roles: los roles son permisos, mientras que la vinculación es un dato único por persona.

- **`PerfilDocente`:** `usuarioId`, vinculación vigente y departamento. Tiene `actualizarVinculacion()`, `cambiarDepartamento()` y `perteneceA(usuario)`.
- **`PerfilEstudiante`:** `usuarioId` y programa. Tiene `cambiarPrograma()` y `perteneceA(usuario)`. En la versión 2 tendrá los datos de elegibilidad (créditos, matrícula condicional).

Al crear un `Director` o un `Estudiante` desde su perfil, se valida que el perfil **pertenezca al mismo usuario**.

### `TipoVinculacion` (enum)
`PLANTA`, `OCASIONAL`, `CATEDRA` y `ESTUDIANTE_POSGRADO`, con `requiereCodirector()`. Es un enum porque los tipos los define el reglamento y el código depende de ellos.

### `Departamento` y `Programa` (records)
Catálogos `(id, nombre)` que se registran como datos, no como enum. Tienen `esMismo(otro)`, que compara por id.

---

## 4. Paquete `comun`

### `Periodo` (record)
Rango de fechas `(inicio, fin)`, con fin posterior al inicio. Es la base del control de tiempos del trabajo.

| Método | Para qué sirve | Regla que soporta |
|---|---|---|
| `Periodo.desde(inicio, meses)` | Crea el periodo de ejecución | 9 meses desde la aprobación (Art. 15) |
| `extenderMeses(meses)` | Extiende el fin | Prórroga de hasta 3 meses (Art. 16) |
| `diasRestantes(fecha)` | Días calendario hasta el fin (0 si ya pasó) | Solicitar la prórroga al menos 15 días antes; guardar el tiempo al suspender |
| `reanudarDesde(fecha, dias)` | Nuevo fin = reanudación + días que faltaban | Suspensión (Art. 27) |
| `haTranscurrido(meses, fecha)` | Tiempo mínimo cumplido | 3 meses para sustentar (Art. 20), 6 meses para cambiar objetivos (Art. 18) |
| `vencido(fecha)` | La fecha es posterior al fin | Vencimiento → NO_APROBADO (Art. 21) |
| `contiene(fecha)` | La fecha está dentro del periodo | — |

**Ejemplo de suspensión:** en un periodo de 9 meses, se suspende a los 3 meses con 6 meses restantes. Al reanudar, el nuevo fin es la fecha de reanudación más esos 6 meses (en días).

### `Organo` (enum) y `Decision` (record)
`Organo`: `COMITE_PROGRAMA`, `DEPARTAMENTO` y `CONSEJO_FACULTAD`.

`Decision (organo, resultado, acta, rutaArchivo, observaciones, fecha)` registra lo que decide un órgano colegiado. Por ejemplo: el concepto del Comité sobre la propuesta, o la resolución del Consejo sobre el anteproyecto, la prórroga, la suspensión o la cancelación.
- **Reglas:** órgano, resultado y fecha son obligatorios. Si no es aprobatoria, las observaciones son obligatorias. El acta es opcional.
- **Funcionalidades:** `esAprobatoria()` y `emitidaPor(organo)`.

### `Concepto` (record) y `ResultadoConcepto` (enum)
`Concepto (emisor, resultado, observaciones, fecha)` registra la opinión de **una persona**: evaluador, jurado o asesor. El emisor puede ser cualquier `Participante`.
- **Reglas:** emisor, resultado y fecha son obligatorios. Si no es aprobatorio, las observaciones son obligatorias.
- **Funcionalidades:** `esAprobado()` y `requiereCorrecciones()`.

`ResultadoConcepto`: `APROBADO`, `CON_CORRECCIONES`, `RECHAZADO`, `EVALUADO` y `NO_APROBADO`. La propuesta solo admite `APROBADO` y `NO_APROBADO`. Lo comparten `Decision` y `Concepto`. La sustentación tendrá su propia `Calificacion` en la Fase 7.

---

## 5. Plan de pruebas

### Cómo ejecutarlas
```bash
./mvnw test -Dtest='com.sgtg.backend.domain.**'
```
No requieren MySQL. `./mvnw test` sin filtro también ejecuta `BackendApplicationTests`, que sí levanta el contexto de Spring y necesita la base de datos.

### Estrategia
- **Pruebas unitarias puras** con JUnit 5, sin mocks ni Spring: el dominio no tiene dependencias externas.
- **Pruebas parametrizadas** (`@CsvSource`, `@EnumSource`) para recorrer tablas de reglas, como las transiciones de `Proceso` o los tipos de vinculación, sin repetir código.
- **Casos límite explícitos** en las reglas de fechas (el día exacto del límite y el día siguiente).
- **Caminos felices y de error:** cada regla se prueba cumpliéndose y violándose, y en el segundo caso se verifica que se lance `ReglaNegocioException`.

### Pruebas y cobertura

| Clase de prueba | Pruebas | Qué verifica |
|---|---|---|
| `trabajo_grado/ProcesoTest` | 49 | Las 20 transiciones válidas, 15 inválidas representativas, que toda etapa activa pueda cancelarse, que las etapas finales no admitan transiciones, y la etapa final de cada `MotivoCierre` |
| `trabajo_grado/EstadoTest` | 9 | Cambios válidos e inválidos del estado de una entrega |
| `trabajo_grado/DocumentoTest` | 10 | Versionado, 5 URLs inválidas (sin esquema, `ftp`, sin servidor…), datos incompletos, código de formato por modalidad, documentos exclusivos de PP y que `CambioEtapa` solo acepte transiciones válidas |
| `comun/PeriodoTest` | 8 | Creación por meses, extensión sin alterar el original, días restantes, suspensión y reanudación, límite exacto de `haTranscurrido`, `vencido` y `contiene` en los extremos |
| `comun/DecisionConceptoTest` | 5 | Observaciones obligatorias si no aprueba, datos obligatorios, órgano emisor y que asesor y evaluador puedan emitir conceptos |
| `participantes/ParticipantesTest` | 14 | Creación desde usuario y perfil sin copiar la contraseña, roles permitidos, que el perfil pertenezca al usuario, copia de la vinculación, regla del codirector de planta del mismo departamento, una persona con varios roles, asesor sin usuario, datos obligatorios y `requiereCodirector` por vinculación |
| **Total** | **95** | |

### Casos límite destacados

| Regla | Caso | Resultado esperado |
|---|---|---|
| Plazo de la prórroga | Fin el 15-oct; se consulta el 30-sep | 15 días restantes (permitido) |
| | Se consulta el 1-oct | 14 días restantes (no permitido) |
| Mínimo para sustentar | Inicio 15-ene; se consulta el 14-abr | No han pasado 3 meses |
| | Se consulta el 15-abr | Sí han pasado 3 meses |
| Vencimiento | Se consulta el mismo día del fin | No vencido |
| | Se consulta el día siguiente | Vencido |
| Suspensión | Se reanuda con los días guardados | `diasRestantes(reanudacion)` es igual a los días guardados |
| Vinculación | El perfil cambia de OCASIONAL a PLANTA después de asignar | El director sigue siendo OCASIONAL y requiere codirector |

### Qué no se prueba en esta fase
- **`CalendarioHabil`:** es solo una interfaz. Su versión simulada se probará con los plazos de la Fase 1 y su implementación real en infraestructura.
- **`Propuesta`:** sigue siendo la clase anterior; se reescribe y se prueba en la Fase 1.
- **Reglas del flujo del trabajo** (quién puede avanzar, en qué condiciones): corresponden al agregado `TrabajoGrado`, a partir de la Fase 1.
