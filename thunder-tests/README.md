# Pruebas de integración con Thunder Client

Colección que recorre la API de las Fases 0 a 8 contra el backend en ejecución. Cada petición verifica el código de respuesta y los datos devueltos, y guarda en variables los tokens e identificadores que usan las siguientes.

## Archivos
- `sgtg-coleccion.json`: colección con 236 peticiones en 8 carpetas.
- `archivos/`: los PDF que adjuntan las peticiones (formato A, actas, resoluciones, informes, solicitudes, etc.) y `documento.txt`, que se usa para comprobar que solo se aceptan PDF.
- `sgtg-entorno-local.json`: entorno con `baseUrl` y las credenciales del administrador de desarrollo (las mismas del archivo `.env`).

## Documentos (multipart)
Las operaciones que llevan un documento son `multipart/form-data`, igual que en el front:
- el PDF va en la parte `archivo` (en la solicitud de sustentación, en `documentoFinal`, `pazYSalvo` y `conceptoAsesor`);
- los demás datos van como JSON en la parte `datos`. En Thunder el campo se llama `datos=application/json`: lo que va después del `=` es el *Content-Type* de la parte, que el backend necesita para leerla como JSON.

El backend guarda cada archivo en `<trabajoId>/<tipo>/v<n>/<nombre del tipo> - v<n>.pdf` (por ejemplo `15/TI-A/v2/Formato A - v2.pdf`), sin importar el nombre del archivo subido, y la respuesta solo trae esa ruta. Las pruebas la validan con una expresión regular (`v\d+`) porque la carpeta de archivos sobrevive a los reinicios de la base de datos de desarrollo y la iteración puede no ser la 1. Los archivos se descargan con `GET /trabajos-grado/{id}/archivos?ruta=...`.

Las carpetas 2 a 4 también verifican los plazos que calcula el backend: días hábiles restantes (10 justo después de radicar o asignar evaluadores), `aTiempo` y `fechaLimite` de la decisión del Comité y de los conceptos, y las opciones de plazo del Consejo (`opcionesPlazo`, 2 en investigación y 3 en práctica, vacía al aprobar).

Los archivos adjuntos y el *Content-Type* de los campos requieren una licencia de Thunder Client (la misma que exigen las colecciones).

### Rutas de los archivos
Las peticiones usan rutas relativas (`archivos/formato-a.pdf`). Thunder las resuelve desde su carpeta de datos, así que deben apuntar a esta carpeta `thunder-tests`:
- con **Save To Workspace** activo y `sgtg_backend` abierto como carpeta del workspace (Thunder usa `thunder-tests/`), o
- con la opción **Custom Path** apuntando a `sgtg_backend/thunder-tests`.

Si Thunder guarda sus datos en otro lugar, los adjuntos fallan con "archivo no encontrado". En ese caso, cambia en la colección `archivos/` por la ruta absoluta de esta carpeta (por ejemplo `C:/ruta/sgtg_backend/thunder-tests/archivos/`).

## Importar (una sola vez)
1. En VS Code abre Thunder Client (icono del rayo en la barra lateral).
2. Pestaña **Collections** → menú `≡` → **Import** → selecciona `sgtg-coleccion.json`.
3. Pestaña **Env** → menú `≡` → **Import** → selecciona `sgtg-entorno-local.json`.
4. En la pestaña **Env**, marca **SGTG local** como entorno activo. Ahí se guardarán los tokens e ids.

## Ejecutar
1. Levanta el backend: `docker compose up --build -d`.
2. En **Collections**, menú `…` de la colección → **Run All**. Las peticiones se ejecutan en orden y el resultado muestra qué pruebas pasaron.

La carpeta 6 adelanta la fecha del backend con el **reloj de desarrollo** (`/api/v1/sgtg/reloj`, solo para el administrador), que existe únicamente con `SGTG_RELOJ_AJUSTABLE=true`. `docker-compose` lo activa por defecto; nunca debe activarse en producción. La colección lo restablece a la fecha real al inicio (carpeta 1) y al final (carpeta 6).

Se puede ejecutar varias veces sobre la misma base de datos: los códigos, correos y nombres usan `{{#guid}}` y los números de documento `{{#number}}{{#number}}` (hasta 12 dígitos), así que no chocan con datos de ejecuciones anteriores. Las consultas generales (por etapa o de todos los trabajos) buscan el trabajo en el cuerpo de la respuesta (`{"id":N,"modalidad"`) en lugar de suponer que es el primero.

## Qué cubre
| Carpeta | Contenido |
|---|---|
| 1. Preparación (administrador) | Login del administrador; crea departamento, programa, docente, cinco estudiantes (un estudiante solo tiene un trabajo activo, así que cada flujo usa el suyo), usuario de Decanatura, miembro del comité, jefe de departamento, dos evaluadores, miembro del Consejo de Facultad y jurado; restablece el reloj; registra los perfiles (el jefe con el mismo departamento del docente). **Usuarios:** crea un docente sin relación con el perfil en la misma creación; desactiva un usuario, que ya no inicia sesión (401); contraseña incorrecta y correo inexistente responden igual (401); el docente cambia su contraseña (la anterior deja de servir); consulta su usuario (`/me`); al listar usuarios no recibe el número de documento |
| 2. Flujo de la propuesta | El docente crea el trabajo con descripción y línea de investigación (queda como director y recibe el rol `DIRECTOR`); la respuesta del Comité sin documento se rechaza (400); corrige la propuesta antes de radicarla; radica el formato A (PDF), que queda `EN_REVISION` (el estado es de cada versión: `propuesta.versiones[i].estado`); una propuesta radicada no se corrige; el director descarga el formato A, un docente sin relación no (403) y una ruta ajena da 404; el comité no la aprueba (con el acta); solo se aceptan PDF; el director corrige la propuesta, radica la versión 2 y el comité la aprueba (la versión 1 queda `NO_APROBADO`, la 2 `APROBADO` y el trabajo pasa a `ANTEPROYECTO`); el estudiante consulta su trabajo y descarga el acta |
| 3. Flujo del anteproyecto y aprobación del Consejo | El director radica el anteproyecto (cada versión con su estado: `anteproyecto.versiones[i].estado`); el jefe lo ve en la consulta por etapa y asigna los evaluadores (pasa a `EVALUACION`; el docente asignado recibe el rol `EVALUADOR`); un evaluador aprueba con formato B y el otro pide correcciones (formato B obligatorio, con el documento opcional de anotaciones) (vuelve a `ANTEPROYECTO`); el director radica la versión 2 (vuelve a `EVALUACION`); ambos aprueban (sigue en `EVALUACION`); un docente no remite (403), sin formato C no se remite (400) y el jefe lo remite al Consejo con el formato C (pasa a `APROBACION_CONSEJO`); la decisión del Consejo sin resolución se rechaza; el Consejo no aprueba sin comentario (vuelve a `ANTEPROYECTO`); se radica la versión 3 y los evaluadores la aprueban; el Consejo no puede fijar 12 meses a un trabajo de investigación (400) y lo aprueba con la resolución a 9 meses (`mesesEjecucion`; pasa a `EN_CURSO` con su plazo y su fecha mínima de sustentación); el evaluador ve el trabajo en sus trabajos |
| 4. Trabajo en curso | Cambio de título: el Consejo no lo aprueba, el director lo solicita de nuevo y el Consejo lo aprueba (el trabajo adopta el nuevo título); flujo completo de una práctica profesional hasta `EN_CURSO`, dos informes mensuales, prórroga de 3 meses aprobada por el Consejo (pasa a `PRORROGA`) y un informe durante la prórroga; suspensión negada, nueva solicitud aprobada (`SUSPENDIDO`, sin informes) y reanudación del director (vuelve a `PRORROGA`) |
| 5. Permisos y reglas de negocio | Un estudiante con un trabajo activo no entra en otro (400); el docente consulta los estudiantes disponibles y el estudiante no (403); 401 sin token; 403 por rol; solo el director radica; regla de etapa; solo `JEFE_DEP` asigna evaluadores; solo los evaluadores asignados emiten concepto; solo `CONSEJO` decide y solo en su etapa; solo el director solicita el cambio de objetivos y solo una vez; informes mensuales: en investigación los radica el estudiante (el director recibe 403) y los revisa el director con observaciones y un PDF de notas sin cambiar la etapa, una sola revisión por revisor y el estudiante no revisa (403); informes con documento; prórroga de 1 a 3 meses, solo del director, decidida por el Consejo, con solicitud pendiente y no estando ya en prórroga; suspensión solo del director, con documento, decidida por el Consejo, una sola vez, y reanudación solo de un trabajo suspendido; consulta por etapa restringida y con etapa válida; el Consejo lista todos los trabajos y el estudiante no (403); codirector de planta (Art. 6); rol inexistente; número de documento con letras; documento repetido; no existe registro público |
| 6. Sustentación (reloj ajustable) | Reglas y permisos (meses mínimos, solo el director solicita, solo el Consejo programa y solo con solicitud, solo los jurados califican); se crea un segundo jurado. **A:** un trabajo de investigación llega a `EN_CURSO`, se adelanta el reloj 100 días, la solicitud sin monografía se rechaza, el director solicita con monografía, formato E y anexos (sin paz y salvo); sin paz y salvo aprobado no se programa; un docente no revisa el paz y salvo (403), Decanatura no lo aprueba sin observaciones (400), no lo aprueba con observaciones y luego lo aprueba; un evaluador del anteproyecto no puede ser jurado del mismo trabajo y sin formato E no se programa; el Consejo programa al coordinador y al otro jurado (en 2099), el director pide una nueva fecha (con ella pendiente no se califica) y el Consejo la aprueba para hoy; el otro jurado no califica (403), sin acta no se califica, el coordinador califica `APROBADO_CON_OBSERVACIONES`, el director radica los ajustes con anexos, solo el coordinador los verifica y carga la versión definitiva (`FINALIZADO`), y ambos jurados ven el trabajo. **B:** la práctica profesional (en prórroga), +90 días, exige el concepto del asesor, Decanatura aprueba el paz y salvo, el coordinador la aplaza, el otro jurado no califica ni registra la inasistencia, se programa la segunda sesión y la inasistencia la deja `NO_APROBADO`. **C:** +100 días, la verificación diaria cierra por vencimiento el trabajo de investigación de las carpetas 2 a 4. Al final restablece el reloj |
| 7. Cancelación | Sobre un trabajo nuevo: solo el director solicita y con formato; una sola pendiente; el trabajo sigue mientras tanto; un docente no decide; el Consejo la niega, se solicita de nuevo y la aprueba (`CANCELADO`, motivo `CANCELACION_DIRECTOR`); un trabajo finalizado no se cancela |
| 8. Anulación y notificaciones | Sobre un trabajo nuevo: solo el director anula (403) y con justificación (400); la anulación lo deja `CANCELADO` con motivo `ANULACION_DIRECTOR` y la justificación en el historial; no se anula dos veces ni un trabajo finalizado. Buzón: el estudiante recibe la anulación (la última creada va primero), el director no recibe sus propias acciones y el Comité recibió las propuestas radicadas; nadie marca ni elimina una notificación ajena (403); marcar una o todas como leídas, eliminar una (luego 404) y vaciar el buzón |

La reanudación automática de la suspensión no se prueba aquí; la cubren las pruebas automáticas.

Los documentos obligatorios (resolución, paz y salvo, informe, solicitud…) los valida el dominio después de la etapa y los plazos; por eso esas pruebas están en el punto del flujo en el que la operación sí es posible.

Las carpetas dependen de las anteriores (usan sus variables), así que deben ejecutarse en orden. Para ejecutar una sola petición, primero hay que ejecutar las que guardan las variables que usa.
