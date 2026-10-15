# Fase 8: Cancelación

Este documento describe la Fase 8, la última del plan: clases del dominio, reglas, operaciones, la API y cómo se prueban. El plan completo está en [plan-dominio.md](plan-dominio.md).

El director solicita la cancelación del trabajo con el formato de solicitud adjunto, desde **cualquier etapa no final**. El Consejo de Facultad la decide con la resolución adjunta:

- **Aprueba:** el trabajo queda `CANCELADO` con motivo `CANCELACION_DIRECTOR`.
- **No aprueba:** el trabajo sigue su curso y se puede volver a solicitar.

Mientras la solicitud está pendiente, **el trabajo sigue normalmente**: puede avanzar de etapa e incluso cerrarse por otro motivo.

Artículos de la Resolución 231 de 2020 que cubre: **26 y 56**.

## Decisiones

| # | Decisión | Origen |
|---|---|---|
| 1 | Solo el director solicita la cancelación. | Usuario |
| 2 | La solicitud lleva un formato obligatorio (ruta del documento). El visto bueno no se registra aparte en el sistema. | Usuario |
| 3 | El Consejo decide igual que en las fases anteriores (ruta de la resolución obligatoria, comentario opcional). Si no aprueba, se puede volver a solicitar. | Usuario |
| 4 | Mientras está pendiente, el trabajo sigue su curso. | Usuario |
| 5 | Solo puede haber una solicitud pendiente a la vez. Si el trabajo se cierra por otro motivo antes de la decisión, la solicitud ya no se puede decidir. | Propia |

`MotivoCierre.CANCELACION_ESTUDIANTE` se conserva en el enum, pero no se usa, porque solo el director solicita.

Antes de la aprobación del Consejo el director también puede **anular** el trabajo sin decisión del Consejo (motivo
`ANULACION_DIRECTOR`); ver [notificaciones-y-anulacion.md](notificaciones-y-anulacion.md).

## Clases del dominio

### `Cancelacion` (record)
Componentes: `rutaSolicitud`, `fechaSolicitud`, `etapaAlSolicitar` y `decision`. La decisión es `null` mientras está pendiente.

Al crearse valida:
- Que tenga el formato (`documento_obligatorio`).

Si trae decisión, también valida:
- Que la emita el Consejo (`organo_invalido`).
- Que tenga la ruta de la resolución (`resolucion_obligatoria`).
- Que el resultado sea `APROBADO` o `NO_APROBADO` (`resultado_invalido`).
- Que su fecha no sea anterior a la solicitud (`fecha_invalida`).

Métodos: `solicitar(...)`, `resolver(decision)`, `estaPendiente()` y `fueAprobada()`.

### `TrabajoGrado` (cambios)
| Operación | Regla |
|---|---|
| `solicitarCancelacion(rutaSolicitud, hoy)` | Etapa no final (`etapa_invalida`). Sin otra pendiente (`solicitud_pendiente`). Guarda la etapa en la que se solicitó |
| `resolverCancelacion(decision, responsable, ahora)` | Etapa no final; exige una solicitud pendiente (`sin_solicitud_pendiente`). Si se aprueba: `→ CANCELADO` con motivo `CANCELACION_DIRECTOR`. Valida antes de modificar |
| `cancelacionPendiente()`, `getCancelaciones()` | Consultas |

`DatosTrabajoGrado` tiene un componente nuevo, `cancelaciones`, después de `sustentacion`. El constructor abreviado (sin sustentación ni cancelaciones) se conserva.

## Aplicación e infraestructura

| Método y ruta (`/api/v1/sgtg/trabajos-grado`) | Permiso | Cuerpo |
|---|---|---|
| `POST /{id}/cancelacion` | Director (caso de uso) | `{ "rutaDocumento": "solicitudes/2026/cancelacion.pdf" }` |
| `POST /{id}/cancelacion/decisiones-consejo` | `CONSEJO` | `{ "resultado": "APROBADO", "rutaResolucion": "...", "observaciones": "opcional" }` |

- Las fechas son las del día (`Clock`).
- **Respuesta:** incluye `cancelaciones`: ruta, fecha, etapa al solicitar y `decision`.
- **Persistencia:** tabla `trabajo_cancelaciones` (`CancelacionEmbeddable`, con la decisión en columnas `decision_*`).

## Plan de pruebas

```bash
./mvnw test -Dtest=EtapaCancelacionTest                       # dominio
./mvnw test -Dtest='ManageTrabajoGrado*Test'                  # caso de uso y persistencia
./mvnw test -Dtest=FlujoAnteproyectoIntegracionTest           # punta a punta por HTTP
```

### `EtapaCancelacionTest` (9 pruebas, dominio)
| Prueba | Qué verifica |
|---|---|
| `aprobadaCancelaElTrabajoDesdeCualquierEtapaNoFinal` | Desde `PROPUESTA`: `CANCELADO`, motivo, historial y rutas |
| `negadaElTrabajoSigueYSePuedeVolverASolicitar` | Sin comentario; nueva solicitud |
| `mientrasEstaPendienteElTrabajoSigueSuCurso` | Avanza a `ANTEPROYECTO` y se cancela desde ahí |
| `soloHayUnaSolicitudPendienteALaVez` | `solicitud_pendiente` |
| `laSolicitudExigeElFormato` | `documento_obligatorio` |
| `laDecisionDelConsejoExigeSolicitudResolucionYResultadoValido` | Sin solicitud, sin ruta, otro órgano, resultado inválido, fecha anterior |
| `unTrabajoCerradoNoSeCancela` | `etapa_invalida` |
| `unaCancelacionPendienteSinDecidirCaducaSiElTrabajoSeCierra` | El plazo vence con la solicitud pendiente y ya no se puede decidir |
| `lasCancelacionesSeReconstruyen` | `DatosTrabajoGrado` con cancelaciones |

### Otras pruebas
- **`ManageTrabajoGradoCUImplAdapterTest` (+1):** solo el director solicita; el Consejo aprueba con la fecha del reloj.
- **`ManageTrabajoGradoGatewayImplAdapterTest` (+1):** guarda una cancelación negada y otra pendiente, y luego se aprueba.
- **`FlujoAnteproyectoIntegracionTest` (+1):** por HTTP se verifica:
  - Un estudiante recibe 406.
  - Sin formato responde 400.
  - No se permite una segunda solicitud pendiente.
  - El trabajo sigue mientras la solicitud está pendiente.
  - Un docente recibe 403.
  - El Consejo la niega, se solicita de nuevo y la aprueba (`CANCELADO`).

Total del proyecto: **315 pruebas** (antes eran 303).

### Thunder Client
Carpeta nueva **"7. Cancelación"**, con 11 peticiones. Recorre el ciclo completo sobre un trabajo nuevo y verifica que no se cancela el trabajo `FINALIZADO` de la carpeta 6.

En total son 151 peticiones en 7 carpetas, validadas dos veces seguidas contra Docker con MySQL.
