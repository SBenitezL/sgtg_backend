# Pruebas con Postman

Colecciones de Postman (plan gratuito) para probar la API contra el backend en ejecución. Reemplazan a las de Thunder Client, que exige licencia para las colecciones y los archivos adjuntos.

## Archivos
- `sgtg-pruebas-integracion.postman_collection.json`: las pruebas de integración de las Fases 0 a 8 (249 peticiones en 8 carpetas), convertidas de `thunder-tests/sgtg-coleccion.json`. La carpeta 6 adelanta el reloj para probar la sustentación y los vencimientos. El detalle de lo que cubre cada carpeta está en [thunder-tests/README.md](../thunder-tests/README.md#qué-cubre).
- `sgtg-utilidades.postman_collection.json`: el reloj de desarrollo (adelantar la fecha del backend y ejecutar los vencimientos) y la carga masiva de usuarios con datos creíbles.
- `sgtg-local.postman_environment.json`: entorno con `baseUrl` y las credenciales del administrador.
- `usuarios-carga-masiva.csv`: los usuarios de la carga masiva (nombre, correo, código, documento, roles, departamento o programa). Se abre con Excel.

Los PDF que adjuntan las pruebas se leen de `thunder-tests/archivos`.

## Preparar Postman (una sola vez)
1. **Import** → selecciona los dos archivos `.postman_collection.json` y el `.postman_environment.json`.
2. Arriba a la derecha, elige el entorno **SGTG local**. En el entorno, pon en `adminPassword` la contraseña de `SGTG_ADMIN_PASSWORD` de tu `.env`.
3. **Settings → General → Working directory**: elige la carpeta `sgtg_backend`. Postman busca ahí los PDF (`thunder-tests/archivos/...`). Si no se configura, las peticiones con documentos fallan con "file not found".

Las colecciones también traen esas variables con valores por defecto, así que funcionan sin el entorno si las credenciales son las del `.env.example`.

## Ejecutar
Levanta el backend (`docker compose up --build -d`, o `./mvnw spring-boot:run` con las variables del `.env`) y en Postman usa **Run** sobre la colección o una carpeta. En los planes de Postman de 2026 las ejecuciones del Collection Runner no tienen límite.

También se puede ejecutar desde la terminal con Newman, el ejecutor gratuito de Postman (requiere Node.js), desde la carpeta `sgtg_backend`:

```bash
npx newman run postman/sgtg-pruebas-integracion.postman_collection.json -e postman/sgtg-local.postman_environment.json --working-dir .
npx newman run postman/sgtg-utilidades.postman_collection.json -e postman/sgtg-local.postman_environment.json --working-dir .
```

Para usar otra contraseña o puerto sin editar el entorno se agrega `--env-var adminPassword=... --env-var baseUrl=http://localhost:8090`.

### Pruebas de integración
Las carpetas dependen de las anteriores (usan los tokens e ids que guardan en variables de la colección), así que se ejecutan en orden. Se pueden repetir sobre la misma base de datos: códigos, correos y nombres usan `{{$guid}}` y los números de documento la variable `documentoAleatorio` (10 dígitos), que el script de la colección genera en cada petición. La colección restablece el reloj al inicio (carpeta 1) y al final (carpeta 6).

## Reloj de desarrollo
Carpeta **Reloj de desarrollo** de `sgtg-utilidades`. Solo funciona con `SGTG_RELOJ_AJUSTABLE=true` (activo por defecto en `docker-compose` y en el `.env` de ejemplo; nunca en producción). Las peticiones inician sesión como administrador por su cuenta: no hace falta ejecutar antes el login.

| Petición | Qué hace |
|---|---|
| Consultar la fecha del backend | Fecha actual de la aplicación y cuántos días está adelantada |
| Adelantar 1 día / 1 semana / 15 días / 1 mes / 3 meses / 6 meses | Adelanta el reloj esos días |
| Adelantar N días | Adelanta los días de la variable de colección `diasAdelantar` |
| Adelantar N días y ejecutar vencimientos | Adelanta `diasAdelantar` días y ejecuta enseguida la verificación de vencimientos |
| Ir a una fecha | Deja el backend en la fecha de la variable `fechaObjetivo` (AAAA-MM-DD, posterior a la fecha real) |
| Ejecutar la verificación de vencimientos | Ejecuta ahora la tarea diaria: reanuda las suspensiones cumplidas, aplica los plazos de la sustentación y cierra los trabajos con el plazo vencido. Responde cuántos trabajos cambiaron de etapa |
| Restablecer a la fecha real | Vuelve a la fecha real |

- La tarea diaria corre a la hora real configurada (`SGTG_VENCIMIENTOS_CRON`), no cuando se adelanta el reloj. Por eso, después de adelantar, hay que ejecutar la verificación de vencimientos (o usar "Adelantar N días y ejecutar vencimientos").
- El reloj vive en memoria: al reiniciar el backend vuelve a la fecha real.
- Para probar con el front: haz el flujo hasta el punto que depende del tiempo, adelanta el reloj desde Postman y recarga el front. Los plazos y fechas que muestra los calcula el backend con la fecha del reloj.

## Carga masiva de usuarios
Carpeta **Carga masiva de usuarios** de `sgtg-utilidades`. Ejecuta la carpeta con **Run** (o sus dos peticiones en orden):

Solo usa los 4 programas que existen en la FIET, cada uno con su departamento:

| Programa | Departamento |
|---|---|
| Ingeniería de Sistemas | Departamento de Sistemas |
| Tecnología en Telemática | Departamento de Telemática |
| Ingeniería Electrónica y Telecomunicaciones | Departamento de Telecomunicaciones |
| Ingeniería en Automática Industrial | Departamento de Electrónica, Instrumentación y Control |

1. **Cargar docentes, Consejo y Decanatura**: crea los 4 departamentos si faltan y 22 usuarios.
   - 20 docentes con su perfil docente. En cada departamento hay un jefe (JEFE_DEP) y un miembro del Comité de Programa (COMITE), ambos de planta, y 3 docentes sin cargo (uno de planta, uno ocasional y uno de cátedra, en orden mezclado; uno tiene cédula de extranjería).
   - Un miembro del Consejo de Facultad (CONSEJO), uno solo porque el Consejo es de toda la facultad.
   - Una persona de Decanatura (DECANATURA).
   - Estos dos últimos solo tienen su rol: no son docentes ni tienen perfil.
2. **Cargar estudiantes**: crea los 4 programas si faltan, cada uno adscrito a su departamento, y 60 estudiantes (15 por programa, en orden mezclado) con su perfil de estudiante. Si un programa ya existía sin su departamento (o con otro), lo reporta como error: su Comité no vería sus trabajos.

Todos usan la contraseña de la variable de colección `claveCargaMasiva` (por defecto `Unicauca2026*`). Los datos son siempre los mismos: nombres y apellidos comunes en el Cauca, correo institucional (`jdordonez@unicauca.edu.co`), código y cédula con formato real. Se puede ejecutar varias veces: los usuarios que ya existen se omiten, y la prueba informa cuántos se crearon y cuántos ya existían. Si ya hay departamentos o programas con el mismo nombre (MySQL no distingue mayúsculas ni tildes: "Departamento de sistemas" es el mismo que "Departamento de Sistemas"), los reutiliza. Si no logra resolver el departamento o programa de un usuario, no lo crea y lo reporta como error.

¿Los datos sobreviven a un reinicio? Depende de `ddl-auto`. Con el `docker-compose` de este repositorio (`create-drop`) la base se recrea en cada arranque y hay que volver a cargarlos. Con el del front (`SPRING_JPA_HIBERNATE_DDL_AUTO=update`) se conservan.

Para borrar la carga sin reiniciar (deja el administrador, con `cusuari = 1`, y los roles; solo si no hay trabajos de grado):

```bash
docker exec -i sgtg-mysql-1 mysql -uroot -p -P3307 sgtg -e "START TRANSACTION; DELETE FROM perfiles_docente; DELETE FROM perfiles_estudiante; DELETE FROM user_roles WHERE cusuari <> 1; DELETE FROM users WHERE cusuari <> 1; DELETE FROM departamentos; DELETE FROM programas; COMMIT;"
```

Usuarios con cargo:

| Departamento | Jefe (JEFE_DEP) | Comité (COMITE) |
|---|---|---|
| Sistemas | Javier Ramírez Patiño<br>`jramirez@unicauca.edu.co` | Diego Rojas Morales<br>`drojas@unicauca.edu.co` |
| Telemática | José Luis Bravo Torres<br>`jlbravo@unicauca.edu.co` | José Luis Quintero Castillo<br>`jlquintero@unicauca.edu.co` |
| Telecomunicaciones | Carlos Andrés Agredo Hurtado<br>`caagredo@unicauca.edu.co` | Luis Salazar Rodríguez<br>`lsalazar@unicauca.edu.co` |
| Electrónica, Instrumentación y Control | María Fernanda Caicedo Morales<br>`mfcaicedo@unicauca.edu.co` | Julián Díaz Rodríguez<br>`jdiaz@unicauca.edu.co` |

- Consejo de Facultad: Juan David Ordóñez Burbano (`jdordonez@unicauca.edu.co`).
- Decanatura: Catalina Campo Morales (`ccampo@unicauca.edu.co`).

Los demás docentes y los 60 estudiantes están en `usuarios-carga-masiva.csv`.

- El Comité solo ve y decide los trabajos del programa adscrito a su departamento (por ejemplo, `drojas` solo los de Ingeniería de Sistemas). El jefe solo ve los trabajos cuyo director es de su departamento, y solo sobre esos asigna evaluadores y remite al Consejo. El Consejo y Decanatura ven todos. Ver [docs/ajustes-2026-10-23.md](../docs/ajustes-2026-10-23.md).
- Los roles DIRECTOR, EVALUADOR y JURADO no se cargan: el sistema los asigna al crear el trabajo, asignar evaluadores o programar la sustentación.
- Un docente de cátedra necesita un codirector de planta para dirigir (Art. 6).

## Diferencias con la colección de Thunder
- Los tokens e ids se guardan en variables de la colección, no del entorno.
- `{{#guid}}` pasa a `{{$guid}}` y `{{#number}}{{#number}}` a `{{documentoAleatorio}}`.
- Postman no envía un multipart sin partes. Las pruebas de "documento obligatorio" (remisión sin formato C, informe, suspensión y cancelación sin documento) llevan una parte de texto `sinDocumento` que el backend ignora.
