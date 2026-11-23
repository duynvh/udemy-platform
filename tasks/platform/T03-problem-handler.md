# T03: Problem Details handler

Phase 1 · Size S · Dependencies: T02

**Description:** Add one global handler in `shared` that turns errors into RFC 9457 Problem Details. Modules add their own handlers for domain exceptions later; this one covers validation errors and unexpected errors.

**Acceptance criteria:**
- [ ] Bean Validation failure → 400 `application/problem+json` with an `errors` list (field, message).
- [ ] Unexpected exception → 500 with `detail: "Unexpected error"` and a `traceId` property; no stack trace or exception class name in the body.
- [ ] Malformed JSON body → 400 with a short `detail`.

**Verification** (run in `backend/`):
- [ ] `./mvnw -pl marketplace-api test -Dtest=GlobalProblemHandlerTest` passes: `handle_validationError_returns400WithFieldErrors`, `handle_unexpectedException_returns500WithoutStackTrace`, `handle_malformedJson_returns400`

**Files:**
- `backend/marketplace-api/src/main/java/com/marketplace/shared/adapter/in/web/GlobalProblemHandler.java`
- `backend/marketplace-api/src/test/java/com/marketplace/shared/adapter/in/web/GlobalProblemHandlerTest.java` (uses a test-only controller inside the test class)
