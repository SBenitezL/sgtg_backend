# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

Backend de SGTG (sistema de gestión de trabajos de grado: propuestas, estudiantes, directores, modalidades, procesos). Spring Boot 3.5, Java 17, Maven, MySQL, Spring Security con JWT (jjwt 0.11.5), Lombok. El código, comentarios Javadoc y mensajes están en español.

## Comandos

```bash
./mvnw spring-boot:run                 # ejecutar local (requiere MySQL en localhost:3307, BD sgtg)
./mvnw clean package                   # compilar y empaquetar
./mvnw test                            # todas las pruebas
./mvnw test -Dtest=NombreClaseTest     # una clase de prueba
./mvnw test -Dtest=NombreClaseTest#metodo  # un solo método
docker-compose up --build              # backend + MySQL 8 (puerto 3307)
docker-compose up mysql                # solo la base de datos para desarrollo local
```

En Windows usar `mvnw.cmd` si no se está en Git Bash. La configuración de lanzamiento de VS Code carga variables desde `.env`.

`spring.jpa.hibernate.ddl-auto=create-drop`: el esquema se recrea en cada arranque y los datos se pierden. Los roles se siembran al inicio con `RoleDataInitializer`.

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
- `infraestructure/jwt/JwtAuthenticationFilter` + `configuration/SecurityConfig`: API stateless; `/api/v1/sgtg/auth/**` es público, el resto requiere JWT.

Flujo para una funcionalidad nueva: modelo de dominio → puerto de entrada (`application/input`) → puertos de salida (`application/output`) → caso de uso en `domain/use_cases` → `@Bean` en `ApplicationConfig` → entidad + repositorio + mapper + gateway en infraestructura → DTOs + controlador.

## Dominio de trabajo de grado

Las reglas de negocio vienen de la **Resolución 231 de 2020 del Consejo de Facultad FIET** (Universidad del Cauca). El plan por fases, las decisiones de diseño y el estado de avance están en [docs/plan-dominio.md](docs/plan-dominio.md): leerlo antes de trabajar en el dominio y actualizar la tabla de estado al completar una fase.

- Paquetes: `domain/models/trabajo_grado` (agregado `TrabajoGrado`, etapas, `Proceso`), `domain/models/participantes`, `domain/models/comun`, `domain/services` (`CalendarioHabil`), `domain/exceptions`. `Usuario` y `Role` (auth) siguen en `domain/models`.
- Los participantes (`Estudiante`, `Director`, `Evaluador`, `Asesor`) **no heredan de `Usuario`**: lo referencian por `usuarioId` y nunca copian la contraseña. Solo `Asesor` puede no tener usuario. Los datos académicos (vinculación, departamento, programa) están en `PerfilDocente` / `PerfilEstudiante`, no en `Usuario` ni en los roles.
- Solo el agregado cambia de etapa; las transiciones permitidas están en `Proceso.puedeAvanzarA()`.
- Usar el lenguaje del reglamento: APLAZADO, NO_APROBADO, Comité de Programa, Consejo de Facultad, etc. Un NO APROBADO cierra el trabajo; el estudiante inicia uno nuevo.
- Los documentos se guardan en un servidor de archivos; el dominio solo guarda la URL (`Documento`).
- Las fechas se reciben como parámetro (`LocalDate hoy`); el dominio nunca llama a `LocalDate.now()`. Los días hábiles se calculan con `CalendarioHabil`.
- Las reglas del dominio lanzan `ReglaNegocioException` con claves `error.dominio.*` de `message.properties`.
- Los objetos de valor son `record` que validan en el constructor.
- Probar el dominio con JUnit puro: `./mvnw test -Dtest='com.sgtg.backend.domain.**'` (no requiere MySQL, a diferencia de `BackendApplicationTests`).

## Manejo de errores

Los casos de uso no lanzan excepciones directamente: llaman a `ExceptionFormatterIntPort` (ej. `returnResponseBusinessRuleViolated("error.auth....")`), cuya implementación `ExceptionFormatterImplAdapter` lanza la excepción propia correspondiente (`infraestructure/output/exception/own`, subclases de `OwnException` con un `ErrorCode`). `RestApiException` (`@ControllerAdvice`) las convierte en la respuesta HTTP con la estructura `Error`.

Los mensajes se pasan como claves (ej. `error.auth.user_not_found`) definidas en `src/main/message.properties`. Ojo: ese archivo está fuera de `src/main/resources`, por lo que no está en el classpath y aún no hay un `MessageSource` que resuelva las claves.

## Convenciones

- Nombres de dominio en español (`Usuario`, `Propuesta`, `Estudiante`...); sufijos de clase en inglés según su rol (`IntPort`, `ImplAdapter`, `DTORequest`, `DTOResponse`, `Entity`, `Repository`).
- Lombok se usa en infraestructura (`@RequiredArgsConstructor`, etc.); los modelos de dominio tienen getters/setters escritos a mano.
- Documentar interfaces y métodos públicos con Javadoc en español, como en las clases existentes.
- Mensajes de commit: `Add: ...`, `Fix: ...` / `fix: ...`, en español.
