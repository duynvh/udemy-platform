# Task

T03 — Problem Details handler. Task file: [tasks/platform/T03-problem-handler.md](../../../tasks/platform/T03-problem-handler.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Security baseline: "One Problem Details handler in `shared`. No stack trace in responses."), [architecture.md §6.2](../../architecture.md) (A10 Mishandling of Exceptional Conditions).

# What I changed

| File | What it does |
|---|---|
| `backend/marketplace-api/pom.xml` | Adds `spring-boot-starter-validation` (Bean Validation for `@Valid` request bodies). |
| `.../shared/adapter/in/web/GlobalProblemHandler.java` | `@RestControllerAdvice` that extends `ResponseEntityExceptionHandler`. Adds an `errors` list for validation failures, a fixed message for unreadable JSON, and a 500 handler with `traceId` and no internal details. Lowest precedence. |
| `.../shared/adapter/in/web/GlobalProblemHandlerTest.java` | `@WebMvcTest` with a nested test-only controller that triggers each error type. Three tests, one per error type. |

# The real problem

Without a handler, Spring returns different error shapes for different errors: a Problem Detail for some framework errors, the Boot `/error` JSON for others, and for an unhandled exception a 500 whose log has the stack trace but whose client gets no way to report it. A frontend then needs special code for each shape.

The security problem is information leakage. Exception messages can contain SQL, file paths, passwords in connection strings, or the request body. OWASP Top 10 2025 A10 ("Mishandling of Exceptional Conditions") and A02 (misconfiguration) both cover this.

# Why this solution

- **RFC 9457 Problem Details** is the standard error format. Spring has `ProblemDetail` built in, and the content type is `application/problem+json`. The frontend reads `title`, `status`, `detail` everywhere.
- **Extend `ResponseEntityExceptionHandler`:** it already maps about 20 Spring MVC exceptions (unsupported media type, missing parameter, 404 for no handler) to correct status codes. We override only two of them.
- **`errors` list with `field` and `message`:** a form can show the message next to the right field. It does not echo the rejected value, so passwords never return in an error.
- **Fixed detail for unreadable JSON:** the Jackson parser message can contain parts of the request. A fixed sentence is safe.
- **500 with `traceId`:** the client sees "Unexpected error" plus an id. Support searches that id in Loki and finds the full stack trace. When tracing exists (T11), the id is the real trace id from the MDC; before that, a random UUID still links response and log line.
- **`@Order(LOWEST_PRECEDENCE)`:** module handlers (for example `CourseProblemHandler` for `CourseNotFoundException` → 404) run first. The global handler is the safety net.

# Production shape

- The 500 log line is structured JSON with `trace.id` (T11), so Grafana links the user's `traceId` to the trace and the log.
- An alert counts 5xx responses per service (T12 dashboards, architecture §11.3 SLO).
- Each module adds one advice for its domain exceptions (404, 403, 409, 412, 422), as in architecture §9.5.
- The `type` field can later point to a documentation URL per error (for example `https://api.example.com/problems/price-not-allowed`); now it is `about:blank`, which the RFC allows.

# Other possible approaches

1. Spring Boot property `spring.mvc.problemdetails.enabled=true` only.
2. A custom error JSON format (`{ "code": ..., "message": ... }`).
3. One big global handler that knows all domain exceptions of all modules.
4. Return the exception message in `detail` for all errors, and hide it only in production.

# Why I did not choose those alternatives

1. The property gives standard Problem Details for MVC exceptions, but no field error list and no safe 500 handler.
2. A custom format makes every client learn a new shape; RFC 9457 is a standard that tools and libraries already understand.
3. It breaks module boundaries: `shared` would import exceptions from every module. Each module owns its error mapping.
4. Behavior that differs per environment is how leaks reach production. The same safe output everywhere; developers read the log.

# Key concepts to learn

- **RFC 9457 (Problem Details):** JSON error object with `type`, `title`, `status`, `detail`, `instance`, plus custom properties (here `errors`, `traceId`).
- **`@RestControllerAdvice`:** a bean whose `@ExceptionHandler` methods apply to all controllers.
- **Exception handler resolution:** Spring picks the handler whose exception type is closest to the thrown type; between advice beans, `@Order` decides.
- **`ResponseEntityExceptionHandler`:** base class that maps standard Spring MVC exceptions; override its `handle...` methods to customize.
- **MDC (Mapped Diagnostic Context):** per-thread key-value data that logging includes in each line; tracing puts `traceId` there.
- **`TestTypeExcludeFilter`:** Spring Boot excludes classes nested in test classes from component scanning, so a test-only controller does not leak into other tests; `@Import` adds it where needed.

# Common mistakes

- `@ExceptionHandler(Exception.class)` also catches Spring Security's `AccessDeniedException` thrown by method security. It then returns 500 instead of 403. When method security arrives, add a handler for it (or rethrow) in the security setup.
- Putting `ex.getMessage()` in `detail` for 500 responses.
- Echoing `rejectedValue` in validation errors: it returns passwords or tokens to the client and to browser logs.
- Forgetting `@Valid` on the `@RequestBody` parameter: the constraints exist, but nothing validates them.
- Two advice beans without `@Order`: which one handles an exception becomes random across restarts.

# Small example

Request with an empty title:

```http
POST /api/v1/courses
Content-Type: application/json

{"title":""}
```

Response:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/v1/courses",
  "errors": [ { "field": "title", "message": "must not be blank" } ]
}
```

Unexpected failure:

```json
{ "title": "Internal Server Error", "status": 500, "detail": "Unexpected error", "traceId": "4bf92f3577b34da6a3ce929d0e0e4736" }
```

# How to think about this next time

- Ask: "What does the client need to fix the request, and what must it never see?" Return the first; log the second.
- Ask: "Who owns this exception?" Domain exceptions belong to the module's advice; only framework and unknown errors go to the global handler.
- Every error response needs a way back to the log line: an id the user can report.
