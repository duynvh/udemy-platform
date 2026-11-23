# Task

T02 — Backend application with ping endpoint. Task file: [tasks/platform/T02-backend-app.md](../../../tasks/platform/T02-backend-app.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Local ports, Security baseline, Success criterion 3).

# What I changed

| File | What it does |
|---|---|
| `backend/pom.xml` | Adds module `marketplace-api`. |
| `backend/marketplace-api/pom.xml` | App module: `spring-boot-starter-webmvc`, `spring-boot-starter-actuator`, test starter `spring-boot-starter-webmvc-test`, Spring Boot Maven plugin (executable jar). |
| `.../com/marketplace/MarketplaceApplication.java` | Entry point. Its package `com.marketplace` is the root; each sub-package becomes one Spring Modulith module later. |
| `.../shared/adapter/in/web/PingController.java` | `GET /api/v1/ping` → `{"status":"ok"}`. Package-private class, a nested `record` for the response. |
| `.../resources/application.yml` | Port 8090, management port 8091, exposure `health,info,prometheus`, virtual threads on. Ports can be changed with `SERVER_PORT` and `MANAGEMENT_PORT`. |
| `.../shared/adapter/in/web/PingControllerTest.java` | `@WebMvcTest` slice test with `MockMvcTester`: `ping_anyRequest_returnsOk`. |

# The real problem

Every later task needs one small, real path through the system to attach things to: the BFF relays it (T13), traces and metrics observe it (T10, T11), the web app calls it (T17), k6 loads it (T22). Without this "walking skeleton", each of those tasks must also invent a test endpoint, and the platform is tested only when the first business feature exists.

The second problem is exposure. Actuator endpoints show internal information (environment, beans, heap). If they are on the public port, the BFF or an ingress can expose them by mistake.

# Why this solution

- **A ping endpoint, not a business endpoint:** it has no dependencies (no database), so it tests the platform, not the business logic.
- **`shared/adapter/in/web` package:** the endpoint follows the Hexagonal layout from day one. There is no use case behind it, so it has no `application` or `domain` part; that is fine for a technical endpoint.
- **Separate management port (8091):** the main port 8090 serves only the API. The BFF routes only to 8090, so Actuator can never be reached from the browser. Prometheus scrapes 8091 inside the container network.
- **Exposure list `health,info,prometheus`:** deny by default; `env`, `beans`, `heapdump` stay closed (verified: 404).
- **Virtual threads:** one property. Tomcat runs each request on a virtual thread, so blocking calls (JDBC, HTTP to Stripe) do not exhaust a thread pool.
- **`@WebMvcTest` + `MockMvcTester`:** starts only the web layer, so the test runs in seconds and needs no server port. `MockMvcTester` gives AssertJ assertions on status and JSON.

# Production shape

- The container exposes only 8090 to the BFF network. Port 8091 is reachable only by Prometheus and the orchestrator's health probes (`/actuator/health/liveness`, `/actuator/health/readiness`; the groups are already present in the health response).
- The ping endpoint stays as a cheap synthetic check for uptime monitoring through the BFF.
- Health details (`show-details`) stay off; a probe needs only `UP`/`DOWN`.
- `SERVER_PORT` and `MANAGEMENT_PORT` come from the environment, so the same image runs anywhere.

# Other possible approaches

1. Use `/actuator/health` on the main port as the skeleton endpoint.
2. Put the ping endpoint in a business module (for example `catalog`).
3. Test the endpoint with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and a real HTTP client.
4. Spring Security now, to protect Actuator instead of a separate port.

# Why I did not choose those alternatives

1. Actuator on the main port is exactly the exposure risk we want to avoid; also health is not an API path, so the BFF route `/api/**` would not test it.
2. A business module does not exist yet, and the ping is technical; it belongs to `shared`.
3. A full context test is slower and needs ports. The slice test proves the controller contract; the manual check proved the real ports. T13 adds a real HTTP test through the gateway.
4. Security on the backend comes with the resource-server setup (identity / bff-gateway plans). A separate port is a network-level control that works even if a security rule is wrong.

# Key concepts to learn

- **Walking skeleton:** the thinnest end-to-end path that runs in the real architecture, built before features.
- **Spring Boot 4 modular starters:** `spring-boot-starter-webmvc` (formerly `-web`) and a matching test starter `spring-boot-starter-webmvc-test`; test annotations moved to packages like `org.springframework.boot.webmvc.test.autoconfigure`.
- **Slice test (`@WebMvcTest`):** loads only MVC components (controllers, advice, converters), not services or repositories.
- **`MockMvcTester`:** AssertJ-style wrapper over MockMvc (Spring Framework 6.2+).
- **Management port:** `management.server.port` starts a second embedded server only for Actuator.
- **Virtual threads (Java 21+):** cheap threads managed by the JVM; blocking I/O parks the virtual thread, not an OS thread.

# Common mistakes

- Exposing `*` in `management.endpoints.web.exposure.include` "for local debugging" and shipping it.
- Using old Boot 3 imports (`org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`) on Boot 4; the compile error is "cannot find symbol".
- Making the controller and methods `public` by habit. Package-private is enough for Spring and keeps the API surface small.
- Returning a `Map<String, Object>` for JSON. A `record` gives a typed, documented response shape.
- Piping Maven output into `Select-Object -First N` in PowerShell: it stops the pipeline and kills Maven (exit 255) even when the build would pass. Filter with `-Last N` after the run.

# Small example

```java
@RestController
class PingController {
    record PingResponse(String status) {}

    @GetMapping("/api/v1/ping")
    PingResponse ping() {
        return new PingResponse("ok");
    }
}
```

```java
@WebMvcTest(PingController.class)
class PingControllerTest {
    @Autowired private MockMvcTester mockMvc;

    @Test
    void ping_anyRequest_returnsOk() {
        var response = mockMvc.get().uri("/api/v1/ping").exchange();
        assertThat(response).hasStatus(HttpStatus.OK);
        assertThat(response).bodyJson().extractingPath("$.status").isEqualTo("ok");
    }
}
```

# How to think about this next time

- Before features: "What is the thinnest path that crosses every deployable?" Build it first and keep it.
- For every operational endpoint: "Who must reach it, and from which network?" Put it on a port or path only they can reach.
- Write the test first even for trivial endpoints: the RED step here caught nothing in logic, but it proved the Boot 4 test imports compile before any production code existed.
