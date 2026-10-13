# Fase 4: Trabajo en curso

Este documento describe la Fase 4: clases del dominio, reglas, operaciones, la API, la tarea programada y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md). La `Ejecucion` nace en la Fase 3: ver [fase-3-aprobacion-consejo.md](fase-3-aprobacion-consejo.md).

Con el anteproyecto aprobado por el Consejo, el trabajo está `EN_CURSO` y corre el plazo de la modalidad. En esta etapa hay tres cosas:

1. **Cambio de título u objetivos:** el director lo solicita y el Consejo de Facultad lo decide con la resolución adjunta. Se hace una sola vez, dentro de los primeros 6 meses.
2. **Informes mensuales:** solo en práctica profesional. Los radica el director con el documento adjunto. *(Reemplazado el 2026-10-12: ver la sección final.)*
3. **Vencimiento:** una tarea programada diaria cierra como `NO_APROBADO` (motivo `VENCIMIENTO_PLAZO`) los trabajos cuyo plazo terminó sin iniciar la sustentación.

Artículos de la Resolución 231 de 2020 que cubre: **15, 17, 18, 21 y 45**.

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| 1 | El cambio de título u objetivos lo decide el Consejo de Facultad con resolución adjunta (ruta obligatoria, comentario opcional), igual que en la Fase 3. | Usuario |
| 2 | Los informes mensuales los radica el director con el documento adjunto; solo se guarda la ruta. No hay un paso de aval del asesor en el sistema. | Usuario |
| 3 | El vencimiento lo aplica una tarea programada. | Usuario |
| 4 | Un cambio **no aprobado no consume** la única oportunidad: el reglamento limita los cambios realizados, no las solicitudes. Solo puede haber una solicitud pendiente a la vez. | Propia |
| 5 | La solicitud exige justificación, y el título o los objetivos propuestos deben diferir de los vigentes. Cambiar solo el título o solo los objetivos es válido. | Propia |
| 6 | El plazo de 6 meses se cuenta hasta la fecha de la **solicitud**, desde la notificación y con ese día incluido. La decisión del Consejo puede llegar después. | Propia |
| 7 | Los informes se numeran de forma consecutiva y se aceptan en `EN_CURSO` y en `PRORROGA`. No se limita la cantidad por mes. | Propia |

## Clases del dominio

```
domain/models/trabajo_grado/
├── CambioObjetivos   (record: solicitud y decisión del Consejo)
├── InformeMensual    (record: consecutivo, ruta y fecha)
└── Ejecucion         (+ cambiosObjetivos, informesMensuales)
```

### `CambioObjetivos`
Componentes: `tituloAnterior`, `objetivosAnteriores`, `tituloPropuesto`, `objetivosPropuestos`, `justificacion`, `fechaSolicitud` y `decision`. La decisión es `null` mientras está pendiente.

Al crearse valida:
- La descripción propuesta (`descripcion_obligatoria`).
- La justificación (`datos_incompletos`).
- Que haya un cambio real (`sin_cambios`).

Si trae decisión, también valida:
- Que la emita el Consejo (`organo_invalido`).
- Que tenga la ruta de la resolución (`resolucion_obligatoria`).
- Que el resultado sea `APROBADO` o `NO_APROBADO` (`resultado_invalido`).
- Que su fecha no sea anterior a la solicitud (`fecha_invalida`).

Métodos: `solicitar(...)`, `resolver(decision)`, `estaPendiente()` y `fueAprobado()`.

### `InformeMensual (numero, rutaArchivo, fecha)`
La ruta es obligatoria (`documento_obligatorio`) y se recorta.

### `Ejecucion` (cambios)
- **`solicitarCambioObjetivos(solicitud)`** rechaza la solicitud en estos casos:
  - Hay otra pendiente (`solicitud_pendiente`).
  - Ya hay un cambio aprobado (`ya_realizado`).
  - La fecha cae fuera de [notificación, notificación + 6 meses] (`fuera_de_plazo`).
- **`resolverCambioObjetivos(decision)`** aplica la decisión a la solicitud pendiente (`sin_solicitud_pendiente` si no la hay). Como la decisión se valida al crear la solicitud resuelta, no se modifica nada si es inválida.
- **`registrarInformeMensual(ruta, hoy)`** asigna el siguiente consecutivo y no acepta fechas anteriores a la notificación (`fecha_invalida`).
- **Consultas:** `cambioPendiente()`, `fechaLimiteCambioObjetivos()`, `getCambiosObjetivos()` y `getInformesMensuales()`. `DatosEjecucion` también incluye las dos listas.

### `TrabajoGrado` (cambios)
| Operación | Regla |
|---|---|
| `solicitarCambioObjetivos(titulo, objetivos, justificacion, hoy)` | Solo `EN_CURSO`; guarda la descripción vigente como "anterior" |
| `resolverCambioObjetivos(decision)` | Solo `EN_CURSO`; si aprueba, el trabajo adopta el título y los objetivos propuestos. La etapa no cambia |
| `registrarInformeMensual(ruta, hoy)` | Solo práctica profesional (`informe_mensual.solo_practica`), en `EN_CURSO` o `PRORROGA` |
| `verificarVencimientos(ahora)` | Si está en `EN_CURSO` o `PRORROGA` y el plazo venció, cierra el trabajo en `NO_APROBADO` con motivo `VENCIMIENTO_PLAZO`. El responsable del cambio es `SISTEMA`. Devuelve `true` si cambió |

El método privado `cerrar(motivo, responsable, ahora, observacion)` lo reutilizarán la sustentación y la cancelación.

## Aplicación e infraestructura

| Método y ruta (`/api/v1/sgtg/trabajos-grado`) | Permiso | Cuerpo |
|---|---|---|
| `POST /{id}/cambio-objetivos` | Director (caso de uso) | `{ "titulo": "...", "objetivos": ["..."], "justificacion": "..." }` |
| `POST /{id}/cambio-objetivos/decisiones-consejo` | `CONSEJO` | `{ "resultado": "APROBADO", "rutaResolucion": "...", "observaciones": "opcional" }` |
| `POST /{id}/informes-mensuales` | Director (caso de uso) | `{ "rutaDocumento": "informes/2026/abril.pdf" }` |

- Las fechas (solicitud, decisión, informe) son las del día (`Clock`).
- **Respuesta:** el bloque `ejecucion` ahora incluye `fechaLimiteCambioObjetivos`, `cambiosObjetivos` (con su `decision`) e `informesMensuales`.

### Tarea programada de vencimientos
- **`VencimientosScheduler`** (`infraestructure/input/scheduler`) invoca `verificarVencimientos()` del caso de uso. Se ejecuta todos los días a las **00:05, hora de Colombia**.
  - La hora se configura con `sgtg.vencimientos.cron` (variable de entorno `SGTG_VENCIMIENTOS_CRON`, formato cron de Spring: segundo, minuto, hora, día, mes, día de la semana).
  - Registra en el log cuántos trabajos cerró y no propaga errores.
- **`SchedulingConfig`** habilita `@EnableScheduling`.
- **El caso de uso** recorre los trabajos en `EN_CURSO` y `PRORROGA` y guarda solo los que cerró.

### Persistencia
- `ejecucion_cambios_objetivos` (`CambioObjetivosEmbeddable`):
  - Los objetivos anteriores y propuestos se guardan como JSON con `ListaTextoConverter`, porque JPA no permite anidar una lista dentro de una colección de valores.
  - Esas columnas son `@Lob` (texto largo): como `VARCHAR` grandes superaban el límite de tamaño de fila de MySQL (65 535 bytes). H2 no lo detecta; se encontró al validar contra Docker.
  - La decisión va embebida con columnas `decision_*`.
- `ejecucion_informes_mensuales` (`InformeMensualEmbeddable`).

## Plan de pruebas

```bash
./mvnw test -Dtest=EtapaEnCursoTest                           # dominio
./mvnw test -Dtest='ManageTrabajoGrado*Test'                  # caso de uso y persistencia
./mvnw test -Dtest='FlujoAnteproyectoIntegracionTest,VencimientosSchedulerTest'
```

### `EtapaEnCursoTest` (14 pruebas, dominio)
| Prueba | Qué verifica |
|---|---|
| `elConsejoApruebaElCambioYElTrabajoAdoptaLaNuevaDescripcion` | Título y objetivos nuevos; se conservan los anteriores y la resolución |
| `unCambioNoAprobadoNoConsumeLaOportunidad` | Sin comentario; se puede volver a solicitar |
| `elCambioSoloSeRealizaUnaVez` | `ya_realizado` |
| `noSeSolicitaOtroCambioMientrasHayaUnoPendiente` | `solicitud_pendiente` |
| `soloSeSolicitaDentroDeLosPrimerosSeisMeses` | Límite 20-sep-2026 incluido; antes de la notificación o después del límite: `fuera_de_plazo` |
| `laSolicitudDebeCambiarAlgoYEstarJustificada` | `sin_cambios`, `datos_incompletos`, `descripcion_obligatoria`; cambiar solo el título es válido |
| `laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido` | Sin solicitud, sin ruta, otro órgano, resultado inválido, fecha anterior; nada se modifica |
| `elCambioSoloSeSolicitaEnCurso` | `etapa_invalida` |
| `laPracticaProfesionalRegistraInformesConsecutivos` | Consecutivos 1 y 2; ruta recortada |
| `losInformesSoloAplicanALaPracticaProfesional` | `solo_practica` |
| `elInformeExigeElDocumentoYUnaFechaValida` | `documento_obligatorio`, `fecha_invalida` |
| `alVencerElPlazoSinSustentacionQuedaNoAprobado` | Hasta el día final no vence; al día siguiente queda `NO_APROBADO` con motivo y responsable `SISTEMA`; una segunda verificación no hace nada |
| `elVencimientoSoloAplicaAlTrabajoEnDesarrollo` | Un trabajo en propuesta no se afecta |
| `laEjecucionReconstruidaConservaCambiosEInformes` | `DatosEjecucion` de ida y vuelta |

### Otras pruebas
- **`ManageTrabajoGradoCUImplAdapterTest` (+3):**
  - Solo el director solicita, y la decisión del Consejo usa la fecha del reloj.
  - Solo el director radica informes, y solo en práctica profesional.
  - La verificación diaria cierra el trabajo vencido, guarda solo ese y deja el vigente.
- **`ManageTrabajoGradoGatewayImplAdapterTest` (+1):** una práctica en curso guarda y recupera un cambio no aprobado, uno pendiente (objetivos en JSON) y dos informes.
- **`FlujoAnteproyectoIntegracionTest`:** después de la aprobación del Consejo, se prueba por HTTP:
  - Solicitud del director (un estudiante recibe 406).
  - Decisión del Consejo (un docente recibe 403).
  - Que el cambio se haga una sola vez.
  - Informes solo en práctica profesional.
- **`VencimientosSchedulerTest` (2):** delega en el caso de uso y un error no detiene la tarea.

Total del proyecto: **235 pruebas** (antes eran 215).

### Thunder Client
- **Carpeta nueva "4. Trabajo en curso":**
  - Cambio de título: no aprobado, nueva solicitud y aprobado.
  - Flujo completo de una práctica profesional hasta `EN_CURSO`, con dos informes mensuales.
- **Carpeta "5. Permisos y reglas de negocio":** gana 5 pruebas.

En total son 79 peticiones, validadas contra Docker con MySQL. El vencimiento no se prueba en Thunder porque depende del paso del tiempo; lo cubren las pruebas de dominio y de caso de uso.

### Qué no se prueba en esta fase
- La prórroga, que extiende el plazo y mantiene los informes en `PRORROGA`: Fase 5.
- La solicitud de sustentación, que detiene el vencimiento: Fase 7.

## Actualización 2026-10-12: informes mensuales en ambas modalidades y revisiones

Decisiones del usuario al integrar el front (fases 4 y 5):

1. Los informes mensuales (formato D) aplican **también a los trabajos de investigación**. Los radica un
   **estudiante** del trabajo; en práctica profesional también el **director**. Quien radica puede agregar
   observaciones (opcionales).
2. El director, el codirector o el asesor **revisan** cada informe con observaciones y/o un PDF de notas
   (tipo `REVISION_INFORME`). Es una formalidad: **no cambia la etapa** del trabajo. Una revisión por revisor.

Cambios:

| Elemento | Cambio |
|---|---|
| `TipoDocumento.INFORME_MENSUAL` | Ya no es solo de práctica profesional (`TI-D` / `PP-D`). Nuevo `REVISION_INFORME`. |
| `InformeMensual` | `(numero, rutaArchivo, fecha, radicadoPor, observaciones, revisiones)`; `revisadoPor(id)`, `conRevision(rev)`. |
| `RevisionInforme` (nuevo) | `(revisorUsuarioId, revisor, observaciones, rutaArchivo, fecha)`; exige observaciones o archivo (`revision_vacia`). |
| `Ejecucion.revisarInformeMensual(numero, revision)` | `no_encontrado`, `revision_repetida`, `fecha_revision_invalida`. |
| `TrabajoGrado` | Se quitó `solo_practica`; `registrarInformeMensual(ruta, hoy, radicadoPor, observaciones)` y `revisarInformeMensual(numero, revision)`. |
| Caso de uso | Radica: estudiante, o director en práctica profesional (`solo_radicador_informe`, 406). Revisa: director, codirector o asesor (`solo_revisor_informe`, 406). |
| `POST /{id}/informes-mensuales` | Parte `archivo` (formato D) y parte opcional `datos` `{ "observaciones": "..." }`. |
| `POST /{id}/informes-mensuales/{numero}/revisiones` (nuevo) | Partes opcionales `datos` `{ "observaciones": "..." }` y `archivo` (notas); al menos una. |
| Persistencia | `InformeMensualEmbeddable` + `radicadoPor`, `observaciones`; tabla `ejecucion_informes_revisiones` (`RevisionInformeEmbeddable`). |
| Respuesta | `informesMensuales[]` incluye `radicadoPor`, `observaciones` y `revisiones[]`. |

Thunder (carpeta 5): en investigación el director recibe 406; la estudiante radica con observaciones; la
estudiante no revisa (406); el director revisa con notas sin cambiar la etapa; una sola revisión por revisor (400).
