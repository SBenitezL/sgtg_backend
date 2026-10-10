# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

Backend de SGTG (sistema de gestión de trabajos de grado: propuestas, estudiantes, directores, modalidades, procesos). Spring Boot 3.5, Java 17, Maven, MySQL, Spring Security con JWT (jjwt 0.11.5), Lombok. El código, comentarios Javadoc y mensajes están en español.

## Comandos

```bash
./mvnw spring-boot:run                 # ejecutar local (requiere MySQL en localhost:3307, BD sgtg)
./mvnw clean package                   # compilar y empaquetar
./mvnw test                            # todas las pruebas (usan H2 en memoria, no requieren MySQL)
./mvnw test -Dtest=NombreClaseTest     # una clase de prueba
./mvnw test -Dtest=NombreClaseTest#metodo  # un solo método
docker-compose up --build              # backend + MySQL 8 (puerto 3307)
docker-compose up mysql                # solo la base de datos para desarrollo local
```

En Windows usar `mvnw.cmd` si no se está en Git Bash. La configuración de lanzamiento de VS Code y `docker-compose` cargan variables desde `.env` (no versionado; copiar de `.env.example`). `docker-compose` exige `SGTG_ADMIN_EMAIL` y `SGTG_ADMIN_PASSWORD` y espera a que MySQL esté sano (healthcheck) antes de iniciar el backend.

La tarea programada `VencimientosScheduler` (`infraestructure/input/scheduler`) reanuda a diario las suspensiones que llegaron a su fecha máxima, aplica los plazos de la sustentación (ajustes y segunda sesión) y cierra los trabajos con el plazo vencido; su hora se configura con `sgtg.vencimientos.cron` (`SGTG_VENCIMIENTOS_CRON`).

El `Clock` de la aplicación es `RelojAjustable` (`infraestructure/configuration`). Con `sgtg.reloj.ajustable=true` (`SGTG_RELOJ_AJUSTABLE`, activo por defecto en `docker-compose`, nunca en producción) se expone `RelojController` (`/api/v1/sgtg/reloj`, solo ADMINISTRADOR) para adelantar la fecha y ejecutar la verificación de vencimientos; la colección de Thunder lo usa para la sustentación. En las pruebas JUnit, `FlujoSustentacionIntegracionTest` reemplaza el `Clock` con un reloj propio (`@TestConfiguration` + `@Primary`).

Las pruebas usan H2, que no aplica algunos límites de MySQL (ej. tamaño máximo de fila): al cambiar entidades, validar también con `docker-compose`.

`spring.jpa.hibernate.ddl-auto=create-drop`: el esquema se recrea en cada arranque y los datos se pierden. Los roles se siembran al inicio con `RoleDataInitializer`; si se definen `SGTG_ADMIN_EMAIL` y `SGTG_ADMIN_PASSWORD`, `AdminDataInitializer` crea el administrador inicial.

Las pruebas usan `src/test/resources/application.properties` (H2 en modo MySQL), que reemplaza por completo al `application.properties` principal.

## Arquitectura (hexagonal / puertos y adaptadores)

Paquete base `com.sgtg.backend`. Nótese la ortografía de los paquetes existentes (`infraestructure`, `percistence`, `use_cases`); respetarla al crear clases nuevas.

- `domain/models`: modelos de dominio como POJOs puros (sin anotaciones JPA ni Spring). Contienen reglas de negocio propias (ej. `Usuario.isValidRegister()`).
- `domain/use_cases`: implementación de los casos de uso (`*CUImplAdapter`). **No llevan `@Service`**: se registran como `@Bean` manualmente en `infraestructure/configuration/ApplicationConfig.java`, inyectando los puertos por constructor.
- `application/input`: puertos de entrada (`*CUIntPort`), las interfaces que implementan los casos de uso y que consumen los controladores.
- `application/output`: puertos de salida (`*GatewayIntPort`, `ExceptionFormatterIntPort`) que el dominio usa para hablar con persistencia, servicios de autenticación, etc.
- `infraestructure/input/<modulo>/controller` + `dto/input` / `dto/output`: controladores REST bajo `/api/v1/sgtg/...` con DTOs validados con Jakarta Validation.
- `infraestructure/output/percistence`: `entities` (JPA), `repositories` (Spring Data) y `gateway` (`*GatewayImplAdapter`, `@Service`, implementan los puertos de salida).
- `infraestructure/mapper`: mapeadores estáticos entidad ↔ dominio (`MapperXxx.toDomain` / `toEntity`).
- `infraestructure/output/auth`: `AuthService` (implementa `ManageAuthServiceGatewayIntPort`) y `JwtService`.
- `infraestructure/jwt/JwtAuthenticationFilter` + `configuration/SecurityConfig`: API stateless; solo `/api/v1/sgtg/auth/login` es público, el resto requiere JWT (sin token: 401). Los permisos por rol se declaran en los controladores con `@PreAuthorize("hasAuthority('ROL')")` (los roles no llevan prefijo `ROLE_`); los que dependen de un trabajo concreto (ser su director, evaluador o participante, o jefe del departamento del director) los valida el caso de uso.
- **No hay registro público**: solo el `ADMINISTRADOR` crea usuarios (`POST /api/v1/sgtg/usuarios`), les asigna roles y los activa o inactiva (`PUT /usuarios/{id}/estado`; no puede inactivarse a sí mismo).
- Crear (`POST /usuarios`) y editar (`PUT /usuarios/{id}`, contraseña vacía = conservar) aceptan el perfil académico en la misma petición: `tipoVinculacion` + `departamentoId` (perfil docente) y `programaId` (perfil de estudiante). `ManageUsuarioCUImplAdapter` valida el perfil y los catálogos **antes** de guardar el usuario. Las respuestas (`UsuarioDTOResponse`) incluyen `tipoVinculacion`, `departamento` y `programa`. `GET /usuarios` lo usa cualquier autenticado (para elegir estudiantes, directores…); solo el administrador recibe el documento de identidad. La contraseña de un usuario nuevo o editado exige 8 caracteres.
- Login (`AuthController` → `ManageAuthCUImplAdapter`): correo inexistente o contraseña incorrecta responden igual, **401** `GC-0005` con "Correo o contraseña incorrectos." (`AuthService.verificarCredenciales` compara contra un hash aunque el correo no exista, para no revelar por tiempo qué correos existen). El estado se revisa solo después de validar la contraseña: un usuario inactivo recibe 401 con su propio mensaje. `UserEntity.isEnabled()` es el campo `activo`, y `JwtAuthenticationFilter` ignora los tokens de usuarios inactivos (401).
- `PUT /usuarios/me/password` cambia la contraseña del usuario autenticado (`passwordActual`, `passwordNueva` de al menos 8 caracteres); una contraseña actual incorrecta responde 400, no 401, porque la sesión sigue vigente.
- El usuario autenticado se obtiene en los controladores con `@AuthenticationPrincipal UserEntity`. La fecha actual se obtiene del bean `Clock` (zona America/Bogota) en los casos de uso y se pasa al dominio.

Flujo para una funcionalidad nueva: modelo de dominio → puerto de entrada (`application/input`) → puertos de salida (`application/output`) → caso de uso en `domain/use_cases` → `@Bean` en `ApplicationConfig` → entidad + repositorio + mapper + gateway en infraestructura → DTOs + controlador.

## Dominio de trabajo de grado

Las reglas de negocio vienen de la **Resolución 231 de 2020 del Consejo de Facultad FIET** (Universidad del Cauca). El plan por fases, las decisiones de diseño y el estado de avance están en [docs/plan-dominio.md](docs/plan-dominio.md): leerlo antes de trabajar en el dominio y actualizar la tabla de estado al completar una fase.

- Paquetes: `domain/models/trabajo_grado` (agregado `TrabajoGrado`, etapas, `Proceso`), `domain/models/participantes`, `domain/models/comun`, `domain/services` (`CalendarioHabil`), `domain/exceptions`. `Usuario` y `Role` (auth) siguen en `domain/models`.
- Los participantes (`Estudiante`, `Director`, `Evaluador`, `Asesor`) **no heredan de `Usuario`**: lo referencian por `usuarioId` y nunca copian la contraseña. Solo `Asesor` puede no tener usuario. Los datos académicos (vinculación, departamento, programa) están en `PerfilDocente` / `PerfilEstudiante`, no en `Usuario` ni en los roles.
- Solo el agregado cambia de etapa; las transiciones permitidas están en `Proceso.puedeAvanzarA()`.
- Usar el lenguaje del reglamento: APLAZADO, NO_APROBADO, Comité de Programa, Consejo de Facultad, etc. Un NO APROBADO cierra el trabajo; el estudiante inicia uno nuevo.
- Los archivos los maneja la infraestructura; el dominio solo guarda la ruta relativa (`Documento.ruta` y los campos `ruta*`). Los endpoints que adjuntan documentos reciben `multipart/form-data`: el PDF en la parte `archivo` (en la solicitud de sustentación: `documentoFinal`, `pazYSalvo`, `conceptoAsesor`) y, si hay más datos, un JSON en la parte `datos`. El caso de uso valida que sea PDF (extensión y firma `%PDF-`), lo guarda con `ManageArchivoGatewayIntPort` y pasa la ruta al agregado; si el agregado rechaza la operación, elimina el archivo. Si falta un archivo obligatorio, lo rechaza el dominio con su mensaje.
- `ManageArchivoGatewayImplAdapter` (`infraestructure/output/archivo`) guarda en disco bajo `sgtg.archivos.directorio` (`SGTG_ARCHIVOS_DIRECTORIO`, volumen `archivos_data` en `docker-compose`) con la estructura `<trabajoId>/<carpeta>/v<n>/<nombre>`; la carpeta es `TipoDocumento.carpeta(modalidad)` (código del formato, ej. `PP-A`, o el nombre del tipo, ej. `ANTEPROYECTO`) y `v<n>` es la siguiente iteración libre (nunca sobrescribe). La carta de aceptación se carga antes de que el trabajo tenga id: va a `temporal/` y se reubica tras guardarlo. Tamaño máximo con `spring.servlet.multipart.max-file-size` (`SGTG_ARCHIVOS_TAMANO_MAXIMO`, 20MB).
- Las consultas del trabajo devuelven solo rutas; el archivo se obtiene con `GET /api/v1/sgtg/trabajos-grado/{trabajoId}/archivos?ruta=...`, permitido a quien puede consultar el trabajo (participantes y roles COMITE, JEFE_DEP, CONSEJO, ADMINISTRADOR) y solo para rutas de la carpeta de ese trabajo.
- Las fechas se reciben como parámetro (`LocalDate hoy`); el dominio nunca llama a `LocalDate.now()`. Los días hábiles se calculan con `CalendarioHabil`.
- Las reglas del dominio lanzan `ReglaNegocioException` con claves `error.dominio.*` de `messages.properties`.
- Los objetos de valor son `record` que validan en el constructor.
- Probar el dominio con JUnit puro: `./mvnw test -Dtest='com.sgtg.backend.domain.**'`.
- Persistencia del agregado: tabla `trabajos_grado` más colecciones de valores (`@ElementCollection`) para estudiantes, dirección, propuesta, anteproyecto (versiones, evaluadores, conceptos y decisiones del Consejo), ejecución (cambios de objetivos, con sus listas como JSON en columnas `@Lob`, e informes mensuales; la prórroga va embebida en columnas `prorroga_*`; suspensiones), sustentación (columnas `sustentacion_*` y tablas de sesiones, calificaciones, inasistencias y verificaciones), cancelaciones e historial; se guarda completo con `MapperTrabajoGrado` y se lee con `TrabajoGrado.reconstruir(DatosTrabajoGrado)`. Al agregar estado al agregado en una fase nueva, actualizar `DatosTrabajoGrado`, la entidad y el mapper.

## Manejo de errores

Los casos de uso no lanzan excepciones directamente: llaman a `ExceptionFormatterIntPort` (ej. `returnResponseBusinessRuleViolated("error.auth....")`), cuya implementación `ExceptionFormatterImplAdapter` lanza la excepción propia correspondiente (`infraestructure/output/exception/own`, subclases de `OwnException` con un `ErrorCode`). `RestApiException` (`@ControllerAdvice`) las convierte en la respuesta HTTP con la estructura `Error`.

Las reglas del dominio lanzan `ReglaNegocioException`, que `RestApiException` responde como 400 (`GC-0004`).

Los mensajes se pasan como claves (ej. `error.auth.user_not_found`) definidas en `src/main/resources/messages.properties`; `RestApiException` las traduce al texto con `MessageSource`. Toda clave nueva debe agregarse a ese archivo.

## Convenciones

- Nombres de dominio en español (`Usuario`, `Propuesta`, `Estudiante`...); sufijos de clase en inglés según su rol (`IntPort`, `ImplAdapter`, `DTORequest`, `DTOResponse`, `Entity`, `Repository`).
- Lombok se usa en infraestructura (`@RequiredArgsConstructor`, etc.); los modelos de dominio tienen getters/setters escritos a mano.
- Documentar interfaces y métodos públicos con Javadoc en español, como en las clases existentes.
- Mensajes de commit: `Add: ...`, `Fix: ...` / `fix: ...`, en español.
