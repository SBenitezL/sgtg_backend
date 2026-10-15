# Notificaciones, avisos de plazos y anulación

Este documento describe lo que se agregó para la fase 7 de la integración con el front (transversales): el buzón de
notificaciones, los avisos de plazos de la tarea diaria, la anulación del trabajo por el director y la consulta general
de Decanatura. Las decisiones son del usuario (2026-10-14) y están también en el plan de integración del front
(`sgtg_frontend/docs/plan-integracion-fases.md`, §8).

## Notificaciones

### Dominio
- `Notificacion` (record): destinatario (`usuarioId`), `tipo` (`INFO`, `CONFIRMACION`, `ALERTA`, `ERROR`), título,
  mensaje, trabajo y etapa del trabajo al notificar (el front decide a qué vista llevar), `clave` (solo en los avisos que
  no se deben repetir), fecha y si ya se leyó.
- `EventoTrabajoGrado` (enum): cada hecho del trabajo con su tipo, título, texto y destinatarios. El texto recibe el
  título del trabajo y un detalle (un resultado, la fecha de la sesión, la justificación de la anulación…).
- `Destinatario` (enum): `ESTUDIANTES`, `DIRECCION` (director, codirector y asesor con usuario), `EVALUADORES`,
  `EVALUADORES_PENDIENTES` (los que no han emitido concepto sobre la versión actual), `JURADOS` (de la sesión actual),
  `COMITE`, `JEFE_DEP` (solo el jefe del departamento del director), `CONSEJO` y `DECANATURA` (usuarios activos con el
  rol).

### Generación
`NotificadorTrabajoGrado` (en `domain/use_cases`) crea las notificaciones:
- **Por operación:** cada caso de uso del trabajo de grado notifica su hecho después de guardar. Quien realiza la
  acción no recibe la notificación. Si la operación falla no se notifica, y una falla al notificar no deshace la
  operación (se registra en el log).
- **Tarea diaria:** los cierres y cambios de etapa que aplica (`TRABAJO_CERRADO_POR_PLAZO`, `ETAPA_ACTUALIZADA`) y los
  avisos de plazos:

| Etapa | Plazo | Aviso | Destinatarios |
|---|---|---|---|
| Propuesta en revisión | Concepto del Comité | 3 días antes y al vencer | Comité |
| Evaluación del anteproyecto | Concepto de los evaluadores | 3 días antes y al vencer | Evaluadores que no han emitido concepto |
| En curso o prórroga | Fin del plazo de ejecución | 30 días antes | Estudiantes y dirección |

Cada aviso lleva la clave `<EVENTO>:<trabajoId>:<fecha límite>` y se envía una sola vez por usuario; si el plazo cambia
(p. ej. por una prórroga), se avisa de nuevo para la nueva fecha.

### API (`/api/v1/sgtg/notificaciones`, usuario autenticado)
| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/notificaciones` | Las del usuario, de la última creada a la primera |
| `PATCH` | `/notificaciones/{id}/leida` | La notificación leída (406 si es de otro usuario, 404 si no existe) |
| `PATCH` | `/notificaciones/leidas` | 204: marca todas como leídas |
| `DELETE` | `/notificaciones/{id}` | 204 (406 si es de otro usuario) |
| `DELETE` | `/notificaciones` | 204: vacía el buzón |

Persistencia: tabla `notificaciones` con índices por usuario y por usuario y clave.

## Anulación

`POST /api/v1/sgtg/trabajos-grado/{id}/anulacion` con `{"justificacion": "..."}`. Es un **borrado lógico**: el trabajo
queda `CANCELADO` con motivo `ANULACION_DIRECTOR` y la justificación como observación del cambio de etapa; el front lo
muestra en el historial.

- Solo el director (406 para los demás) y con justificación (400).
- Solo en `PROPUESTA`, `ANTEPROYECTO`, `EVALUACION` o `APROBACION_CONSEJO`; después de la aprobación del Consejo se
  solicita la cancelación (fase 8 del dominio). Mensaje: `error.dominio.anulacion.etapa_invalida`.
- Notifica a estudiantes, dirección y evaluadores (`TRABAJO_ANULADO`).

"Abandonado" no es un estado nuevo: es el cierre por vencimiento de plazos o la anulación.

## Consulta general de Decanatura

`DECANATURA` se agregó a los roles de consulta general: lista todos los trabajos (`GET /trabajos-grado`), consulta su
detalle y descarga sus archivos (estadísticas e historial).

## Pruebas
- `NotificadorTrabajoGradoTest`: destinatarios, textos, exclusión del actor, jefe del departamento, fallas sin
  interrumpir, cierre del sistema y avisos de plazos (umbrales, vencido y una sola vez).
- `ManageNotificacionCUImplAdapterTest`: cada usuario solo gestiona sus notificaciones.
- `ManageTrabajoGradoCUImplAdapterTest`: anulación, notificación de cada hecho con el actor, sin notificación si la
  operación falla, Decanatura y avisos de la tarea diaria.
- `EtapaCancelacionTest`: reglas de la anulación en el dominio.
- `FlujoPropuestaIntegracionTest`: buzón y anulación por HTTP.
- Thunder: carpeta 8 (216 peticiones en total).
