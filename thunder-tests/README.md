# Pruebas de integración con Thunder Client

Colección que recorre la API de las Fases 0 a 8 contra el backend en ejecución. Cada petición verifica el código de respuesta y los datos devueltos, y guarda en variables los tokens e identificadores que usan las siguientes.

## Archivos
- `sgtg-coleccion.json`: colección con 151 peticiones en 7 carpetas.
- `sgtg-entorno-local.json`: entorno con `baseUrl` y las credenciales del administrador de desarrollo (las mismas del archivo `.env`).

## Importar (una sola vez)
1. En VS Code abre Thunder Client (icono del rayo en la barra lateral).
2. Pestaña **Collections** → menú `≡` → **Import** → selecciona `sgtg-coleccion.json`.
3. Pestaña **Env** → menú `≡` → **Import** → selecciona `sgtg-entorno-local.json`.
4. En la pestaña **Env**, marca **SGTG local** como entorno activo. Ahí se guardarán los tokens e ids.

## Ejecutar
1. Levanta el backend: `docker compose up --build -d`.
2. En **Collections**, menú `…` de la colección → **Run All**. Las peticiones se ejecutan en orden y el resultado muestra qué pruebas pasaron.

La carpeta 6 adelanta la fecha del backend con el **reloj de desarrollo** (`/api/v1/sgtg/reloj`, solo para el administrador), que existe únicamente con `SGTG_RELOJ_AJUSTABLE=true`. `docker-compose` lo activa por defecto; nunca debe activarse en producción. La colección lo restablece a la fecha real al inicio (carpeta 1) y al final (carpeta 6).

Se puede ejecutar varias veces sobre la misma base de datos: los códigos, correos y nombres usan `{{#guid}}` y los números de documento `{{#number}}{{#number}}` (hasta 12 dígitos), así que no chocan con datos de ejecuciones anteriores.

## Qué cubre
| Carpeta | Contenido |
|---|---|
| 1. Preparación (administrador) | Login del administrador; crea departamento, programa, docente, estudiante, miembro del comité, jefe de departamento, dos evaluadores, miembro del Consejo de Facultad y jurado; restablece el reloj; registra los perfiles (el jefe con el mismo departamento del docente) |
| 2. Flujo de la propuesta | El docente crea el trabajo (queda como director) y radica la propuesta; el comité no la aprueba (con ruta del acta); se radica la versión 2 y el comité la aprueba (pasa a `ANTEPROYECTO`); el estudiante consulta su trabajo |
| 3. Flujo del anteproyecto y aprobación del Consejo | El director radica el anteproyecto; el jefe lo ve en la consulta por etapa y asigna los evaluadores (pasa a `EVALUACION`); un evaluador aprueba con formato B y el otro pide correcciones (vuelve a `ANTEPROYECTO`); el director radica la versión 2 (vuelve a `EVALUACION`); ambos aprueban (pasa a `APROBACION_CONSEJO`); el Consejo no aprueba sin comentario (vuelve a `ANTEPROYECTO`); se radica la versión 3, los evaluadores la aprueban y el Consejo la aprueba con la resolución (pasa a `EN_CURSO` con su plazo); el evaluador ve el trabajo en sus trabajos |
| 4. Trabajo en curso | Cambio de título: el Consejo no lo aprueba, el director lo solicita de nuevo y el Consejo lo aprueba (el trabajo adopta el nuevo título); flujo completo de una práctica profesional hasta `EN_CURSO`, dos informes mensuales, prórroga de 3 meses aprobada por el Consejo (pasa a `PRORROGA`) y un informe durante la prórroga; suspensión negada, nueva solicitud aprobada (`SUSPENDIDO`, sin informes) y reanudación del director (vuelve a `PRORROGA`) |
| 5. Permisos y reglas de negocio | 401 sin token; 403 por rol; solo el director radica; regla de etapa; solo `JEFE_DEP` asigna evaluadores; solo los evaluadores asignados emiten concepto; solo `CONSEJO` decide, con la ruta de la resolución obligatoria y solo en su etapa; solo el director solicita el cambio de objetivos y solo una vez; informes solo en práctica y con documento; prórroga de 1 a 3 meses, solo del director, decidida por el Consejo, con solicitud pendiente y no estando ya en prórroga; suspensión solo del director, con documento, decidida por el Consejo, una sola vez, y reanudación solo de un trabajo suspendido; consulta por etapa restringida y con etapa válida; codirector de planta (Art. 6); rol inexistente; número de documento con letras; documento repetido; no existe registro público |
| 6. Sustentación (reloj ajustable) | Reglas y permisos (meses mínimos, paz y salvo, solo el director solicita, solo el Consejo programa y solo con solicitud, solo los jurados califican). **A:** un trabajo de investigación llega a `EN_CURSO`, se adelanta el reloj 100 días, el director solicita, el Consejo programa (un jurado y un evaluador del anteproyecto), los jurados califican (rige `APROBADO_CON_OBSERVACIONES`), el director radica los ajustes, los jurados los verifican y uno carga la versión definitiva (`FINALIZADO`). **B:** la práctica profesional (en prórroga), +90 días, exige el concepto del asesor, se aplaza, se programa la segunda sesión y la inasistencia la deja `NO_APROBADO`. **C:** +100 días, la verificación diaria cierra por vencimiento el trabajo de investigación de las carpetas 2 a 4. Al final restablece el reloj |
| 7. Cancelación | Sobre un trabajo nuevo: solo el director solicita y con formato; una sola pendiente; el trabajo sigue mientras tanto; un docente no decide; el Consejo la niega, se solicita de nuevo y la aprueba (`CANCELADO`, motivo `CANCELACION_DIRECTOR`); un trabajo finalizado no se cancela |

La reanudación automática de la suspensión no se prueba aquí; la cubren las pruebas automáticas.

Las carpetas dependen de las anteriores (usan sus variables), así que deben ejecutarse en orden. Para ejecutar una sola petición, primero hay que ejecutar las que guardan las variables que usa.
