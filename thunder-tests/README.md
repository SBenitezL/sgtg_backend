# Pruebas de integración con Thunder Client

Colección que recorre la API de las Fases 0 y 1 contra el backend en ejecución. Cada petición verifica el código de respuesta y los datos devueltos, y guarda en variables los tokens e identificadores que usan las siguientes.

## Archivos
- `sgtg-coleccion.json`: colección con 29 peticiones en 3 carpetas.
- `sgtg-entorno-local.json`: entorno con `baseUrl` y las credenciales del administrador de desarrollo (las mismas del archivo `.env`).

## Importar (una sola vez)
1. En VS Code abre Thunder Client (icono del rayo en la barra lateral).
2. Pestaña **Collections** → menú `≡` → **Import** → selecciona `sgtg-coleccion.json`.
3. Pestaña **Env** → menú `≡` → **Import** → selecciona `sgtg-entorno-local.json`.
4. En la pestaña **Env**, marca **SGTG local** como entorno activo. Ahí se guardarán los tokens e ids.

## Ejecutar
1. Levanta el backend: `docker compose up --build -d`.
2. En **Collections**, menú `…` de la colección → **Run All**. Las peticiones se ejecutan en orden y el resultado muestra qué pruebas pasaron.

Se puede ejecutar varias veces sobre la misma base de datos: los códigos, correos y nombres usan `{{#guid}}` y los números de documento `{{#number}}{{#number}}` (hasta 12 dígitos), así que no chocan con datos de ejecuciones anteriores.

## Qué cubre
| Carpeta | Contenido |
|---|---|
| 1. Preparación (administrador) | Login del administrador; crea departamento, programa, docente, estudiante y miembro del comité; registra los perfiles |
| 2. Flujo de la propuesta | El docente crea el trabajo (queda como director) y radica la propuesta; el comité no la aprueba (con ruta del acta); se radica la versión 2 y el comité la aprueba (pasa a `ANTEPROYECTO`); el estudiante consulta su trabajo |
| 3. Permisos y reglas de negocio | 401 sin token; 403 por rol; solo el director radica; regla de etapa; codirector de planta (Art. 6); rol inexistente; número de documento con letras; documento repetido; no existe registro público |

Las carpetas dependen de las anteriores (usan sus variables), así que deben ejecutarse en orden. Para ejecutar una sola petición, primero hay que ejecutar las que guardan las variables que usa.
