# spring-boot-java-template

Minimal Spring Boot service on Java 25 with actuator, Lombok, JaCoCo coverage enforcement, and two OpenCode skills.

## Requirements

- **JDK 25**, pinned through `java.toolchain` in `build.gradle.kts`. It must already be installed and discoverable: `settings.gradle.kts` has no toolchain resolver, so Gradle fails instead of downloading one.
- **No Gradle install needed.** Use the wrapper (`./gradlew`), which pins Gradle 9.8.1.

## Run

```bash
./gradlew bootRun
```

Or build the executable jar:

```bash
./gradlew bootJar
java -jar build/libs/spring-boot-java-template-0.0.1-SNAPSHOT.jar
```

There are no web controllers yet, so `GET /` returns 404. The HTTP surface is actuator plus the OpenAPI documentation:

```bash
curl localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

- `GET /v3/api-docs` — OpenAPI 3.1 document
- `GET /swagger-ui.html` — Swagger UI (redirects to `/swagger-ui/index.html`)

## Test

```bash
./gradlew test
```

Run a single test class:

```bash
./gradlew test --tests "jvacloud.spring_boot_java_template.SpringBootJavaTemplateApplicationTests"
```

## Coverage

`./gradlew check`, and therefore `./gradlew build`, runs `jacocoTestCoverageVerification` with an **80% instruction minimum**.

- Reports: `build/reports/jacoco/test/html/index.html` and `build/reports/jacoco/test/jacocoTestReport.xml` (both gitignored).
- `SpringBootJavaTemplateApplication` and data-only code are excluded from analysis: DTOs, JPA entities, bean configuration and domain exceptions. See `jacocoExcludes` in `build.gradle.kts`.
- `lombok.config` enables `lombok.addLombokGeneratedAnnotation` so JaCoCo ignores generated getters, builders and `equals`/`hashCode`.
- **Current caveat:** the bootstrap class is the only class, so the analysis bundle is empty and the gate passes vacuously. It starts enforcing as soon as any non-excluded class exists.

## Project layout

```text
src/main/java/jvacloud/spring_boot_java_template/   application entry point
src/main/resources/application.yaml                 app config (name, virtual threads)
src/test/java/...                                   JUnit 5 tests (context + OpenAPI)
.opencode/skills/java-backend/SKILL.md              hexagonal architecture and Java conventions
.opencode/skills/security/SKILL.md                  security practices and PR checklist
AGENTS.md                                           guidance for AI agents working in this repo
```

## Notes on the setup

- **Spring Boot 4 renamed the starters.** This project uses `spring-boot-starter-webmvc` (not `-web`) and `spring-boot-starter-webmvc-test` plus `spring-boot-starter-actuator-test` (not `spring-boot-starter-test`). Adding the Boot 3 names pulls in a duplicate legacy stack.
- **Virtual threads are on.** `spring.threads.virtual.enabled: true` makes Tomcat serve requests on `java.lang.VirtualThread` instead of the platform-thread pool. Remove the property to go back to `org.apache.tomcat.util.threads.TaskThread`.
- **Springdoc is pinned by hand.** `springdoc-openapi-starter-webmvc-ui:3.1.1` is the Boot 4 line (2.x targets Boot 3) and the Spring Boot BOM does not manage it, so the version is explicit in `build.gradle.kts`. `OpenApiDocumentationTests` fails if it stops serving `/v3/api-docs`. **In production, turn it off**: `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`, otherwise the full API surface is published unauthenticated.
- **The package name contains an underscore** (`jvacloud.spring_boot_java_template`). It is deliberate: Spring Initializr rejected the hyphenated artifact name. Renaming it touches the package, both source trees, `group` and `settings.gradle.kts`.
- **No CI, no linter/formatter.** There are no workflows in this repo, so the coverage gate only guards local builds.

## License

No license file yet, which means default copyright applies. Add one before reusing this code elsewhere.
