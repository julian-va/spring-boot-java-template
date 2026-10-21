# AGENTS.md

Single-module Spring Boot 4.1.1 template: Java 25, Gradle Kotlin DSL (`build.gradle.kts`). No CI, no linter/formatter, no README, no LICENSE — don't look for or assume these exist.

## Commands

Always use the wrapper; a bare `gradle` resolves a different version on this machine.

- Build (compiles + tests): `./gradlew build`
- All tests: `./gradlew test`
- One test class: `./gradlew test --tests "jvacloud.spring_boot_java_template.SpringBootJavaTemplateApplicationTests"`
- Run app: `./gradlew bootRun` (actuator at `/actuator`; no `management.*` config exists yet)

## Spring Boot 4 renamed these starters — don't "correct" them

Boot 3 / most examples online use names that are wrong here. This project verifiably builds with:

- `spring-boot-starter-webmvc` (not `spring-boot-starter-web`)
- `spring-boot-starter-webmvc-test` and `spring-boot-starter-actuator-test` (not `spring-boot-starter-test`)

Adding the Boot 3 names pulls in a duplicate legacy stack.

## Package name is intentionally odd

`jvacloud.spring_boot_java_template` contains an underscore because Initializr rejected the hyphenated artifact name; `HELP.md` explains this but is untracked (listed in `.gitignore`). Renaming it touches the package, both source trees, `group`, and `settings.gradle.kts` — ask the user first instead of treating it as cleanup.

## Toolchain gotchas

- JDK 25 is pinned via `java.toolchain`. It must already be installed (SDKMAN `current` here); `settings.gradle.kts` has no foojay toolchain resolver, so Gradle fails the build rather than downloading a JDK.
- Lombok is wired for main *and* test annotation processing (`compileOnly`/`annotationProcessor` plus the `test*` variants). Any new annotation processor needs the same four entries or tests won't compile.
