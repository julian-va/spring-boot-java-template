# AGENTS.md

Single-module Spring Boot 4.1.1 template: Java 25, Gradle Kotlin DSL (`build.gradle.kts`). No CI, no linter/formatter, no LICENSE — don't look for or assume these exist.

## Commands

Always use the wrapper; a bare `gradle` resolves a different version on this machine.

- Build (compiles + tests): `./gradlew build`
- All tests: `./gradlew test`
- One test class: `./gradlew test --tests "jvacloud.spring_boot_java_template.SpringBootJavaTemplateApplicationTests"`
- Architecture tests: `./gradlew test --tests "jvacloud.spring_boot_java_template.ArchitectureTest"`
- Run app: `./gradlew bootRun` — actuator at `/actuator/health`, OpenAPI at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`. No `management.*` config exists yet.

## Coverage (JaCoCo)

- `./gradlew check` — and therefore `build` — runs `jacocoTestCoverageVerification` with an 80% instruction minimum, so adding a class without tests fails the build.
- `**/SpringBootJavaTemplateApplication.class` and data-only code are excluded on purpose (see `jacocoExcludes` in `build.gradle.kts`): DTOs under `infrastructure/entrypoint/**/dto`, JPA entities under `infrastructure/provider/jpa/entity`, `infrastructure/config`, and `domain/exception`.
- The bootstrap class is still the only main class, so the bundle is empty and the gate passes vacuously. It starts enforcing as soon as a **non-excluded** class exists.
- Report: `build/reports/jacoco/test/html/index.html` (HTML + XML, gitignored).
- `lombok.config` enables `lombok.addLombokGeneratedAnnotation` so JaCoCo filters Lombok-generated methods. Removing it makes getters/builders count as untested.

## Spring Boot 4 renamed these starters — don't "correct" them

Boot 3 / most examples online use names that are wrong here. This project verifiably builds with:

- `spring-boot-starter-webmvc` (not `spring-boot-starter-web`)
- `spring-boot-starter-webmvc-test` and `spring-boot-starter-actuator-test` (not `spring-boot-starter-test`)

Adding the Boot 3 names pulls in a duplicate legacy stack.

## Springdoc is pinned by hand

`springdoc-openapi-starter-webmvc-ui:3.1.1` is explicit because the Spring Boot BOM does not manage it, and the 2.x line targets Boot 3. Don't drop the version or "fix" it to 2.x. `OpenApiDocumentationTests` fails if `/v3/api-docs` stops responding, so a Boot upgrade that breaks springdoc surfaces as a test failure.

## ArchUnit is pinned by hand

`com.tngtech.archunit:archunit-junit6:1.5.1` is explicit because the Spring Boot BOM does not manage ArchUnit. Use the **junit6** artifact: Boot 4 runs JUnit 6, and `archunit-junit5` targets the previous platform.

`ArchitectureTest` encodes the dependency rule: `domain` must not depend on `infrastructure`, `infrastructure.provider` must not depend on `domain`, use cases stay within `domain`, and `infrastructure.entrypoint` must not depend on `adapter`/`provider`. Every rule uses `allowEmptyShould(true)` because those packages are still empty; removing it makes the test fail with "failed to check any classes" until real classes exist. Run them with `./gradlew test --tests "*ArchitectureTest"`.

## Package name is intentionally odd

`jvacloud.spring_boot_java_template` contains an underscore because Initializr rejected the hyphenated artifact name; `HELP.md` explains this but is untracked (listed in `.gitignore`). Renaming it touches the package, both source trees, `group`, and `settings.gradle.kts` — ask the user first instead of treating it as cleanup.

## Toolchain gotchas

- JDK 25 is pinned via `java.toolchain`. It must already be installed (SDKMAN `current` here); `settings.gradle.kts` has no foojay toolchain resolver, so Gradle fails the build rather than downloading a JDK.
- Lombok is wired for main *and* test annotation processing (`compileOnly`/`annotationProcessor` plus the `test*` variants). Any new annotation processor needs the same four entries or tests won't compile.
- `build.gradle.kts` is intentionally kept free of comments; don't add explanatory comments to it.

## Runtime config

- `spring.threads.virtual.enabled: true` in `application.yaml`: Tomcat serves requests on virtual threads. `ThreadLocal` state (including Spring Security's `SecurityContext`) is not inherited by work handed to another executor; propagate it explicitly in `@Async` or custom executors.

## Skills

- Project skills live in `.opencode/skills/<id>/SKILL.md`. The directory name is the skill ID and the frontmatter needs a `description`, or OpenCode registers the skill but never advertises it.
- Validate skill frontmatter before committing. A `: ` (colon-space) inside an unquoted value is invalid YAML: GitHub rejects the whole file with "mapping values are not allowed in this context", while OpenCode's lenient parser accepts it, so the mistake passes locally and only fails on GitHub.
