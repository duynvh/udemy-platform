# Task

T04 — Shared kernel types. Task file: [tasks/platform/T04-shared-kernel-types.md](../../../tasks/platform/T04-shared-kernel-types.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Project Structure: "`shared/` → Shared kernel: Money, PageQuery, PageResult").

# What I changed

| File | What it does |
|---|---|
| `.../shared/domain/PageQuery.java` | `record PageQuery(int page, int size)`. Page ≥ 0, size 1–100 (`MAX_SIZE`). |
| `.../shared/domain/PageResult.java` | `record PageResult<T>(List<T> items, long totalItems)`. Copies the list (immutable), total ≥ 0, `map(...)` keeps the total. |
| `.../shared/domain/Money.java` | `record Money(long amountMinor, String currency)`. Amount ≥ 0, currency is a known ISO 4217 code, `plus(...)` only for the same currency, overflow-safe (`Math.addExact`). |
| `.../shared/domain/PageQueryTest.java`, `PageResultTest.java`, `MoneyTest.java` | 13 unit tests. Each failure test checks that the message has the bad value and the expected shape. |

# The real problem

Many use cases list things page by page ("my courses", "review queue", "purchase history") and many handle prices (authoring, catalog, checkout, payout). If each module invents its own page and money types, three problems follow:

1. Ports start to use Spring Data `Pageable` and `Page`. Then the application layer depends on a persistence framework, which breaks the Hexagonal rule.
2. Money gets stored as `double` somewhere. `0.1 + 0.2` is not `0.3` in floating point; refunds and revenue shares then lose or invent cents.
3. Validation is repeated, or forgotten, in each module (a page size of 10 000 can overload the database).

# Why this solution

- **Records:** immutable values with `equals`, `hashCode`, `toString` for free. The compact constructor is the one place for rules, so an invalid instance cannot exist.
- **Shared kernel, but small:** only types that several modules need and that have no business meaning of their own. Course price tiers stay in `authoring` (`PriceTier`); only the generic `Money` is shared.
- **Minor units as `long`:** exact integer arithmetic. 1999 = 19.99 USD. Stripe also uses minor units, so no conversion at the payment adapter.
- **`Math.addExact`:** an overflow throws `ArithmeticException` instead of silently wrapping to a negative amount.
- **ISO 4217 check with `java.util.Currency`:** the JDK knows all valid codes; no hand-written list.
- **Defensive copy in `PageResult`:** `List.copyOf` makes the items immutable, so a service cannot change a page after a repository returned it.
- **`MAX_SIZE = 100`:** an upper bound protects the database and the API from very large pages (SPEC-catalog limits search to 50, which is inside this bound).

# Production shape

- Adapters convert at the boundary: the JPA adapter turns `PageQuery` into `PageRequest` and `Page<Entity>` into `PageResult<Course>`; the REST adapter turns query parameters into `PageQuery` (invalid values → 400 through the T03 handler, after the module maps `IllegalArgumentException`).
- Money is stored in PostgreSQL as `BIGINT` + `CHAR(3)`, never as `FLOAT`; JSON sends `amountMinor` as a number and the frontend formats it with `Intl.NumberFormat`.
- When local currencies arrive (US-4.4), conversion lives in a separate service with a dated exchange rate; `Money.plus` keeps refusing mixed currencies.

# Other possible approaches

1. Use Spring Data `Pageable` / `Page` in ports.
2. Use `BigDecimal` for money.
3. Use the JavaMoney library (JSR 354, Moneta).
4. Each module defines its own page and money types.

# Why I did not choose those alternatives

1. It makes the application layer depend on Spring Data; ArchUnit (T05) would flag a framework dependency in pure code, and fakes in tests become harder.
2. `BigDecimal` is exact but has scale traps (`2.0` is not `equals` to `2.00`) and does not match Stripe's integer API. Prices here are always whole cents.
3. A full money library is much more than we need (formatting, conversion providers) and adds a dependency to the domain.
4. Duplication: three copies of the same rules drift apart.

# Key concepts to learn

- **Shared kernel (DDD):** a small model that several bounded contexts agree to share; every change needs agreement, so keep it tiny.
- **Value object:** defined only by its values, immutable, validated on creation (`Money`, `PageQuery`).
- **Compact constructor:** the record constructor without parameter list; it validates (and may normalize) fields before assignment.
- **Minor units:** the smallest unit of a currency (cents). Note: some currencies have 0 (JPY) or 3 (KWD) decimal places; `Currency.getDefaultFractionDigits()` gives the number for display.
- **Defensive copy:** copy mutable input so outside code cannot change your object's state later.

# Common mistakes

- `double` or `float` for money.
- Adding amounts in different currencies, or assuming every currency has 2 decimals when formatting.
- `List` fields in records without a copy: the record looks immutable but is not.
- Validation messages like "invalid input" without the value and the expected shape; the next developer has to debug blind.
- Growing the shared kernel with business concepts ("Course", "Price tier"): then every module depends on every change.

# Small example

```java
PageResult<Course> page = courseRepository.findByInstructor(instructorId, new PageQuery(0, 20));
PageResult<CourseView> view = page.map(CourseView::from);   // totalItems unchanged

Money total = new Money(1999, "USD").plus(new Money(2999, "USD"));  // 4998 USD minor units
new Money(1999, "USD").plus(new Money(1999, "EUR"));
// IllegalArgumentException: Cannot add EUR to USD; expected the same currency
```

# How to think about this next time

- Ask: "Is this type generic and needed by two or more modules?" Only then put it in `shared`. Otherwise keep it in the module.
- Ask: "Can an invalid instance exist?" If yes, move the rule into the constructor.
- For money, always ask three questions: which unit (minor units), which currency, and what happens on overflow or a mixed currency.
