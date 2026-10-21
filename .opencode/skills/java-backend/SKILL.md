---
name: java-backend
description: Write, refactor and review Java/Spring Boot backend code following hexagonal architecture, constructor injection, JUnit 5 + AssertJ testing and explicit error handling.
version: 0.2.0
tags: [java, backend, spring, guidelines]
---

# Skill: Java Backend Development

## Purpose

This skill defines how the AI agent should write, refactor, and review Java code
in this repository. It is both human-readable guidance and machine-consumable metadata
so automation (CI, bots) can validate requests against the rules.

---

## Language & Version

- Java 21 (minimum)
- Prefer modern Java features: records, sealed classes, switch expressions
- Avoid deprecated APIs and internal JDK APIs

## Build tools (examples)

- Gradle: use Gradle 8.x+
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
- One public class per file
- Max method length: 30 lines (exceptions allowed with justification)
- Keep methods focused and testable

---

## Architecture Rules

- Follow Hexagonal (Ports & Adapters) / Clean Architecture principles. Emphasize clear
  boundaries between core business logic and external frameworks.
  - **Domain (Core):** Entities, value objects, domain services and domain exceptions — pure Java, no framework types.
  - **Domain Use Cases:** Use-case interfaces and implementations. Orchestrate domain objects and call gateways.
  - **Domain Gateways:** Interfaces (outbound ports) for external resources like repositories, email services, etc.
  - **Infrastructure (Adapters):** Implementations of gateways (driven-adapters) and REST controllers (entry-points). Keep these implementations in `infrastructure` packages.
  - **Framework & Drivers:** Spring, JPA, web servers, DB drivers — keep these at the outermost layer.

- Dependency rule: code dependencies must point inward. Inner layers (domain, use-cases) must not depend on adapters or framework types.

- Use cases & gateways conventions:
  - **Use cases (domain layer):** interfaces and implementations that orchestrate business logic (e.g., `CreateUserUseCase`, `CreateUserUseCaseImpl`).
  - **Gateways (domain layer):** interfaces for external resources (e.g., `UserRepository`, `EmailService`, `PasswordEncoder`).
  - Infrastructure adapters live in `infrastructure.drivenAdapters.*` packages and implement the gateway interfaces.
  - Entry points live in `infrastructure.entryPoints.*` packages and call use case interfaces.

- Mapping and boundaries:
  - Keep DTOs, REST models and mappers inside adapter layers; convert to/from domain objects at the boundary.
  - Do not leak framework-specific types (e.g., `Entity`, `@Entity`, Spring `HttpServletRequest`) into domain/use-case packages.

- Example package layout (hexagonal style):
  - `com.example.domain.model` — entities, value objects, domain exceptions
  - `com.example.domain.usecase` — use-case interfaces and implementations
  - `com.example.domain.gateway` — outbound port interfaces (repositories, external services)
  - `com.example.infrastructure.drivenAdapters` — gateway implementations (JPA, email, security)
  - `com.example.infrastructure.providers` — technical providers (JPA entities, SMTP clients, BCrypt)
  - `com.example.infrastructure.entryPoints` — web controllers, request/response DTOs and mappers
  - `com.example.infrastructure.config` — bean configuration and dependency injection setup

  **Note:** In this repository `infrastructure` packages contain the concrete adapter implementations (i.e., the "adapters"). The term "adapter" is still correct conceptually, but implementations should be grouped under `infrastructure.*` (drivenAdapters, providers, entryPoints, config, etc.).

  Example: external service gateway + adapter (Java)

  ```java
  // domain/gateway/ExternalApiGateway.java
  package com.example.domain.gateway;

  public interface ExternalApiGateway {
    ExternalData fetchData(String id);
  }

  // infrastructure/drivenAdapters/externalApi/ExternalApiGatewayAdapter.java
  package com.example.infrastructure.drivenAdapters.externalApi;

  @Component
  public class ExternalApiGatewayAdapter implements ExternalApiGateway {
    private final WebClient webClient;

    public ExternalApiServiceAdapter(WebClient webClient) { this.webClient = webClient; }

    @Override
    public ExternalData fetchData(String id) {
      // call remote service, map response to domain-friendly ExternalData
      var resp = webClient.get().uri("/api/data/{id}", id).retrieve().bodyToMono(RemoteDto.class).block();
      return RemoteMapper.toExternalData(resp);
    }
  }
  ```

- Minimal ASCII diagram:

  [Entry Points (REST)] -> [Use Case Interface] -> [Use Case Implementation] -> [Gateway Interface] -> [Driven Adapters (DB/External)]

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

- Prefer Java 21 concurrency idioms over manual thread management.
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
    var dto = new CreateUserDto("Alice", "a@example.com");
    var result = service.createUser(dto);
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

1. Affected layer(s) (entry-point/use-case/domain-model/gateway/driven-adapter)
2. Chosen abstraction(s) and DTOs/entities
3. Design validation vs. rules above (list any rule exceptions)
4. Use cases and gateways affected, mapper responsibilities (DTO -> domain)
5. Dependency validation: confirm no inward-dependencies are violated

Example response the agent must produce before code:

```
Layer: domain/usecase + infrastructure/drivenAdapters
Abstraction: CreateUserUseCase (interface + implementation) + UserRepositoryAdapter
Validation: Uses constructor injection, domain layer unchanged, method length <= 30
```

If a request violates the rules, explain why and propose an alternative.

---

## Examples (project structure)

### Hexagonal Architecture - Detailed Structure

```
user-management/
├── applications/
│   └── app-service/
│       └── src/main/java/com/example/
│           └── MainApplication.java
│
├── domain/
│   ├── model/
│   │   ├── User.java
│   │   ├── Email.java
│   │   └── exception/
│   │       ├── UserNotFoundException.java
│   │       └── InvalidEmailException.java
│   │
│   ├── usecase/
│   │   ├── CreateUserUseCase.java
│   │   ├── GetUserUseCase.java
│   │   ├── UpdateUserUseCase.java
│   │   └── DeleteUserUseCase.java
│   │
│   └── gateway/
│       ├── UserGateway.java
│       ├── EmailGateway.java
│       └── PasswordEncoderGateway.java
│
└── infrastructure/
    ├── driven-adapters/
    │   ├── jpa-repository/
    │   │   ├── UserRepositoryAdapter.java
    │   │   └── mapper/
    │   │       └── UserMapper.java
    │   │
    │   ├── email-sender/
    │   │   └── EmailServiceAdapter.java
    │   │
    │   └── security/
    │       └── PasswordEncoderAdapter.java
    │
    ├── providers/
    │   ├── jpa/
    │   │   ├── UserJpaRepository.java
    │   │   ├── entities/
    │   │   │   └── UserEntity.java
    │   │   └── config/
    │   │       └── JpaConfig.java
    │   │
    │   ├── smtp/
    │   │   ├── SmtpClient.java
    │   │   └── config/
    │   │       └── EmailConfig.java
    │   │
    │   └── bcrypt/
    │       └── BCryptProvider.java
    │
    ├── entry-points/
    │   └── api-rest/
    │       ├── UserController.java
    │       ├── exception/
    │       │   └── ApiExceptionHandler.java
    │       ├── dto/
    │       │   ├── CreateUserRequest.java
    │       │   ├── UpdateUserRequest.java
    │       │   └── UserResponse.java
    │       └── mapper/
    │           └── UserDtoMapper.java
    │
    └── config/
        └── BeanConfiguration.java
```

### Minimal project layout

```
src/main/java/com/example
  /domain
    /model
    /usecase
    /gateway
  /infrastructure
    /drivenAdapters
    /providers
    /entryPoints
    /config
src/test/java/com/example
```

Sample flow (entry-points -> use case -> gateway -> driven-adapters):

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
  private final UserGateway repository;

  public CreateUserUseCaseImpl(UserGateway repository) {
    this.repository = repository;
  }

  @Override
  public long create(CreateUserCommand command) {
    var user = User.of(command.name(), command.email());
    var saved = repository.save(user);
    return saved.getId();
  }
}
```

REST Controller (infrastructure/entryPoints/apiRest) — calls use case, maps DTO->domain via mapper:

```java
package com.example.infrastructure.entryPoints.apiRest;

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
    var cmd = mapper.toCommand(dto);
    long id = createUser.create(cmd);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
  }
}
```

DTO Mapper (infrastructure/entryPoints/apiRest/mapper):

```java
package com.example.infrastructure.entryPoints.apiRest.mapper;

import com.example.domain.usecase.CreateUserCommand;
import com.example.infrastructure.entryPoints.apiRest.dto.CreateUserRequest;

public class UserDtoMapper {
  public CreateUserCommand toCommand(CreateUserRequest dto) {
    return new CreateUserCommand(dto.name(), dto.email());
  }
}
```

Repository Adapter (infrastructure/drivenAdapters/jpaRepository) implements `UserRepository` gateway:

```java
package com.example.infrastructure.drivenAdapters.jpaRepository;

import com.example.domain.gateway.UserGateway;
import com.example.domain.model.User;
import com.example.infrastructure.providers.jpa.UserJpaRepository;
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
    var entity = mapper.toEntity(user);
    var saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<User> findById(long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }
}
```

---

---

## CI / Automation (suggested)

Add a GitHub Actions job `.github/workflows/java-ci.yml` to run:

- JDK 21
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
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - name: Build and test (Gradle)
        run: ./gradlew build --no-daemon
```

---

## Notes & Exceptions

- Method-length rule can be relaxed when a refactor is documented in PR description
- Mockito allowed for legacy code where dependency inversion is impractical

## Architecture verification (optional)

- Use ArchUnit to assert that package dependencies point inward. Example tests:

```java
@AnalyzeClasses(packages = "com.example")
class ArchitectureTest {

  @ArchTest
  static final ArchRule domain_should_not_depend_on_infrastructure =
    classes().that().resideInAPackage("..domain..")
      .should().onlyDependOnClassesThat()
      .resideInAnyPackage("..domain..", "java..", "org.slf4j..");

  @ArchTest
  static final ArchRule use_cases_should_only_depend_on_domain_and_gateways =
    classes().that().resideInAPackage("..domain.usecase..")
      .should().onlyDependOnClassesThat()
      .resideInAnyPackage("..domain..", "java..", "org.slf4j..");

  @ArchTest
  static final ArchRule driven_adapters_should_implement_gateways =
    classes().that().resideInAPackage("..infrastructure.drivenAdapters..")
      .should().implement(
        DescribedPredicate.describe("gateway interfaces",
          cls -> cls.getPackageName().contains("domain.gateway"))
      );
}
```

## CI: optional integration and architecture checks

- Recommended CI extensions:
  - Add an optional `integration` job using Testcontainers for DB-backed flows.
  - Add an `architecture-check` job that runs ArchUnit tests to ensure dependency rules.

## Transactions & AOP

- Prefer to apply `@Transactional` and cross-cutting concerns at the use-case layer (`domain.usecase`). Avoid placing transactional boundaries in `infrastructure.entryPoints` (controllers).
- Use AOP aspects sparingly and only for true cross-cutting concerns like logging, security, or transaction management.

---

## Change log

- 0.2.0: Added metadata, machine-readable fields, examples, CI skeleton
