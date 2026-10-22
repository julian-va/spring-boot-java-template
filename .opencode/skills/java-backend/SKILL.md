---
name: java-backend
description: Write, refactor and review Java/Spring Boot backend code following hexagonal architecture, constructor injection, JUnit 5 + AssertJ testing and explicit error handling.
version: 0.3.2
tags: [java, backend, spring, guidelines]
---

# Skill: Java Backend Development

## Purpose

This skill defines how the AI agent should write, refactor, and review Java code
in this repository. It is both human-readable guidance and machine-consumable metadata
so automation (CI, bots) can validate requests against the rules.

---

## Language & Version

- Java 25 (minimum)
- Prefer modern Java features: records, sealed classes, switch expressions
- Avoid deprecated APIs and internal JDK APIs

## Build tools (examples)

- Gradle: use Gradle 9.x+
- Maven: use Maven 3.8+

---

## Metadata (machine-readable)

- Inputs: `request` (JSON) — action and payload; `config` — runtime configuration
- Outputs: `response` (JSON) — {status, data, errors}
- Automations: lint, fmt, unit-tests, build

Example input schema (JSON):

```json
{
  "action": "createUser",
  "payload": { "name": "Alice", "email": "alice@example.com" }
}
```

Example output schema (JSON):

```json
{
  "status": "ok|error",
  "data": {},
  "errors": []
}
```

---

## Code Style Rules

- Follow clean code principles and meaningful naming
- Prefer immutability and small value objects (records)
- Never use `var`: declare the explicit type everywhere, including locals, loop variables and try-with-resources
  - `CreateUserCommand command = mapper.toCommand(dto);` — not `var command = ...`
  - `for (User user : users) { ... }` — not `for (var user : users)`
- One public class per file
- Max method length: 30 lines (exceptions allowed with justification)
- Keep methods focused and testable

---

## Null Handling and Collections

- Validate nullability with `java.util.Objects`, not ad-hoc comparisons:
  - `Objects.requireNonNull(value, "message")` for constructor and public method arguments.
  - `Objects.requireNonNullElse(value, fallback)` / `Objects.requireNonNullElseGet(value, supplier)` when a default value is valid.
  - `Objects.isNull(value)` / `Objects.nonNull(value)` in conditions and predicates instead of `value == null` / `value != null`.
  - `Objects.equals(a, b)` for null-safe equality and `Objects.hash(...)` for hash codes.
  - `Objects.toString(value, fallback)` when building messages or logs.
  - `Objects.checkIndex(index, length)`, `Objects.checkFromToIndex(from, to, length)` and `Objects.checkFromIndexSize(from, size, length)` for index and range validation.
- Use `java.util.Collections` for collection helpers: `emptyList()`, `emptySet()`, `emptyMap()`, `singletonList()`, `unmodifiableList()`, `unmodifiableMap()`, `frequency()` and `sort()`.
  - The JDK has **no `CollectionsUtil`** class. If the project already uses Apache Commons Collections, the helper is `org.apache.commons.collections4.CollectionUtils` (`isEmpty`, `isNotEmpty`, `emptyIfNull`, ...); Guava offers `Collections2` and `Iterables`. Do not add a dependency just for these helpers.
- Return an empty collection instead of `null`; never return `null` for a collection or array.

---

## Architecture Rules

- Follow Hexagonal (Ports & Adapters) / Clean Architecture principles. Emphasize clear
  boundaries between core business logic and external frameworks.
  - **Domain (Core):** Entities, value objects, domain services and domain exceptions — pure Java, no framework types.
  - **Domain Use Cases:** Use-case interfaces and implementations. Orchestrate domain objects and call gateways.
  - **Domain Gateways:** Interfaces (outbound ports) for external resources like repositories, email services, etc.
  - **Infrastructure (Adapters):** Inbound adapters (`entrypoint`), outbound adapters (`adapter`) and technical clients (`provider`). Keep these implementations in `infrastructure` packages.
  - **Framework & Drivers:** Spring, JPA, web servers, DB drivers — keep these at the outermost layer.

- Dependency rule: code dependencies must point inward. Inner layers (domain, use-cases) must not depend on adapters or framework types.

- Use cases & gateways conventions:
  - **Use cases (domain layer):** interfaces and implementations that orchestrate business logic (e.g., `CreateUserUseCase`, `CreateUserUseCaseImpl`).
  - **Gateways (domain layer):** interfaces for external resources (e.g., `UserGateway`, `EmailGateway`, `PasswordEncoderGateway`).
  - Outbound adapters live in `infrastructure.adapter.*`, implement the gateway interfaces and translate between domain and technical types.
  - Technical clients live in `infrastructure.provider.*` and must not import `domain` types.
  - Entry points live in `infrastructure.entrypoint.*` and call use case interfaces.

- Adapter vs provider:
  - `adapter` implements a gateway and may import `domain`; it owns the mapping between domain and technical types.
  - `provider` is a framework client (JPA, Elasticsearch, REST, SMTP, S3) and must not import `domain`.
  - Create a provider only when it is reused by two or more adapters or isolates a framework you may replace. Otherwise, let the adapter use the framework directly.

- Mapping and boundaries:
  - Keep DTOs, REST models and mappers inside adapter layers; convert to/from domain objects at the boundary.
  - Do not leak framework-specific types (e.g., `Entity`, `@Entity`, Spring `HttpServletRequest`) into domain/usecase packages.

- Example package layout (hexagonal style; package names are lowercase and singular):
  - `com.example.domain.model` — entities and value objects
  - `com.example.domain.exception` — domain exceptions
  - `com.example.domain.usecase` — use-case interfaces and implementations
  - `com.example.domain.gateway` — outbound port interfaces (repositories, external services)
  - `com.example.infrastructure.entrypoint` — inbound adapters (REST controllers, DTOs, mappers)
  - `com.example.infrastructure.adapter` — gateway implementations (JPA, email, security)
  - `com.example.infrastructure.provider` — technical clients (Spring Data repositories, entities, SMTP, Elasticsearch, REST, S3)
  - `com.example.infrastructure.config` — bean configuration and dependency injection setup

  **Terminology note:** `entrypoint` and `provider` are project conventions, not canonical terms. Hexagonal calls them driving adapters and driven adapters; Clean Architecture calls the outer ring Frameworks & Drivers. Keep the names consistent across the repository.

  Example: external service gateway, technical client and adapter (Java)

  ```java
  // domain/gateway/ExternalApiGateway.java
  package com.example.domain.gateway;

  public interface ExternalApiGateway {
    ExternalData fetchData(String id);
  }

  // infrastructure/provider/externalapi/ExternalApiClient.java
  package com.example.infrastructure.provider.externalapi;

  import org.springframework.stereotype.Component;
  import org.springframework.web.client.RestClient;

  @Component
  public class ExternalApiClient {
    private final RestClient restClient;

    public ExternalApiClient(RestClient restClient) {
      this.restClient = restClient;
    }

    public RemoteDto fetchData(String id) {
      return restClient.get().uri("/api/data/{id}", id).retrieve().body(RemoteDto.class);
    }
  }

  // infrastructure/adapter/externalapi/ExternalApiGatewayAdapter.java
  package com.example.infrastructure.adapter.externalapi;

  import com.example.domain.gateway.ExternalApiGateway;
  import org.springframework.stereotype.Component;

  @Component
  public class ExternalApiGatewayAdapter implements ExternalApiGateway {
    private final ExternalApiClient client;

    public ExternalApiGatewayAdapter(ExternalApiClient client) {
      this.client = client;
    }

    @Override
    public ExternalData fetchData(String id) {
      return RemoteMapper.toExternalData(client.fetchData(id));
    }
  }
  ```

- Minimal ASCII diagram:

  [Entrypoint (REST)] -> [Use Case] -> [Gateway] -> [Adapter] -> [Provider]

- Rationale: hexagonal boundaries improve testability, make adapters replaceable, and enforce single responsibility and separation of concerns.

---

## Framework Rules (Spring Boot guidance)

- Use constructor injection only — prefer final fields
- Do NOT use field injection
- Prefer `@Service`, `@Component`, `@Repository` stereotypes
- Avoid `@Autowired` in favor of constructor injection
- Use `@ConfigurationProperties` for typed configuration
- Logging: use SLF4J/Logback; do NOT use `System.out.println`

---

## Concurrency and Threading

- Prefer Java 25 concurrency idioms over manual thread management.
- Use virtual threads for large numbers of blocking tasks:
  - `Executors.newVirtualThreadPerTaskExecutor()`
- Prefer structured concurrency for grouped task scopes:
  - `StructuredTaskScope` and `StructuredTaskScope.ShutdownOnFailure`
- Avoid `new Thread(...)` unless a platform thread is explicitly required.
- Keep shared mutable state minimal and prefer immutable domain objects.
- Avoid blocking calls in event-loop/reactive code; if blocking is required, use virtual threads or separate thread pools.
- Always shut down executors cleanly (`shutdown()` / `shutdownNow()`), and prefer try-with-resources when possible.
- Use higher-level abstractions (`CompletableFuture`, `ExecutorService`, `ConcurrentHashMap`) instead of low-level synchronization where practical.

---

## Error Handling

- Do not throw generic `Exception`
- Use custom domain exceptions and explicit mappings to HTTP status codes
- Map exceptions explicitly (controller advice or exception mappers)

---

## Testing Rules

- Use JUnit 5
- Use AssertJ for assertions
- Use Mockito only when necessary (prefer test-friendly design)
- Favor unit tests; require integration tests for critical flows and CI gating

Example unit test (JUnit 5 + AssertJ):

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock UserRepository repo;
  @InjectMocks UserService service;

  @Test
  void createUser_savesAndReturnsId() {
    when(repo.save(any())).thenReturn(new User(1L, "Alice"));
    CreateUserDto dto = new CreateUserDto("Alice", "a@example.com");
    User result = service.createUser(dto);
    assertThat(result.getId()).isEqualTo(1L);
  }
}
```

---

## Forbidden Patterns

- No static utility classes for business logic
- No God classes
- No magic numbers (use named constants)
- No `System.out.println` (use logger)

---

## Reasoning Checklist (agent must return before code)

When asked to implement or modify code, the agent must return a short checklist containing:

1. Affected layer(s) (entrypoint/usecase/domain.model/domain.gateway/adapter/provider)
2. Chosen abstraction(s) and DTOs/entities
3. Design validation vs. rules above (list any rule exceptions)
4. Use cases and gateways affected, mapper responsibilities (DTO -> domain)
5. Dependency validation: confirm no inward-dependencies are violated, and that `provider` does not import `domain`
6. Explicit types and null handling: no `var`, `Objects` for null validation, empty collections instead of `null`

Example response the agent must produce before code:

```
Layer: domain/usecase + infrastructure/adapter/jpa
Abstraction: CreateUserUseCase (interface + implementation) + UserRepositoryAdapter
Validation: Uses constructor injection, domain layer unchanged, provider has no domain types, method length <= 30
```

If a request violates the rules, explain why and propose an alternative.

---

## Examples (project structure)

### Hexagonal Architecture - Detailed Structure

```
src/main/java/com/example
├── MainApplication.java
├── domain
│   ├── model
│   │   ├── User.java
│   │   └── Email.java
│   ├── exception
│   │   ├── UserNotFoundException.java
│   │   └── InvalidEmailException.java
│   ├── usecase
│   │   ├── CreateUserUseCase.java
│   │   ├── GetUserUseCase.java
│   │   └── CreateUserUseCaseImpl.java
│   └── gateway
│       ├── UserGateway.java
│       ├── EmailGateway.java
│       └── PasswordEncoderGateway.java
└── infrastructure
    ├── entrypoint
    │   └── rest
    │       ├── UserController.java
    │       ├── exception
    │       │   └── ApiExceptionHandler.java
    │       ├── dto
    │       │   ├── CreateUserRequest.java
    │       │   ├── UpdateUserRequest.java
    │       │   └── UserResponse.java
    │       └── mapper
    │           └── UserDtoMapper.java
    ├── adapter
    │   ├── jpa
    │   │   ├── UserRepositoryAdapter.java
    │   │   └── mapper
    │   │       └── UserMapper.java
    │   ├── email
    │   │   └── EmailAdapter.java
    │   └── security
    │       └── PasswordEncoderAdapter.java
    ├── provider
    │   ├── jpa
    │   │   ├── UserJpaRepository.java
    │   │   └── entity
    │   │       └── UserEntity.java
    │   └── smtp
    │       └── SmtpClient.java
    └── config
        └── BeanConfiguration.java
```

In a multi-module build, each top-level folder (`domain`, `infrastructure`) can become a Gradle module with its own `src/main/java` and `build.gradle.kts`.

### Minimal project layout

```
src/main/java/com/example
  /domain
    /model
    /exception
    /usecase
    /gateway
  /infrastructure
    /entrypoint
    /adapter
    /provider
    /config
src/test/java/com/example
```

Sample flow (entrypoint -> use case -> gateway -> adapter -> provider):

Use case interface (domain/usecase):

```java
package com.example.domain.usecase;

public interface CreateUserUseCase {
  long create(CreateUserCommand command);
}
```

Gateway interface (domain/gateway):

```java
package com.example.domain.gateway;

import com.example.domain.model.User;
import java.util.Optional;

public interface UserGateway {
  User save(User user);
  Optional<User> findById(long id);
}
```

Use-case implementation (domain/usecase):

```java
package com.example.domain.usecase;

import com.example.domain.gateway.UserGateway;
import com.example.domain.model.User;

public class CreateUserUseCaseImpl implements CreateUserUseCase {
  private final UserGateway gateway;

  public CreateUserUseCaseImpl(UserGateway gateway) {
    this.gateway = gateway;
  }

  @Override
  public long create(CreateUserCommand command) {
    User user = User.of(command.name(), command.email());
    User saved = gateway.save(user);
    return saved.getId();
  }
}
```

REST Controller (infrastructure/entrypoint/rest) — calls use case, maps DTO->domain via mapper:

```java
package com.example.infrastructure.entrypoint.rest;

import com.example.domain.usecase.CreateUserUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final CreateUserUseCase createUser;
  private final UserDtoMapper mapper;

  public UserController(CreateUserUseCase createUser, UserDtoMapper mapper) {
    this.createUser = createUser;
    this.mapper = mapper;
  }

  @PostMapping
  public ResponseEntity<Map<String, Long>> create(@RequestBody CreateUserRequest dto) {
    CreateUserCommand cmd = mapper.toCommand(dto);
    long id = createUser.create(cmd);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
  }
}
```

DTO Mapper (infrastructure/entrypoint/rest/mapper):

```java
package com.example.infrastructure.entrypoint.rest.mapper;

import com.example.domain.usecase.CreateUserCommand;
import com.example.infrastructure.entrypoint.rest.dto.CreateUserRequest;

public class UserDtoMapper {
  public CreateUserCommand toCommand(CreateUserRequest dto) {
    return new CreateUserCommand(dto.name(), dto.email());
  }
}
```

Repository Adapter (infrastructure/adapter/jpa) implements the `UserGateway` interface:

```java
package com.example.infrastructure.adapter.jpa;

import com.example.domain.gateway.UserGateway;
import com.example.domain.model.User;
import com.example.infrastructure.adapter.jpa.mapper.UserMapper;
import com.example.infrastructure.provider.jpa.UserJpaRepository;
import com.example.infrastructure.provider.jpa.entity.UserEntity;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserGateway {
  private final UserJpaRepository jpaRepository;
  private final UserMapper mapper;

  public UserRepositoryAdapter(UserJpaRepository jpaRepository, UserMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public User save(User user) {
    UserEntity entity = mapper.toEntity(user);
    UserEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<User> findById(long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }
}
```

---

## CI / Automation (suggested)

Add a GitHub Actions job `.github/workflows/java-ci.yml` to run:

- JDK 25
- Build (Gradle/Maven)
- Run `./gradlew check` or `mvn -DskipTests=false verify`

Example workflow snippet:

```yaml
name: Java CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "25"
      - name: Build and test (Gradle)
        run: ./gradlew build --no-daemon
```

---

## Notes & Exceptions

- Method-length rule can be relaxed when a refactor is documented in PR description
- Mockito allowed for legacy code where dependency inversion is impractical

## Architecture verification (ArchUnit)

- The template ships `ArchitectureTest` in `src/test/java`, with the dependency rule encoded as tests.
- ArchUnit is test-only. The Spring Boot BOM does not manage it, so the version is pinned:
  `testImplementation("com.tngtech.archunit:archunit-junit6:1.5.1")`
  Use `archunit-junit6`, not `archunit-junit5`: Spring Boot 4 runs JUnit 6, and the JUnit 5 artifact targets the previous platform.
- `@AnalyzeClasses` points at the base package and excludes test classes:
  `@AnalyzeClasses(packages = "com.example", importOptions = ImportOption.DoNotIncludeTests.class)`
  In this template the base package is `jvacloud.spring_boot_java_template`.
- Add `.allowEmptyShould(true)` to every rule while the target packages are empty. Without it ArchUnit fails with "failed to check any classes"; once a class exists, the rule is enforced normally.
- Run only the architecture tests: `./gradlew test --tests "*ArchitectureTest"`.

Rules that encode the dependency rule:

- `domain` must not depend on `infrastructure`.
- `infrastructure.provider` must not depend on `domain` (a provider is a technical client).
- `domain.usecase` may only depend on `domain`, the JDK, SLF4J and Spring stereotype/transaction annotations.
- `infrastructure.entrypoint` must not depend on `adapter` or `provider` (controllers call use cases).
- `adapter` is the only layer allowed to depend on both `domain` and `provider`.

```java
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.example",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domain_should_not_depend_on_infrastructure =
      noClasses().that().resideInAPackage("..domain..")
          .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule use_cases_should_only_depend_on_domain =
      classes().that().resideInAPackage("..domain.usecase..")
          .should().onlyDependOnClassesThat()
          .resideInAnyPackage(
              "..domain..",
              "java..",
              "org.slf4j..",
              "org.springframework.stereotype..",
              "org.springframework.transaction.annotation.."
          )
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule providers_should_not_depend_on_domain =
      noClasses().that().resideInAPackage("..infrastructure.provider..")
          .should().dependOnClassesThat().resideInAPackage("..domain..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule entrypoints_should_not_depend_on_adapters_or_providers =
      noClasses().that().resideInAPackage("..infrastructure.entrypoint..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("..infrastructure.adapter..", "..infrastructure.provider..")
          .allowEmptyShould(true);
}
```

- Read failures as dependency directions: the message names the origin class, the target class and the offending call or field. Move the behaviour to the correct layer (for example, a controller calls a use case, never a provider) instead of weakening the rule or expanding its allowlist.
- To prove a rule is active, temporarily add a class that violates it and confirm the test fails; delete the class afterwards.

## CI: optional integration checks

- The `build` job already runs the ArchUnit tests with the rest of the unit tests.
- Recommended CI extension: add an optional `integration` job using Testcontainers for DB-backed flows.

## Transactions & AOP

- Prefer to apply `@Transactional` and cross-cutting concerns at the use-case layer (`domain.usecase`). Avoid placing transactional boundaries in `infrastructure.entrypoint` (controllers).
- Repositories under `infrastructure.provider` already manage their own transactions; do not spread `@Transactional` across providers.
- Use AOP aspects sparingly and only for true cross-cutting concerns like logging, security, or transaction management.

---

## Change log

- 0.3.2: ArchUnit in the template (`archunit-junit6`), empty-selection handling and the four dependency rules with `allowEmptyShould(true)`
- 0.3.1: Prohibit `var` (explicit types only), require `java.util.Objects` for null handling and `java.util.Collections` helpers (no `CollectionsUtil` in the JDK)
- 0.3.0: Lowercase singular packages (`entrypoint`, `adapter`, `provider`), adapter/provider boundary rule, fixed external adapter example and ArchUnit provider rule
- 0.2.2: Use camelCase package names in the tree diagram
- 0.2.1: Target Java 25 and Gradle 9.x for this project
- 0.2.0: Added metadata, machine-readable fields, examples, CI skeleton
