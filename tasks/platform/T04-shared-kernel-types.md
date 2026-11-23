# T04: Shared kernel types

Phase 1 · Size M · Dependencies: T02

**Description:** Add the small, framework-free value types that several modules share: `PageQuery`, `PageResult<T>`, and `Money`. They live in `com.marketplace.shared.domain`.

**Acceptance criteria:**
- [ ] `PageQuery(page, size)`: page ≥ 0, size 1–100; a bad value throws `IllegalArgumentException` with the value and the allowed range.
- [ ] `PageResult<T>(items, totalItems)` has `map(Function<T, R>)` that keeps `totalItems`.
- [ ] `Money(amountMinor, currency)`: amount ≥ 0, currency is a 3-letter ISO 4217 code; `plus(Money)` with a different currency throws with both currencies in the message.
- [ ] No Spring, JPA, or Jackson import in these types.

**Verification** (run in `backend/`):
- [ ] `./mvnw -pl marketplace-api test -Dtest='PageQueryTest,PageResultTest,MoneyTest'` passes
- [ ] Example names: `new_sizeAbove100_throwsIllegalArgument`, `map_twoItems_keepsTotal`, `plus_differentCurrency_throwsIllegalArgument`

**Files:**
- `backend/marketplace-api/src/main/java/com/marketplace/shared/domain/PageQuery.java`
- `backend/marketplace-api/src/main/java/com/marketplace/shared/domain/PageResult.java`
- `backend/marketplace-api/src/main/java/com/marketplace/shared/domain/Money.java`
- `backend/marketplace-api/src/test/java/com/marketplace/shared/domain/` (three test classes)
